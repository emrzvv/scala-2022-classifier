package service

import actor.BayesActor._
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import classifier.entities.ClassificationWithStatisticsResult
import classifier.utils.ClassTypes.ClassType

import scala.language.postfixOps
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class NaiveBayesService(bayesActor: ActorRef) {
  implicit val timeout: Timeout = Timeout(10 seconds)

  def getTextClass(text: String): Future[ClassType] =
    (bayesActor ask GetTextClass(text)).mapTo[ClassType]

  def getTextClassWithHighlights(text: String): Future[ClassificationWithStatisticsResult] =
    (bayesActor ask GetTextClassWithHighlights(text)).mapTo[ClassificationWithStatisticsResult]
}