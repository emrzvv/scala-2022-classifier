package actor

import akka.actor.Actor
import classifier.{NaiveBayesClassifier, NaiveBayesLearningAlgorithm}
import BayesActor._

class BayesActor extends Actor {
  val algorithm: NaiveBayesLearningAlgorithm = new NaiveBayesLearningAlgorithm
  algorithm.addExamplesFromCsv(getClass.getResource("../classifier/data/positive.csv").getPath)
  algorithm.addExamplesFromCsv(getClass.getResource("../classifier/data/negative.csv").getPath)
  // println(getClass.getResource("../classifier/data/positive.csv").getPath)

  val classifier: NaiveBayesClassifier = new NaiveBayesClassifier(algorithm.getModel)
  println("[MODEL IS READY]")
  override def receive: Receive = {
    case GetTextClass(text) =>
      println(text)
      println(classifier.pickBestClass(text))
      sender() ! classifier.pickBestClass(text)

    case GetTextClassWithProbability(text) =>
      sender() ! classifier.pickBestClassWithProbability(text)
  }
}

object BayesActor {
  case class GetTextClass(text: String)
  case class GetTextClassWithProbability(text: String)
}