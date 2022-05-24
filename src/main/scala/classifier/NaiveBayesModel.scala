package classifier

import classifier.utils.ClassTypes.ClassType

import scala.math.log

class NaiveBayesModel(val docLengths: Map[ClassType, Int],
                      val docCount: Map[ClassType, Int],
                      val wordCount: Map[ClassType, Map[String, Int]],
                      val dictionarySize: Int) {

  val classes: Set[ClassType] = docCount.keySet

  def classLogProbability(c: ClassType): Double = {
    log(docCount(c).toDouble / docCount.values.sum)
  }

  def wordLogProbability(c: ClassType, w: String): Double = {
    log((wordCount(c).getOrElse(w, 0) + 1.0) / (dictionarySize + docLengths(c).toDouble))
  }
}
