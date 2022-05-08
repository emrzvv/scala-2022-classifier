package actor

import akka.actor.Actor
import classifier.{NaiveBayesClassifier, NaiveBayesLearningAlgorithm}
import BayesActor._
import akka.util.Timeout
import classifier.utils.Utils
import classifier.utils.ClassTypes._
import logger.ServerLogger

import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class BayesActor extends Actor {
  val algorithm: NaiveBayesLearningAlgorithm = new NaiveBayesLearningAlgorithm

  algorithm.addExamplesFromCsv(Utils.negativeCsvPath)
  algorithm.addExamplesFromCsv(Utils.positiveCsvPath)

  val classifier: NaiveBayesClassifier = new NaiveBayesClassifier(algorithm.getModel)
  ServerLogger.logger.info("[MODEL IS READY]")

  override def receive: Receive = {
    case GetTextClass(text) =>
      sender() ! classifier.pickBestClass(text)

    case GetTextClassWithProbability(text) =>
      sender() ! classifier.pickBestClassWithProbability(text)

    case GetTextClassWithHighlights(text) =>
      ServerLogger.logger.info(s"Input text [$text] is classified: ${classifier.pickBestClassWithProbability(text)}")
      sender() ! (classifier.pickBestClassWithHighlights(text) match {
        case (classType, resultText) if classType == csvNegative => (readableNegative, resultText)
        case (classType, resultText) if classType == csvNeutral => (readableNeutral, resultText)
        case (classType, resultText) if classType == csvPositive => (readablePositive, resultText)
      })
  }
}

object BayesActor {
  case class GetTextClass(text: String)

  case class GetTextClassWithProbability(text: String)

  case class GetTextClassWithHighlights(text: String)
}