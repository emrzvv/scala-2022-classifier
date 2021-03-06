import classifier.NaiveBayesLearningAlgorithm
import classifier.NaiveBayesClassifier
import classifier.utils.ClassTypes

object Main extends App {
  def time[R](block: => R)(message: String): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println(s"$message elapsed time: " + (t1 - t0).toDouble / 1_000_000_000.0 + "s")
    result
  }

  val algorithm: NaiveBayesLearningAlgorithm = new NaiveBayesLearningAlgorithm
  val posTimeAdding = time {algorithm.addExamplesFromCsv(getClass.getResource("./classifier/data/positive.csv").getPath)}("adding positive texts")
  val negTimeAdding = time {algorithm.addExamplesFromCsv(getClass.getResource("./classifier/data/negative.csv").getPath)}("adding negative texts")

  val classifier: NaiveBayesClassifier = time {new NaiveBayesClassifier(algorithm.getModel)}("creating and training model")

  val text = "ура снова праздник!!!"

  val resLog = time { classifier.classifyLog(text) }("calculating logs probability")
  val resNorm = time { classifier.classifyNormal(text) }("calculating normal probability")

  println(resLog)
  println(resNorm)

  println(classifier.pickBestClass(text))
}
