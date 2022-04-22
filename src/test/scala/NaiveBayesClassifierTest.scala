import classifier.{NaiveBayesClassifier, NaiveBayesLearningAlgorithm}
import org.specs2.Specification
import org.specs2.matcher.MatchResult
import org.specs2.specification.core.SpecStructure

class NaiveBayesClassifierTest extends Specification {
  val algorithm: NaiveBayesLearningAlgorithm = new NaiveBayesLearningAlgorithm
  algorithm.addExample("-1", "предоставляю услуги бухгалтера")
  algorithm.addExample("-1", "спешите купить виагру")
  algorithm.addExample("1", "надо купить молоко")
  val classifier: NaiveBayesClassifier = new NaiveBayesClassifier(algorithm.getModel)

  override def is: SpecStructure =
    s2"""
        NaiveBayesClassifier specification
        where "надо купить сигареты" classifies as positive (1) class $e1
        where "предоставляю спешите купить молоко" classifies as negative (-1) class $e2
      """

  def e1: MatchResult[String] = {
    val text: String = "надо купить сигареты"
    val bestClass: (String, Double) = classifier.pickBestClass(text)
    bestClass._1 === "1"
  }

  def e2: MatchResult[String] = {
    val text: String = "предоставляю спешите купить молоко"
    val bestClass: (String, Double) = classifier.pickBestClass(text)
    bestClass._1 === "-1"
  }
}
