package services

import java.security.PublicKey
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import akka.pattern._
import akka.util.Timeout
import cryptographer.RSA
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object ATMKeeperService {

  private case class Command(cmd: String)
  private case class InitSecure(remote: ActorRef, publicKey: PublicKey)

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

    var remoteActorBBB: ActorRef = _
    var remoteBBBPublicKey: PublicKey = _

    override def receive: Receive = {

      case InitSecure(remote, publicKey) =>
        remoteActorBBB = remote
        remoteBBBPublicKey = publicKey
        println(remoteActorBBB)

      case msg: String => println(msg)

      case command: Command =>
        val realSender: ActorRef = sender
        implicit val timeout: Timeout = Timeout(10.seconds)
        val response: Future[Either[String, (String, String)]] =
          (remoteActorBBB ? RSA.encrypt(command.toString, remoteBBBPublicKey)).mapTo[Either[String, (String, String)]]
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
