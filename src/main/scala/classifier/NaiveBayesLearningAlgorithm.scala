package classifier

import classifier.entities.TextEntity
import classifier.utils.ClassTypes.ClassType
import com.github.tototoshi.csv.{CSVReader, DefaultCSVFormat}
import utils.{ClassTypes, Utils}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.Using

class NaiveBayesLearningAlgorithm() {
  private val examples: ArrayBuffer[TextEntity] = ArrayBuffer.newBuilder[TextEntity].result()

  def dictionary(): Set[String] = examples.flatMap(e => e.tokenizedText.map(term => term.word)).toSet

  def addExample(classType: ClassType, text: String): Unit =
    examples.addOne(TextEntity(classType, Utils.luceneTokenize(text)))

  def addAllExamples(textsEntities: List[TextEntity]): Unit =
    examples.addAll(textsEntities)

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

  def addExamplesFromCsv(path: Source): Unit = {
    implicit val format: DefaultCSVFormat = new DefaultCSVFormat {
      override val escapeChar: Char = '\"'
      override val delimiter: Char = ';'
    }

    Using(CSVReader.open(path)) { reader =>
      addAllExamples(reader.all().map(col => entities.TextEntity(col(4) match {
        case "-1" => ClassTypes.Negative
        case "1" => ClassTypes.Positive
        case "0" => ClassTypes.Neutral
      }, Utils.luceneTokenize(col(3)))))
      reader.close()
    }
  }
}
