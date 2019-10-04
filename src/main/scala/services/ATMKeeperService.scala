package services

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import authenticator.Authenticator._
import akka.pattern._
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object ATMKeeperService {

  private case class Command(cmd: String, signature: String)
  private case class Init(remote: ActorRef)

  def remotingConfig(port: Int): Config = ConfigFactory.parseString(
    s"""
        akka {
          actor.warn-about-java-serializer-usage = off
          actor.provider = "akka.remote.RemoteActorRefProvider"
          remote {
            enabled-transports = ["akka.remote.netty.tcp"]
            netty.tcp {
              hostname = "192.168.0.161"
              port = $port
            }
          }
        }
    """)

  def remotingSystem(name: String, port: Int): ActorSystem = ActorSystem(name, remotingConfig(port))

  class BankService extends Actor {

    var remoteActorBBB: ActorRef = _

    override def receive: Receive = {

      case Init(remote) =>
        remoteActorBBB = remote
        println(remoteActorBBB)

      case msg: String => println(msg)

      case command: Command =>
        val realSender: ActorRef = sender
        implicit val timeout: Timeout = Timeout(10.seconds)
        val response: Future[Either[String, String]] = (remoteActorBBB ? command).mapTo[Either[String, String]]
        response map {
          case Right(msg) =>
            realSender ! Right(msg)
          case Left(errMsg) =>
            realSender ! Left(errMsg)
        }
    }

  }

  case class Bank() {

    val system: ActorSystem = remotingSystem("BankSystem", 24321)
    val localActorBank: ActorRef = system.actorOf(Props[BankService], "bank")
    def sendCommandToBBB(cmd: String):  Future[Either[String, String]] = {
      implicit val timeout: Timeout = Timeout(10.seconds)
      (localActorBank ? Command(cmd, generateHMAC(cmd))).mapTo[Either[String, String]]
    }


  }

}
