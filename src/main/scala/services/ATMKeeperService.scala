package services

import java.security.PublicKey

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import akka.pattern._
import akka.util.Timeout
import cryptographer.RSA
import usb.utils.UsbUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object ATMKeeperService {

  private case class Command(cmd: String)
  private case class InitBBBForBank(remote: ActorRef)
  private case class InitBBBPubKey(realSender: ActorRef, publicKey: PublicKey)
  private case class InitBankPubKey(realSender: ActorRef, publicKey: PublicKey)

  def remotingConfig(port: Int): Config = ConfigFactory.parseString(
    s"""
        akka {
          actor.warn-about-java-serializer-usage = off
          actor.provider = "akka.remote.RemoteActorRefProvider"
          remote {
            enabled-transports = ["akka.remote.netty.tcp"]
            netty.tcp {
              hostname = "localhost"
              port = $port
            }
          }
        }
    """) // "10.50.1.61" BMSTU

  def remotingSystem(name: String, port: Int): ActorSystem = ActorSystem(name, remotingConfig(port))

  class BankService extends Actor {

    var remoteActorBBB: ActorRef = null
    var remoteBBBPublicKey: PublicKey = null
    var bankStop = false
    var bankIsInService = false

    override def receive: Receive = {

      case InitBBBForBank(bbbRemote: ActorRef) =>
        remoteActorBBB = bbbRemote
        remoteActorBBB ! s"Connection established with ${self.path}"
        println(remoteActorBBB)

      case InitBBBPubKey(realSender: ActorRef, remotePublicKey: PublicKey) =>
        remoteBBBPublicKey = remotePublicKey
        remoteActorBBB ! InitBankPubKey(realSender, RSA.getSelfPublicKey)

      case msg: String =>
        println(msg)

      case command: Command =>
        val realSender: ActorRef = sender
        implicit val timeout: Timeout = Timeout(1000.seconds)
        val response: Future[Either[Array[Byte], (Array[Byte], Option[Array[Byte]])]] =
           if (UsbUtils().validateBBBUsbConnection()) {
             if (command.cmd == "Выключить банкомат") {
                bankStop = true
                Future { Right("БАНКОМАТ: выключен".getBytes(), None) }
              } else if (command.cmd == "Перезагрузить банкомат") {
                bankIsInService = false
                bankStop = false
                Future { Right("БАНКОМАТ: перезагружен".getBytes(), None) }
              } else if (command.cmd == "Включить банкомат") {
                bankIsInService = false
                bankStop = false
                Future { Right("БАНКОМАТ: включен".getBytes(), None) }
              } else if (bankStop) {
                Future { Right("БАНКОМАТ: выключен".getBytes(), None) }
             } else if (bankIsInService) {
                Future { Right("БАНКОМАТ: сервисное обслуживание (перезагрузите банкомат по окончанию обслуживания)".getBytes(), None) }
             } else if (command.cmd == "Загрузить ключи" || command.cmd == "Обновить ключи") {
                (remoteActorBBB ? command.toString.getBytes).mapTo[Either[Array[Byte], (Array[Byte], Option[Array[Byte]])]]
              } else if (command.cmd == "Сервисное обслуживание банкомата") {
                bankIsInService = true
                Future { Right("БАНКОМАТ: сервисное обслуживание".getBytes(), None) }
              } else if (remoteBBBPublicKey != null) {
                (remoteActorBBB ? RSA.encrypt(command.toString, remoteBBBPublicKey)).mapTo[Either[Array[Byte], (Array[Byte], Option[Array[Byte]])]]
              } else {
                Future { Left("БАНКОМАТ - BEAGLE BONE: Ключи шифрования не переданы! Передайте ключи для защищенной передачи команд".getBytes()) }
              }
          } else {
            Future { Left("BEAGLE BONE: beagle bone не подключена к системе".getBytes()) }
          }
        response map {
          case Right(msgs) if new String(msgs._1) == "БАНКОМАТ - BEAGLE BONE: Ключи сброшены" =>
            remoteBBBPublicKey = null
            RSA.resetKeys
            realSender ! Right((new String(msgs._1), ""))
          case Right(msgs) if new String(msgs._1) == "БАНКОМАТ: сервисное обслуживание (перезагрузите банкомат по окончанию обслуживания)" ||
                              new String(msgs._1) == "БАНКОМАТ: выключен" ||
                              new String(msgs._1) == "БАНКОМАТ: перезагружен" ||
                              new String(msgs._1) == "БАНКОМАТ: включен" ||
                              new String(msgs._1) == "БАНКОМАТ: сервисное обслуживание" =>
            realSender ! Right((new String(msgs._1), ""))
          case Right(msgs) =>
            realSender ! Right((RSA.decrypt(msgs._1), if (msgs._2.isDefined) RSA.decrypt(msgs._2.get) else ""))
          case Left(errMsg) =>
            realSender ! Left(new String(errMsg))
        }
    }

  }

  case class Bank() {
    val system: ActorSystem = remotingSystem("BankSystem", 24321)
    val localActorBank: ActorRef = system.actorOf(Props[BankService], "bank")
    def sendCommandToBBB(cmd: String):  Future[Either[String, (String, String)]] = {
      implicit val timeout: Timeout = Timeout(10.seconds)
        (localActorBank ? Command(cmd)).mapTo[Either[String, (String, String)]]
    }
  }

}
