package telegram_bot.actor

import actor.BayesActor
import akka.actor.{ActorRef, ActorSystem, FSM, Props}
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.Uri.Query
import akka.pattern.ask
import akka.util
import akka.util.Timeout
import classifier.utils.ClassTypes
import spray.json.DefaultJsonProtocol._
import telegram_bot.actor.BotActor.Data.BufferedUpdates

import scala.concurrent.Future
import telegram_bot.models.{Chat, Message, TelegramResponse, Update, User}

import scala.concurrent.duration.DurationInt
import scala.util.Success

/**
 * актор, отвечающий за получение апдейтов от telegram api и реакцию на эти апдейты.
 * для реализации используется интерфейс final state machine:
 * https://doc.akka.io/docs/akka/current/fsm.html
 *
 * @param token      токен бота
 * @param http       объект Http, необходимый для отправки http-запросов
 * @param bayesActor актор классификатора, которому отправляются запросы на классификацию текстов
 */
class BotActor(token: String, http: HttpExt, bayesActor: ActorRef) extends FSM[BotActor.State, BotActor.Data] {
  private val defaultUrl: String = "https://api.telegram.org/bot"
  private val getUpdatesTimeout: Int = 120 // сколько можем ждать ответа от telegram api на запрос getUpdates

  import BayesActor._
  import BotActor._
  import Data._
  import State._
  import context.dispatcher
  import akka.pattern.pipe

  private implicit val system: ActorSystem = context.system
  private implicit val timeout: util.Timeout = Timeout(10.seconds)

  /**
   * объекты JsonFormat, необходимые для разбора поступающего json
   */
  private implicit val chatFormat = jsonFormat1(Chat)
  private implicit val userFormat = jsonFormat4(User)
  private implicit val messageFormat = jsonFormat5(Message)
  private implicit val updateFormat = jsonFormat2(Update)
  private implicit val responseFormat = jsonFormat2(TelegramResponse)

  log.info("Bot actor is ready")

  def makeRequest(methodName: String, query: Option[Query], logMessage: Option[String]): Future[HttpResponse] = {
    log.info(logMessage.getOrElse(s"Making request $methodName with params ${query.toString}"))
    val url = Uri(s"$defaultUrl$token/$methodName").withQuery(query.getOrElse(Query.Empty))

    http.singleRequest(HttpRequest(uri = url))
  }

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
      val results = bufferedUpdates.buffer.map(update => update.message match {
        case Some(message) => message.text match {
          case Some(messageText) =>
            val classifyResult = (bayesActor ? GetTextClassWithHighlights(messageText)).mapTo[(String, String)]

            classifyResult.onComplete {
              case Success((classType, highlightedText)) =>
                if (classType == ClassTypes.readableNeutral) {
                  log.info(s"Встречен нейтральный текст: $messageText")
                } else {
                  val query = Query("chat_id" -> message.chat.get.id.toString,
                    "text" -> s"[${classType.toUpperCase}]: $highlightedText",
                    "parse_mode" -> "HTML",
                    "reply_to_message_id" -> Some(message.message_id).getOrElse("").toString)

                  makeRequest("sendMessage", Some(query), Some(s"Отправлено сообщение в чат ${message.chat.get.id}"))
                }
              case _ =>
                log.warning("ошибка со стороны BayesActor")
            }
          case None =>
            log.info("Встречено нетекстовое сообщение")
        }
        case None => log.info("В полученном апдейте нет сообщения")
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

      val telegramResponse: Future[TelegramResponse] = Unmarshal(entity).to[TelegramResponse]

      println(s"GOT TELEGRAM RESP: $telegramResponse")

      telegramResponse.map(response => if (response.ok) {
        val updates = response.result
        val newLastUpdateId = updates.last.update_id
        val newData = updateData(updates, newLastUpdateId + 1)
        self ! SendMessage(newData)
      } else {
        log.error("bad unmarshalling")
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
          val query = Query("timeout" -> getUpdatesTimeout.toString, "offset" -> lastUpdateId.toString)
          makeRequest("getUpdates", Some(query), Some(s"Запрашиваем апдейты. Последний апдейт: $lastUpdateId"))
            .pipeTo(self)
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