package classifier

import classifier.utils.{Term, Utils}

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
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

  def getHighlightedText(classType: String, text: String): String = {
    val tokenizedText: ArrayBuffer[Term] = Utils.luceneTokenize(text)
    val highlightsAmount = math.min(3, tokenizedText.length)

    val analyzed = tokenizedText
      .map(term => (term, wordCount(classType).getOrElse(term.word, 0)))
      .sortWith((t1, t2) => t1._2 > t2._2).take(3)
      .sortWith((t1, t2) => t1._1.start < t2._1.start)

    val highlighterLengthSum = Utils.startHighlighter.length + Utils.endHighlighter.length

    @tailrec
    def loop(n: Int = 0, currentText: String = text): String = {
      if (n == highlightsAmount) currentText
      else loop(
        n + 1,
        currentText
          .patch(analyzed(n)._1.start + highlighterLengthSum * n, Utils.startHighlighter, 0)
          .patch(analyzed(n)._1.end + highlighterLengthSum * n + Utils.startHighlighter.length, Utils.endHighlighter, 0))
    }

    loop()
  }
}
