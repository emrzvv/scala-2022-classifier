package telegram_bot

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.util.ByteString
import service.NaiveBayesService
import telegram_bot.actor.BotActor
import telegram_bot.actor.BotActor.LoadUpdates

class Bot(token: String, bayesActor: ActorRef)(implicit val system: ActorSystem) {
  val http = Http()
  val botActor: ActorRef = system.actorOf(BotActor.props(token, http, bayesActor), "bot")
  botActor ! LoadUpdates
}

object Bot {
  def apply(token: String, bayesActor: ActorRef)(implicit system: ActorSystem): Bot = new Bot(token, bayesActor)
}