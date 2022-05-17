package classifier

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
class NaiveBayesModel(val docLengths: Map[String, Int],
                      val docCount: Map[String, Int],
                      val wordCount: Map[String, Map[String, Int]],
                      val dictionarySize: Int) {

  val classes: Set[String] = docCount.keySet

  /**
   * @param classType класс
   * @return логарифм априорной вероятности класса P(c)
   */
  def classLogProbability(classType: String): Double = {
    log(docCount(classType).toDouble / docCount.values.sum)
  }

  /**
   * @param classType класс
   * @param word      слово
   * @return логарифм вероятности слова в пределах класса P(w|c)
   */
  def wordLogProbability(classType: String, word: String): Double = {
    log((wordCount(classType).getOrElse(word, 0) + 1.0) / (dictionarySize + docLengths(classType).toDouble))
  }
}
