package telegram_bot

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.util.ByteString
import service.NaiveBayesService
import telegram_bot.actor.BotActor
import telegram_bot.actor.BotActor.LoadUpdates

class Bot(token: String) {
  implicit val system: ActorSystem = ActorSystem("bot-system")
  val http = Http(system)
  val botActor: ActorRef = system.actorOf(BotActor.props(token, http), "bot")
  botActor ! LoadUpdates
}

object Bot {
  def apply(token: String): Bot = new Bot(token)
}