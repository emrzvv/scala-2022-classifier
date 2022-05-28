package classifier

import classifier.entities.{ClassificationResult, ClassificationWithStatisticsResult}
import classifier.utils.ClassTypes.{ClassType, Neutral}

import math.exp
import utils.{ClassTypes, Utils}

/**
 * алгоритм классификации
 *
 * @param model модель классификатора со статистикой
 *              и методами вычисления вероятности класса и вероятности слова в классе
 */
class NaiveBayesClassifier(model: NaiveBayesModel) {
  /**
   * сервис, предоставляющий выделение статистики для пользователя: подсветка маркерных слов в тексте
   */
  lazy val statistics: NaiveBayesStatisticsService = NaiveBayesStatisticsService(model)

  /**
   * рассчёт вероятности документа в пределах класса
   *
   * @param classType класс
   * @param text      текст документа
   * @return оценка P(c|d)
   */
  def calculateProbability(classType: ClassType, text: String): Double = {
    Utils.luceneTokenize(text)
      .map(_.word)
      .map(model.wordLogProbability(classType, _)).sum + model.classLogProbability(classType)
  }

  /**
   * рассчёт логарифмической вероятности текста для всех классов
   *
   * @param text текст
   * @return отображение класс -> логарифмическая вероятность
   */
  def classifyLog(text: String): Map[ClassType, Double] = {
    model.classes.map(classType => (classType, calculateProbability(classType, text))).toMap
  }

  /**
   * расччёт классической [0, 1] вероятности текста для всех классов.
   * P(c|d) = 1 / (1 + sum_{by c' in classes \ c}(e ^^ [log_p(c') - log_p(c)]))
   *
   * @param text текст
   * @return отображение класс -> вероятность
   */
  def classifyNormal(text: String): Map[ClassType, Double] = {
    val classified: Map[ClassType, Double] = classifyLog(text)
    model.classes
      .map(classType =>
        (classType, 1.0 / (1.0 + (classified - classType).map({
          case (_, value) => exp(value - classified(classType))
        }).sum))).toMap

  /**
   * выбор текста с лучшей вероятностью
   *
   * @param text текст
   * @return класс и лучшая вероятность
   */
  def pickBestClassWithProbability(text: String): ClassificationResult = {
    ClassificationResult tupled classifyNormal(text).toList.maxBy(_._2)

  /**
   * выбор текста с лучшей вероятностью
   *
   * @param text текст
   * @return класс, которому соответствует лучшая вероятность
   */
  def pickBestClass(text: String): ClassType = {
    val result: ClassificationResult = pickBestClassWithProbability(text)
    if (result.probability < Utils.probabilityLevel) ClassTypes.Neutral else result.classType
  }

  /**
   * выбор текста с лучшей вероятностью и выделение маркерных слов
   * маркерные слова выбираются из статистики, содержащейся в модели, по принципу наибольшего вхождения во все тексты
   *
   * @param text текст
   * @return класс и размеченный текст
   */
  def pickBestClassWithHighlights(text: String): ClassificationWithStatisticsResult = {
    val classType: ClassType = pickBestClass(text)
    if (classType == Neutral) {
      ClassificationWithStatisticsResult(classType, text)
    } else {
      ClassificationWithStatisticsResult(classType, statistics.getHighlightedText(classType, text))
    }
  }
}
