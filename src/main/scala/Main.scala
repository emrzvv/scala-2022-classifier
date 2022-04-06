import classifier.NaiveBayesLearningAlgorithm
import classifier.NaiveBayesClassifier

object Main extends App {
  val model: NaiveBayesLearningAlgorithm = new NaiveBayesLearningAlgorithm
  model.addExample("NEGATIVE", "предоставляю услуги бухгалтера")
  model.addExample("NEGATIVE", "спешите купить виагру")
  model.addExample("POSITIVE", "надо купить молоко")
  val classifier: NaiveBayesClassifier = new NaiveBayesClassifier(model.getModel())
  println(classifier.classifyLog("надо купить сигареты"))
  println(classifier.classifyNormal("надо купить сигареты"))
}
