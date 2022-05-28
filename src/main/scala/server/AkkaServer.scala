package server

import actor.BayesActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import logger.ServerLogger
import rest.RestAPI
import service.NaiveBayesService
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn
import scala.util.{Failure, Success}

class AkkaServer {
  implicit val system: ActorSystem = ActorSystem("my-system")

  val bayesActor: ActorRef = system.actorOf(Props[BayesActor]())
  val bayesService = new NaiveBayesService(bayesActor)
  val restApi = new RestAPI(bayesService)
  val route: Route = restApi.routes

  val config: Config = ConfigFactory.load()
  val localhost: String = config.getString("serverHost")
  val port: Int = config.getInt("serverPort")

  Http().newServerAt(localhost, port).bind(route)
    .map(_ => ServerLogger.logger.info(s"Server is bounded to $localhost:$port"))
    .onComplete {
      case Failure(exception) =>
        ServerLogger.logger.error(s"Unexpected error while binding server: ${exception.getMessage}")
        system.terminate()
      case Success(_) => ()
    }

  StdIn.readLine("Press ENTER to stop\n")
  system.terminate()
  ServerLogger.logger.info("Server is shut down")
}

object AkkaServer {
  def apply() = new AkkaServer
}