package actor

import akka.actor.{Actor, ActorLogging}
import classifier.{NaiveBayesClassifier, NaiveBayesLearningAlgorithm}
import BayesActor._
import classifier.utils.Utils
import com.typesafe.config.{Config, ConfigFactory}
import logger.ServerLogger
import scala.io.Source

/**
 * класс актора, отвечающий за классифицкацию текстов
 */
class BayesActor extends Actor with ActorLogging {
  val algorithm: NaiveBayesLearningAlgorithm = new NaiveBayesLearningAlgorithm

  algorithm.addExamplesFromCsv(Source.fromResource(Utils.csvNegativePath))
  algorithm.addExamplesFromCsv(Source.fromResource(Utils.csvPositivePath))

  val classifier: NaiveBayesClassifier = new NaiveBayesClassifier(algorithm.getModel)
  log.info("Bayes Actor and model is ready")

  override def receive: Receive = {
    case GetTextClass(text) =>
      sender() ! classifier.pickBestClass(text)

    case GetTextClassWithProbability(text) =>
      sender() ! classifier.pickBestClassWithProbability(text)

    case GetTextClassWithHighlights(text) =>
      log.info(s"Input text [$text] is classified: ${classifier.pickBestClassWithProbability(text)}")
      sender() ! classifier.pickBestClassWithHighlights(text)
  }
}

object BayesActor {
  def apply(): BayesActor = new BayesActor

  case class GetTextClass(text: String)

  case class GetTextClassWithProbability(text: String)

  case class GetTextClassWithHighlights(text: String)
}