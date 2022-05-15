package server

import actor.BayesActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import logger.ServerLogger
import rest.RestAPI
import service.NaiveBayesService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn
import scala.util.{Failure, Success}

class AkkaServer(bayesActor: ActorRef)(implicit val system: ActorSystem) {
  val bayesService = new NaiveBayesService(bayesActor)
  val restApi = new RestAPI(bayesService)
  val localhost: String = Config.address
  val port: Int = Config.port
  val route: Route = restApi.routes

  Http().newServerAt(localhost, port).bind(route)
    .map(_ => ServerLogger.serverLogger.info(s"Server is bounded to $localhost:$port"))
    .onComplete {
      case Failure(exception) =>
        ServerLogger.serverLogger.error(s"Unexpected error while binding server: ${exception.getMessage}")
        system.terminate()
      case Success(_) => ()
    }

  StdIn.readLine("Press ENTER to stop\n")
  system.terminate()
  ServerLogger.serverLogger.info("Server is shut down")
}

object AkkaServer {
  def apply(bayesActor: ActorRef)(implicit actorSystem: ActorSystem) = new AkkaServer(bayesActor)
}