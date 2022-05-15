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
  //  implicit object MessageFormat extends RootJsonFormat[TextMessage] {
  //    override def read(json: JsValue): TextMessage = json match {
  //      case JsObject(fields) =>
  //        try {
  //          TextMessage(fields("message_id").convertTo[Long],
  //            fields("from").convertTo[User],
  //            fields("data").convertTo[Long],
  //            fields("chat").convertTo[Chat],
  //            fields("text").convertTo[String])
  //        } catch {
  //          case _ => deserializationError("cannot deserialize to TextMessage object")
  //        }
  //      case _ => deserializationError("not a TextMessage object")
  //    }
  //
  //    override def write(obj: TextMessage): JsValue =
  //  }
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
    case Event(LoadUpdates, _) => println("ENTERING LOAD UPDATES"); goto(MakingRequest)
    case Event(SendMessage(bufferedUpdates), _) => {
      println(s"DATA TO SEND: ${bufferedUpdates.lastUpdateId} ${bufferedUpdates.buffer.mkString("Array(", ", ", ")")}")

      val results = bufferedUpdates.buffer.map(update => update.message.text match {
        case Some(messageText) =>
          val classifyResult = (bayesActor ? GetTextClassWithHighlights(messageText)).mapTo[(String, String)]
          classifyResult.onComplete {
            case Success((classType, highlightedText)) =>
              if (classType == ClassTypes.readableNeutral) {
                log.info(s"встречен нейтральный текст: ${update.message.text}")
                HttpResponse(status = StatusCodes.OK)
              } else sendMessage(update.message.chat.id, s"$classType: $highlightedText", "HTML", Some(update.message.message_id))
            case _ => HttpResponse(status = StatusCodes.InternalServerError)
          }
        case None =>
          log.info("встречено нетекстовое сообщение")
      })

      goto(MakingRequest) using BufferedUpdates(Array.empty[Update], bufferedUpdates.lastUpdateId)
    }


  case Event(_, BufferedUpdates(buffer, lastUpdateId)) => {
    val incomeData = BufferedUpdates(buffer, lastUpdateId)
    println(s"DATA IN IDLE: $incomeData $lastUpdateId")

    stay()
  }
}

when (MakingRequest) {
case Event (HttpResponse (StatusCodes.OK, _, entity, _), BufferedUpdates (buffer, lastUpdateId) )
if entity != HttpEntity.Empty => {

val resp: Future[TelegramResponse] = Unmarshal (entity).to[TelegramResponse]
println ("ENTERED MAKING REQUEST")

resp.map (r => if (r.ok) {
val updates = r.result
val newLastUpdateId = updates.last.update_id
println (s"NEW LAST UPDATE ID: $newLastUpdateId")
val newData = updateData (updates, newLastUpdateId + 1)
println (newData.buffer.mkString ("Array(", ", ", ")") )
println (newData.lastUpdateId)
println ("GOING TO IDLE")
self ! SendMessage (newData)
} else {
println ("error\n");
stay ()
})

goto (Idle)
}
case Event (HttpResponse (StatusCodes.BadRequest, _, _, _), BufferedUpdates (buffer, lastUpdateId) ) => {
log.error (s"Bot made a bad request. Last update id: $lastUpdateId")
stay ()
}
case Event (LoadUpdates, _) =>
stay () // just ignore
}

whenUnhandled {
case Event (value, stateData) => {
log.warning (s"Unhandled event $value")
stay ()
}
}

onTransition {
case Idle -> MakingRequest =>
nextStateData match {
case BufferedUpdates (buffer, lastUpdateId) =>
println ("TRANSITION IDLE -> MAKINGREQUEST")
getUpdates (lastUpdateId).pipeTo (self)
}
case MakingRequest -> Idle =>
nextStateData match {
case BufferedUpdates (buffer, lastUpdateId) =>
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