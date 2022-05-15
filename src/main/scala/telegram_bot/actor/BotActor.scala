package telegram_bot.actor

import actor.BayesActor
import akka.actor.{ActorRef, ActorSystem, FSM, Props}
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.Uri.Query
import akka.pattern.ask
import akka.util
import akka.util.Timeout
import classifier.utils.{ClassTypes, Utils}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}
import telegram_bot.actor.BotActor.Data.BufferedUpdates

import scala.concurrent.{Await, Future}
import telegram_bot.models.{Chat, Message, TelegramResponse, Text, Update, User}

import scala.concurrent.duration.DurationInt
import scala.util.Success

class BotActor(token: String, http: HttpExt, bayesActor: ActorRef) extends FSM[BotActor.State, BotActor.Data] {
  private val defaultUrl: String = "https://api.telegram.org/bot"
  private val getUpdatesTimeout: Int = 120

  import BayesActor._
  import BotActor._
  import Data._
  import State._
  import context.dispatcher
  import akka.pattern.pipe

  private implicit val system: ActorSystem = context.system
  private implicit val timeout: util.Timeout = Timeout(10.seconds)

  private implicit val chatFormat = jsonFormat1(Chat)
  private implicit val userFormat = jsonFormat4(User)
  private implicit val messageFormat = jsonFormat5(Message)
  private implicit val updateFormat = jsonFormat2(Update)
  private implicit val responseFormat = jsonFormat2(TelegramResponse)

  log.info("Bot actor is ready")

  def getUpdates(lastUpdateId: Long): Future[HttpResponse] = {
    log.info(s"Getting updates from offset: $lastUpdateId")
    val query = Query("timeout" -> getUpdatesTimeout.toString, "offset" -> lastUpdateId.toString)
    val url = Uri(s"$defaultUrl$token/getUpdates").withQuery(query)

    http.singleRequest(HttpRequest(uri = url))
  }

  def sendMessage(chatId: Long, text: String, parseMode: String, replyToMessageId: Option[Long]): Future[HttpResponse] = {
    log.info(s"Sending message: $text to chat $chatId")
    val query = Query("chat_id" -> chatId.toString,
      "text" -> text,
      "parse_mode" -> parseMode,
      "reply_to_message_id" -> replyToMessageId.getOrElse("").toString)
    val url = Uri(s"$defaultUrl$token/sendMessage").withQuery(query)

    http.singleRequest(HttpRequest(uri = url))
  }

  def updateData(buffer: Array[Update], lastUpdateId: Long): BufferedUpdates = {
    BufferedUpdates(buffer, lastUpdateId)
  }

  startWith(Idle, BufferedUpdates(Array.empty, 0))

  when(Idle) {
    case Event(LoadUpdates, _) => {
      log.info("loading updates")
      goto(MakingRequest)
    }

    case Event(SendMessage(bufferedUpdates), _) => {
      val results = bufferedUpdates.buffer.map(update => update.message.text match {
        case Some(messageText) =>
          val classifyResult = (bayesActor ? GetTextClassWithHighlights(messageText)).mapTo[(String, String)]
          classifyResult.onComplete {
            case Success((classType, highlightedText)) =>
              if (classType == ClassTypes.readableNeutral) {
                log.info(s"встречен нейтральный текст: ${update.message.text.getOrElse("")}")
              } else sendMessage(update.message.chat.id, s"[${classType.toUpperCase}]: $highlightedText", "HTML", Some(update.message.message_id))
            case _ =>
              log.warning("ошибка со стороны BayesActor")
          }
        case None =>
          log.info("встречено нетекстовое сообщение")
      })

      goto(MakingRequest) using BufferedUpdates(Array.empty[Update], bufferedUpdates.lastUpdateId)
    }

    case Event(_, _) => {
      stay()
    }
  }

  when(MakingRequest) {
    case Event(HttpResponse(StatusCodes.OK, _, entity, _), BufferedUpdates(buffer, lastUpdateId))
      if entity != HttpEntity.Empty => {

      val resp: Future[TelegramResponse] = Unmarshal(entity).to[TelegramResponse]

      resp.map(r => if (r.ok) {
        val updates = r.result
        val newLastUpdateId = updates.last.update_id
        val newData = updateData(updates, newLastUpdateId + 1)
        self ! SendMessage(newData)
      } else {
        log.error("bad unmarhsalling")
        stay()
      })

      goto(Idle)
    }
    case Event(HttpResponse(statusCode, _, _, _), BufferedUpdates(buffer, lastUpdateId)) => {
      log.error(s"Плохой ответ от telegram api: $statusCode; lastUpdateId: $lastUpdateId ")
      stay()
    }
    case Event(LoadUpdates, _) =>
      stay()
  }

  whenUnhandled {
    case Event(value, stateData) => {
      log.warning(s"Unhandled event $value")
      stay()
    }
  }

  onTransition {
    case Idle -> MakingRequest =>
      nextStateData match {
        case BufferedUpdates(buffer, lastUpdateId) =>
          getUpdates(lastUpdateId).pipeTo(self)
      }
    case MakingRequest -> Idle =>
      nextStateData match {
        case BufferedUpdates(buffer, lastUpdateId) =>
          self ! LoadUpdates
      }
  }

}

object BotActor {
  def props(token: String, http: HttpExt, bayesActor: ActorRef): Props = Props(new BotActor(token, http, bayesActor))

  trait State

  object State {
    case object Idle extends State

    case object MakingRequest extends State
  }

  trait Data

  object Data {
    case class BufferedUpdates(buffer: Array[Update], lastUpdateId: Long) extends Data
  }

  case object LoadUpdates

  case class SendMessage(bufferedUpdates: BufferedUpdates)
}