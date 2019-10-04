package services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import configurations.Conf.{confApiServiceInterface, confApiServicePort}
import services.ATMKeeperService.Bank

import scala.concurrent.{ExecutionContextExecutor, Future}

object ApiService {

  implicit val system: ActorSystem = ActorSystem("routing-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  case class Routing() extends Directives {

    def routingStart(): Unit = {

      val bank: Bank = Bank()

      val route = post {
        path("send-command") {
          parameter("command") { cmd =>
            complete {
              val response: Future[Either[String, String]] = bank.sendCommandToBBB(cmd)
              response map {
                case Right(msg) =>
                  println(msg)
                  msg
                case Left(errMsg) =>
                  println(errMsg)
                  errMsg
              }
            }
          }
        }
      }

      Http().bindAndHandle(route, confApiServiceInterface, confApiServicePort)
      println(s"Bank Server online at http://$confApiServiceInterface:$confApiServicePort/")

    }

  }

}
