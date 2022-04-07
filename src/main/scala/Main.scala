import classifier.NaiveBayesLearningAlgorithm
import classifier.NaiveBayesClassifier
import classifier.utils.ClassTypes

object Main extends App {
  val model: NaiveBayesLearningAlgorithm = new NaiveBayesLearningAlgorithm
//  model.addExample(ClassTypes.negative, "предоставляю услуги бухгалтера")
//  model.addExample(ClassTypes.negative, "спешите купить виагру")
//  model.addExample(ClassTypes.positive, "надо купить молоко")
//  model.addExample(ClassTypes.neutral, "а может не надо")
//  model.addExample(ClassTypes.neutral, "а может всё-таки надо")
  model.addExamplesFromCsv(getClass.getResource("./classifier/data/positive.csv").getPath)
  model.addExamplesFromCsv(getClass.getResource("./classifier/data/negative.csv").getPath)
  val classifier: NaiveBayesClassifier = new NaiveBayesClassifier(model.getModel)
  println(classifier.classifyLog("надо купить сигареты"))
  println(classifier.classifyNormal("надо купить сигареты"))
}
