package server

import actor.BayesActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import rest.RestAPI
import service.NaiveBayesService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn
import scala.util.{Failure, Success}

class AkkaServer {
  implicit val system: ActorSystem = ActorSystem("my-system")

  val bayesActor: ActorRef = system.actorOf(Props[BayesActor])
  val bayesService = new NaiveBayesService(bayesActor)
  val restApi = new RestAPI(bayesService)
  val localhost: String = Config.address
  val port: Int = Config.port
  val route: Route = restApi.routes

  Http().newServerAt(localhost, port).bind(route)
    .map(_ => println(s"Server is bounded to $localhost"))
    .onComplete {
      case Failure(exception) =>
        println(s"Unexpected error while binding server: ${exception.getMessage}")
        system.terminate()
      case Success(_) => ()
    }

  StdIn.readLine("Press ENTER to stop\n")
  system.terminate()
}

object AkkaServer {
  def apply() = new AkkaServer
}