package telegram_bot.actor

import akka.actor.{FSM, Props}
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.Uri.Query
import spray.json.DefaultJsonProtocol._
import telegram_bot.actor.BotActor.Data.BufferedUpdates

import scala.concurrent.{Await, Future}
import telegram_bot.models.{Chat, TelegramResponse, TextMessage, Update, User}

class BotActor(token: String, http: HttpExt) extends FSM[BotActor.State, BotActor.Data] {
  private val defaultUrl: String = "https://api.telegram.org/bot"
  private val getUpdatesTimeout: Int = 120

  import BotActor._
  import Data._
  import State._
  import context.dispatcher
  import akka.pattern.pipe

  private implicit val system = context.system

  private implicit val chatFormat = jsonFormat1(Chat)
  private implicit val userFormat = jsonFormat4(User)
  private implicit val messageFormat = jsonFormat5(TextMessage)
  private implicit val updateFormat = jsonFormat2(Update)
  private implicit val responseFormat = jsonFormat2(TelegramResponse)

  log.info("Bot actor is ready")

  def go(lastUpdateId: Long): Future[HttpResponse] = {
    log.info(s"Getting updates from offset: $lastUpdateId")
    val query = Query("timeout" -> getUpdatesTimeout.toString, "offset" -> lastUpdateId.toString)
    val url = Uri(s"$defaultUrl$token/getUpdates").withQuery(query)

    http.singleRequest(HttpRequest(uri = url))
  }

  def sendMessage(chatId: Long, text: String, parseMode: String, replyToMessageId: Option[Long]): Future[HttpResponse] = {
    log.info(s"Sending message: $text to chat $chatId")
    val query = Query("chat_id" -> chatId.toString, "text" -> text, "parse_mode" -> parseMode, "reply_to_message_id" -> replyToMessageId.getOrElse("").toString)
    val url = Uri(s"$defaultUrl$token/sendMessage").withQuery(query)

    http.singleRequest(HttpRequest(uri = url))
  }

  def updateData(buffer: Array[Update], lastUpdateId: Long): BufferedUpdates = {
    BufferedUpdates(buffer, lastUpdateId)
  }

  startWith(Idle, BufferedUpdates(Array.empty, 0)) // change last processed update id

  when(Idle) {
    //    case Event(HttpRequest(_), BufferedUpdates(buffer, lastUpdateId)) => {
    //      val incomeData = BufferedUpdates(buffer, lastUpdateId)
    //      stay using incomeData
    //    }
    case Event(LoadUpdates, _) => println("ENTERING LOAD UPDATES"); goto(MakingRequest)
    case Event(SendingMessage(bufferedUpdates), _) => {
      println(s"DATA TO SEND: ${bufferedUpdates.lastUpdateId} ${bufferedUpdates.buffer.mkString("Array(", ", ", ")")}")

      val results = bufferedUpdates.buffer.map(update =>
          sendMessage(update.message.chat.id, "finally", "HTML", Some(update.message.message_id)))

      results.foreach(futureResponse => futureResponse.map(response => if (response.status != StatusCodes.OK) log.warning(s"message has not been sent: ${response.entity.toString}")))

      goto(MakingRequest) using BufferedUpdates(Array.empty[Update], bufferedUpdates.lastUpdateId)
    }
    case Event(_, BufferedUpdates(buffer, lastUpdateId)) => {
      val incomeData = BufferedUpdates(buffer, lastUpdateId)
      println(s"DATA IN IDLE: $incomeData $lastUpdateId")

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
        println(s"NEW LAST UPDATE ID: $newLastUpdateId")
        val newData = updateData(updates, newLastUpdateId + 1)
        println(newData.buffer.mkString("Array(", ", ", ")"))
        println(newData.lastUpdateId)
        println("GOING TO IDLE")
        self ! SendingMessage(newData)
      } else {
        println("error\n");
        stay()
      })

      goto(Idle)
    }
    case Event(HttpResponse(StatusCodes.BadRequest, _, _, _), BufferedUpdates(buffer, lastUpdateId)) => {
      log.error(s"Bot made a bad request. Last update id: $lastUpdateId")
      stay()
    }
    case Event(LoadUpdates, _) =>
      stay() // just ignore
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
        case BufferedUpdates(buffer, lastUpdateId) => println(s"OFFSET REQUEST ID: $lastUpdateId"); go(lastUpdateId).pipeTo(self)
      }
    case MakingRequest -> Idle =>
      nextStateData match {
        case BufferedUpdates(buffer, lastUpdateId) => {
          println(s"IN TRANSITION: $lastUpdateId --- ${buffer.mkString("Array(", ", ", ")")}")
          self ! LoadUpdates
        }
      }
  }

}

object BotActor {
  def props(token: String, http: HttpExt): Props = Props(new BotActor(token, http))

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

  case class SendingMessage(bufferedUpdates: BufferedUpdates)
}