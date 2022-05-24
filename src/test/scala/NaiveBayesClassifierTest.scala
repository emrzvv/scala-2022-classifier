import classifier.{NaiveBayesClassifier, NaiveBayesLearningAlgorithm}
import classifier.utils.ClassTypes._
import org.specs2.Specification
import org.specs2.matcher.MatchResult
import org.specs2.matcher.Matchers.beTrue
import org.specs2.specification.core.SpecStructure


class NaiveBayesClassifierTest extends Specification {
  val algorithm: NaiveBayesLearningAlgorithm = new NaiveBayesLearningAlgorithm
  algorithm.addExample(Negative, "предоставляю услуги бухгалтера")
  algorithm.addExample(Negative, "спешите купить виагру")
  algorithm.addExample(Positive, "нужно купить молоко")
  val classifier: NaiveBayesClassifier = new NaiveBayesClassifier(algorithm.getModel)

  override def is: SpecStructure =
    s2"""
        NaiveBayesClassifier specification

        where "надо купить сигареты" classifies as neutral (0) class with probability level 0.7 $e1
        where "предоставляю спешите купить молоко и виагру" classifies as positive (1) class $e2
        where
        "Падает если попытаться классифицировать строку, содержащую незнакомые слова.\nNoSuchElementException:
        key not found: коллег\nДавай еще тесты напишем для нового режима классификации"

        doesn't throw exception if it meets new words: $e3
        classifies samples correctly: $e4

      """

  def e1: MatchResult[ClassType] = {
    val text: String = "надо купить сигареты"
    val bestClass: ClassType = classifier.pickBestClass(text)
    bestClass === Neutral
  }

  def e2: MatchResult[ClassType] = {
    val text: String = "предоставляю спешите купить молоко, виагру и услуги бухгалтера"
    val bestClass: ClassType = classifier.pickBestClass(text)
    bestClass === Negative
  }

  def e3: MatchResult[Any] = {
    val text: String = "Падает если попытаться классифицировать строку, содержащую незнакомые слова.\nNoSuchElementException: key not found: коллег\nДавай еще тесты напишем для нового режима классификации"
    classifier.pickBestClassWithHighlights(text) must_!=throwA[NoSuchElementException]
  }

  def e4: MatchResult[Boolean] = {
    val text: String = "нужно купить сигареты"
    val result: Map[ClassType, Double] = classifier.classifyLog(text)

    val eps: Double = 10e-4
    (math.abs(-7.629 - result(Negative)) <= eps &&
      math.abs(-6.906 - result(Positive)) <= eps) must beTrue
  }
}
