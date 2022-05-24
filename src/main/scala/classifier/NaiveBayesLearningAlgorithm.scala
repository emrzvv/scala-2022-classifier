package classifier

import classifier.entities.TextEntity
import classifier.utils.ClassTypes.ClassType
import com.github.tototoshi.csv.{CSVReader, DefaultCSVFormat}
import utils.{ClassTypes, Utils}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

class NaiveBayesLearningAlgorithm() {
  private val examples: Vector[TextEntity] =
    addExamplesFromCsv(Source.fromResource("data/negative.csv")) ++
      addExamplesFromCsv(Source.fromResource("data/positive.csv"))

  def dictionary(): Set[String] = examples.flatMap(e => e.tokenizedText.map(term => term.word)).toSet

  def getModel: NaiveBayesModel = {
    // all text are already tokenized

    val docsByClass = examples
      .groupBy(_.classType)
      .map({ case (key, arrayValue) => (key, arrayValue.map(_.tokenizedText)) })

    val classToWords = docsByClass.map({ case (key, arrayValue) => (key, arrayValue.flatten) })

    val docLengths = classToWords.map({ case (key, words) => (key, words.length) })

    val docCount = docsByClass
      .map({ case (key, arrayValue) => (key, arrayValue.length) })

    val wordCount = classToWords
      .map({
        case (key, words) => (key, words.groupMapReduce(w => w.word)(_ => 1)(_ + _))
      })

    new NaiveBayesModel(docLengths, docCount, wordCount, dictionary().size)
  }

  def addExamplesFromCsv(path: Source): Vector[TextEntity] = {
    implicit val format: DefaultCSVFormat = new DefaultCSVFormat {
      override val escapeChar: Char = '\"'
      override val delimiter: Char = ';'
    }

    val reader = CSVReader.open(path)

    val data = reader.all().map(col => entities.TextEntity(col(4) match {
      case "-1" => ClassTypes.Negative
      case "1" => ClassTypes.Positive
      case "0" => ClassTypes.Neutral
    }, Utils.luceneTokenize(col(3))))

    reader.close()

    data.toVector
  }
}
