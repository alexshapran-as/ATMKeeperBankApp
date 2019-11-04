package services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.{Directive0, Directives}
import akka.stream.ActorMaterializer
import configurations.Conf.{confApiServiceInterface, confApiServicePort}
import services.ATMKeeperService.Bank
import scala.concurrent.{ExecutionContextExecutor, Future}

object ApiService {

  implicit val system: ActorSystem = ActorSystem("routing-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  case class Routing() extends Directives {

    def respondWithJsonContentType: Directive0 =
      mapResponse(response => response.mapEntity(entity => entity.withContentType(ContentTypes.`application/json`)))

    def routingStart(): Unit = {
      val bank: Bank = Bank()

      val route = respondWithJsonContentType {
        get {
          path("send-command") {
            parameter("command") { cmd =>
              println(cmd)
              complete {
                val response: Future[Either[String, (String, String)]] = bank.sendCommandToBBB(cmd)
                response map {
                  case Right(msgs) =>
                    println(s"${msgs._1}\n${msgs._2}")
                    s"""{
                       |  "result": "success",
                       |  "state": "$msgs"
                       |}""".stripMargin
                  case Left(errMsg) =>
                    s"""{
                       |  "result": "false",
                       |  "state": "$errMsg"
                       |}""".stripMargin
                }
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
