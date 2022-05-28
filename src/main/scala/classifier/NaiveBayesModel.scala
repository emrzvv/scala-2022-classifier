package classifier

import classifier.utils.ClassTypes.ClassType

import scala.math.log

/**
 * модель классификатора, подсчитывающая логарифмическую априорную вероятность класса P(c)
 * и логарифмическую вероятность слова в пределах класса P(w|c)
 *
 * далее описана статистика выборки, необходимая для вычисления вероятностей
 * документ == текст
 *
 * @param docLengths     соответствие класса к суммарной длине всех документов (длина документа - кол-во слов) этого класса
 * @param docCount       соответствие класса к количеству документов в нём
 * @param wordCount      соответствие класса к суммарному количеству слов, которые входят в документы данного класса
 * @param dictionarySize количество слов
 */
class NaiveBayesModel(val docLengths: Map[ClassType, Int],
                      val docCount: Map[ClassType, Int],
                      val wordCount: Map[ClassType, Map[String, Int]],
                      val dictionarySize: Int) {

  val classes: Set[ClassType] = docCount.keySet

  /**
   * @param c класс
   * @return логарифм априорной вероятности класса P(c)
   */
  def classLogProbability(c: ClassType): Double = {
    log(docCount(c).toDouble / docCount.values.sum)
  }

  /**
   * @param c класс
   * @param w слово
   * @return логарифм вероятности слова в пределах класса P(w|c)
   */
  def wordLogProbability(c: ClassType, w: String): Double = {
    log((wordCount(c).getOrElse(w, 0) + 1.0) / (dictionarySize + docLengths(c).toDouble))
  }
}
