package actor

import akka.actor.{Actor, ActorLogging}
import classifier.{NaiveBayesClassifier, NaiveBayesLearningAlgorithm}
import BayesActor._
import classifier.utils.Utils
import classifier.utils.ClassTypes._
import logger.ServerLogger

/**
 * класс актора, отвечающий за классифицкацию текстов
 */
class BayesActor extends Actor with ActorLogging {
  val algorithm: NaiveBayesLearningAlgorithm = new NaiveBayesLearningAlgorithm

  algorithm.addExamplesFromCsv(Utils.negativeCsvPath)
  algorithm.addExamplesFromCsv(Utils.positiveCsvPath)

  val classifier: NaiveBayesClassifier = new NaiveBayesClassifier(algorithm.getModel)
  log.info("Bayes Actor and model is ready")

  override def receive: Receive = {
    case GetTextClass(text) =>
      sender() ! classifier.pickBestClass(text)

    case GetTextClassWithProbability(text) =>
      sender() ! classifier.pickBestClassWithProbability(text)

    case GetTextClassWithHighlights(text) =>
      log.info(s"Input text [$text] is classified: ${classifier.pickBestClassWithProbability(text)}")

      /**
       * ставим в соответствие классам, описывающим окрас текста в классификаторе
       * "человекочитаемые" класса
       */
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