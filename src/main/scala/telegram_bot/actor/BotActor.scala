package telegram_bot.actor

import akka.actor.{FSM, Props}
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

import io.circe.syntax._

class BotActor(token: String, http: HttpExt) extends FSM[BotActor.State, BotActor.Data] {
  private val defaultUrl: String = "https://api.telegram.org/bot"
  private val getUpdatesTimeout: Int = 120

  import BotActor._
  import Data._
  import State._
  import context.dispatcher
  import akka.pattern.pipe

  private implicit val system = context.system

  def go(lastUpdateId: Long): Future[HttpResponse] = {
    val params: String = s"timeout=$getUpdatesTimeout&offset=$lastUpdateId"
    val url = s"$defaultUrl$token/getUpdates?"
    http.singleRequest(HttpRequest(uri = url))
  }

  def updateData(buffer: ArrayBuffer[String], lastUpdateId: Long): BufferedUpdates = {
    BufferedUpdates(buffer, lastUpdateId)
  }

  startWith(Idle, BufferedUpdates(ArrayBuffer.empty, 0)) // change last processed update id

  when(Idle) {
    case Event(HttpRequest(_), BufferedUpdates(buffer, lastUpdateId)) => {
      val incomeData = BufferedUpdates(buffer, lastUpdateId)
      stay using incomeData
    }
    case Event(LoadUpdates, _) => goto(MakingRequest)
  }

  when(MakingRequest) {
    case Event(HttpRequest(_), BufferedUpdates(buffer, lastUpdateId)) => {
      val incomeData = BufferedUpdates(buffer, lastUpdateId)
      stay using incomeData
    }
    case Event(HttpResponse(StatusCodes.OK, _, entity, _), BufferedUpdates(buffer, lastUpdateId))
      if entity != HttpEntity.Empty => {
      // прикрутить materializer в go и вытащить последний update id
      val newLastUpdateId = 0
      val incomeMessages = ???
      val newData = BufferedUpdates(buffer.addAll(incomeMessages), newLastUpdateId)

      goto(Idle) using newData
    }
    case Event(HttpResponse(StatusCodes.BadRequest, _, _, _), BufferedUpdates(buffer, lastUpdateId)) => {
      log.error(s"we made a bad request. last update id: $lastUpdateId")
      stay()
    }
    case Event(LoadUpdates, _) =>
      stay() // just ignore
  }

  whenUnhandled {
    case Event(value, stateData) => {
      log.warning(s"unhandled event $value")
      stay()
    }
  }

  onTransition {
    case Idle -> MakingRequest =>
      nextStateData match {
        case BufferedUpdates(buffer, lastUpdateId) => go(lastUpdateId).pipeTo(self)
      }
    case MakingRequest -> Idle =>
      nextStateData match {
        case BufferedUpdates(buffer, lastUpdateId) => self ! LoadUpdates
      }
  }

//  private def sendRequest(methodName: String, params: String): Unit = {
//    // implicit val timeout: Timeout = Timeout(120.seconds)
//    val url = s"$defaultUrl$token/$methodName?$params"
//    http.singleRequest(HttpRequest(uri = url)).onComplete { response =>
//      context.self ! response
//    }
//  }
//
//  private def listen(): Unit = {
//    while (true) {
//      sendRequest("getUpdates", s"offset=${globalOffset + currentOffset}&timeout=$getUpdatesTimeout")
//      println("SENDED")
//      currentOffset += 1
//    }
//  }
//
//  override def preStart(): Unit = {
//    listen()
//  }
//
//  override def receive: Receive = {
//    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
//      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
//        log.info("Got response, body: " + body.utf8String)
//      }
//    case resp @ HttpResponse(code, _, _, _) =>
//      log.info("Request failed, response code: " + code)
//      resp.discardEntityBytes()
//  }


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
    case class BufferedUpdates(buffer: ArrayBuffer[String], lastUpdateId: Long) extends Data
  }

  case object LoadUpdates
}