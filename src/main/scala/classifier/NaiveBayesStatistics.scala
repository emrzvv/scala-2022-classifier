package classifier

import classifier.entities.Term
import classifier.utils.Utils

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

class NaiveBayesStatistics(model: NaiveBayesModel) {
  def getHighlightedText(classType: String, text: String): String = {
    val tokenizedText: ArrayBuffer[Term] = Utils.luceneTokenize(text)
    val highlightsAmount = math.min(3, tokenizedText.length)

    val analyzed = tokenizedText
      .map(term => (term, model.wordCount(classType).getOrElse(term.word, 0)))
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

object NaiveBayesStatistics {
  def apply(model: NaiveBayesModel): NaiveBayesStatistics = new NaiveBayesStatistics(model)
}