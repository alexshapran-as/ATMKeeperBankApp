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
        implicit val timeout: Timeout = Timeout(10.seconds)
        val response: Future[Either[String, (String, String)]] =
          if (UsbUtils().validateBBBUsbConnection()) {
            if (command.cmd == "Загрузить ключи") {
              (remoteActorBBB ? command.toString.getBytes).mapTo[Either[String, (String, String)]]
            } else if (remoteBBBPublicKey != null) {
              (remoteActorBBB ? RSA.encrypt(command.toString, remoteBBBPublicKey)).mapTo[Either[String, (String, String)]]
            } else {
              Future { Left("BANK - BEAGLE BONE: Ключи шифрования не переданы! Передайте ключи для защищенного общения") }
            }
          } else {
            Future { Left("BEAGLE BONE: beagle bone не подключена к системе") }
          }
        response map {
          case Right(msgs) =>
            realSender ! Right(msgs)
          case Left(errMsg) =>
            realSender ! Left(errMsg)
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
