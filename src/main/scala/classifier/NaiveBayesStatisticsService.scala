package classifier

import classifier.entities.Term
import classifier.utils.Utils._

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

/**
 * класс, предоставляющий сервис статистики
 *
 * @param model модель, содержащая статистику
 */
class NaiveBayesStatisticsService(model: NaiveBayesModel) {
  case class TermCounter(term: Term, amount: Int)

  /**
   * сортируем термы в тексте по частоте в текстах определённого класса в порядке убывания
   * берём первые n нужных терм, которые нужно выделить
   * затем сортируем термы в порядке их вхождения в классифицируемый текст
   *
   * @param tokenizedText токенизированный текст
   * @param classType     класс
   * @return отсортированный по частоте массив соответствий терм и количеству их вхождений в тексты определённого класса
   */
  private def analyzedText(tokenizedText: ArrayBuffer[Term], classType: String): ArrayBuffer[TermCounter] = {
    tokenizedText
      .map(term => TermCounter(term, model.wordCount(classType).getOrElse(term.word, 0)))
      .sortWith((left, right) => left.amount > right.amount).take(toHighlightAmount)
      .sortWith((left, right) => left.term.start < right.term.start)
  }

  /**
   * получаем промаркированный, предварительно классифицированный текст для определёного класса
   *
   * @param classType полученный класс текста
   * @param text      текст
   * @return текст с выделенными словами.
   *         Разметка для выделения и количество слов, которые нужно выделить, задаётся в классе Utils
   */
  def getHighlightedText(classType: String, text: String): String = {
    val tokenizedText: ArrayBuffer[Term] = luceneTokenize(text)
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

object NaiveBayesStatisticsService {
  def apply(model: NaiveBayesModel): NaiveBayesStatisticsService = new NaiveBayesStatisticsService(model)
}