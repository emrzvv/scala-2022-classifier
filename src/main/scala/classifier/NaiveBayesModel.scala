package classifier

import scala.math.log

class NaiveBayesModel(docLengths: Map[String, Int],
                      docCount: Map[String, Int],
                      wordCount: Map[String, Map[String, Int]],
                      dictionarySize: Int) {

  val classes: Set[String] = docCount.keySet

  def classLogProbability(c: String): Double = {
    log(docCount(c).toDouble / docCount.values.sum)
  }

  def wordLogProbability(c: String, w: String): Double = {
    log((wordCount(c).getOrElse(w, 0) + 1.0) / (dictionarySize + docLengths(c).toDouble))
  }
}
