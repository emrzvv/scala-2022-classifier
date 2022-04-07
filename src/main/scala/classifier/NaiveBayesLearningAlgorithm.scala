package classifier

import com.github.tototoshi.csv.{CSVReader, DefaultCSVFormat}
import utils.TextEntity
import utils.StringUtils

import scala.collection.mutable.ArrayBuffer

class NaiveBayesLearningAlgorithm() {
  private val examples: ArrayBuffer[TextEntity] = ArrayBuffer.newBuilder[TextEntity].result()

  def dictionary(): Set[String] = examples.flatMap(e => e.text.split(" ")).toSet

  def addExample(example: TextEntity): Unit = examples.addOne(TextEntity(example.classType, StringUtils.naiveTokenize(example.text)))

  def addExample(classType: String, text: String): Unit = examples.addOne(TextEntity(classType, StringUtils.naiveTokenize(text)))

  def addAllExamples(textsEntities: List[TextEntity]): Unit = examples.addAll(textsEntities)

  def getModel: NaiveBayesModel = {
    val docsByClass = examples
      .groupBy(_.classType)
      .map({case (key, arrayValue) => (key, arrayValue.map(_.text))})

    val docLengths = docsByClass
      .map({case (key, arrayValue) => (key, arrayValue.map(_.split(" ").length).sum)})

    val docCount = docsByClass
      .map({case (key, arrayValue) => (key, arrayValue.length)})

    val wordCount = docsByClass
      .map({
        case (key, arrayValue) =>
          (key, arrayValue.flatMap(_.split(" ")).groupMapReduce(w => w)(_ => 1)(_ + _))
      })

    new NaiveBayesModel(docLengths, docCount, wordCount, dictionary().size)
  }

  def addExamplesFromCsv(path: String): Unit = {
    implicit val format: DefaultCSVFormat = new DefaultCSVFormat {
      override val escapeChar: Char = '\"'
      override val delimiter: Char = ';'
    }

    val reader = CSVReader.open(path)

    addAllExamples(reader.all().map(col => TextEntity(col(4), col(3))))
    // examples.foreach(println)
  }
}
