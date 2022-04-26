import classifier.{NaiveBayesClassifier, NaiveBayesLearningAlgorithm}
import classifier.utils.ClassTypes.{csvNeutral, csvNegative, csvPositive}
import org.specs2.Specification
import org.specs2.matcher.MatchResult
import org.specs2.specification.core.SpecStructure


class NaiveBayesClassifierTest extends Specification {
  val algorithm: NaiveBayesLearningAlgorithm = new NaiveBayesLearningAlgorithm
  algorithm.addExample(csvNegative, "предоставляю услуги бухгалтера")
  algorithm.addExample(csvNegative, "спешите купить виагру")
  algorithm.addExample(csvPositive, "надо купить молоко")
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

      """

  def e1: MatchResult[String] = {
    val text: String = "надо купить сигареты"
    val bestClass: String = classifier.pickBestClass(text)
    bestClass === csvNeutral
  }

  def e2: MatchResult[String] = {
    val text: String = "предоставляю спешите купить молоко, виагру и услуги бухгалтера"
    val bestClass: String = classifier.pickBestClass(text)
    bestClass === csvNegative
  }

  def e3: MatchResult[Any] = {
    val text: String = "Падает если попытаться классифицировать строку, содержащую незнакомые слова.\nNoSuchElementException: key not found: коллег\nДавай еще тесты напишем для нового режима классификации"
    classifier.pickBestClassWithHighlights(text) must_!=throwA[NoSuchElementException]
  }
}
