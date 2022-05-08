package service

import actor.BayesActor._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

import scala.language.postfixOps
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class NaiveBayesService(bayesActor: ActorRef) {
  implicit val timeout: Timeout = Timeout(10 seconds)

  def getTextClass(text: String): Future[String] =
    (bayesActor ? GetTextClass(text)).mapTo[String]

  def getTextClassWithHighlights(text: String): Future[(String, String)] =
    (bayesActor ? GetTextClassWithHighlights(text)).mapTo[(String, String)]
}