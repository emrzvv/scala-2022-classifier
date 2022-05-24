package classifier

import classifier.entities.Term
import classifier.utils.ClassTypes.ClassType
import classifier.utils.Utils._

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

class NaiveBayesStatistics(model: NaiveBayesModel) {
  case class TermCounter(term: Term, amount: Int)

  private def analyzedText(tokenizedText: Vector[Term], classType: ClassType): Vector[TermCounter] = {
    tokenizedText
      .map(term => TermCounter(term, model.wordCount(classType).getOrElse(term.word, 0)))
      .sortWith((left, right) => left.amount > right.amount).take(toHighlightAmount) // sort by frequency
      .sortWith((left, right) => left.term.start < right.term.start) // sort by term beginning
  }

  def getHighlightedText(classType: ClassType, text: String): String = {
    val tokenizedText: Vector[Term] = luceneTokenize(text)
    val highlightsAmount = math.min(toHighlightAmount, tokenizedText.length)
    val analyzed = analyzedText(tokenizedText, classType)
    val highlighterLengthSum = startHighlighter.length + endHighlighter.length

    @tailrec
    def loop(n: Int = 0, currentText: String = text): String = {
      if (n == highlightsAmount) currentText
      else loop(
        n + 1,
        currentText
          .patch(analyzed(n).term.start + highlighterLengthSum * n, startHighlighter, 0)
          .patch(analyzed(n).term.end + highlighterLengthSum * n + startHighlighter.length, endHighlighter, 0))
    }

    loop()
  }
}

object NaiveBayesStatistics {
  def apply(model: NaiveBayesModel): NaiveBayesStatistics = new NaiveBayesStatistics(model)
}