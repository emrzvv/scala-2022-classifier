package actor

import akka.actor.Actor
import classifier.{NaiveBayesClassifier, NaiveBayesLearningAlgorithm}
import BayesActor._
import logger.ServerLogger

import scala.io.Source


class BayesActor extends Actor {
  val algorithm: NaiveBayesLearningAlgorithm = new NaiveBayesLearningAlgorithm

  algorithm.addExamplesFromCsv(Source.fromResource("data/negative.csv"))
  algorithm.addExamplesFromCsv(Source.fromResource("data/positive.csv"))

  val classifier: NaiveBayesClassifier = new NaiveBayesClassifier(algorithm.getModel)
  ServerLogger.logger.info("[MODEL IS READY]")

  override def receive: Receive = {
    case GetTextClass(text) =>
      sender() ! classifier.pickBestClass(text)

    case GetTextClassWithProbability(text) =>
      sender() ! classifier.pickBestClassWithProbability(text)

    case GetTextClassWithHighlights(text) =>
      ServerLogger.logger.info(s"Input text [$text] is classified: ${classifier.pickBestClassWithProbability(text)}")
      sender() ! classifier.pickBestClassWithHighlights(text)
  }
}

object BayesActor {
  def apply(): BayesActor = new BayesActor

  case class GetTextClass(text: String)

  case class GetTextClassWithProbability(text: String)

  case class GetTextClassWithHighlights(text: String)
}