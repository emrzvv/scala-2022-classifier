package classifier

import classifier.entities.{Term, TextEntity}
import classifier.utils.ClassTypes.ClassType
import com.github.tototoshi.csv.{CSVReader, DefaultCSVFormat}
import utils.{ClassTypes, Utils}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.Using

/**
 * обучающий алгоритм классификации
 */
class NaiveBayesLearningAlgorithm() {
  /**
   * токенизированная выборка
   */
  private val examples: ArrayBuffer[TextEntity] = ArrayBuffer.newBuilder[TextEntity].result()

  def dictionary(): Set[String] = examples.flatMap(e => e.tokenizedText.map(term => term.word)).toSet

  def addExample(classType: ClassType, text: String): Unit =
    examples.addOne(TextEntity(classType, Utils.luceneTokenize(text)))

  def addAllExamples(textsEntities: List[TextEntity]): Unit =
    examples.addAll(textsEntities)

  def getModel: NaiveBayesModel = {
    /**
     * группируем тексты по классу
     */
    val docsByClass: Map[String, ArrayBuffer[ArrayBuffer[Term]]] = examples
      .groupBy(_.classType)
      .map({ case (key, arrayValue) => (key, arrayValue.map(_.tokenizedText)) })

    /**
     * группируем слова по классу
     */
    val classToWords: Map[String, ArrayBuffer[Term]] = docsByClass
      .map({ case (key, arrayValue) => (key, arrayValue.flatten) })

    /**
     * ставим в соответствие каждому классу количество слов в нём
     */
    val docLengths = classToWords.map({ case (key, words) => (key, words.length) })

    /**
     * ставим в соответствие каждому классу количество документов в нём
     */
    val docCount: Map[String, Int] = docsByClass.map({ case (key, arrayValue) => (key, arrayValue.length) })

    /**
     * ставим в соответствие каждому классу статистику:
     * слово -> количество слов в текстах данного класса
     */
    val wordCount: Map[String, Map[String, Int]] = classToWords
      .map({ case (key, words) => (key, words.groupMapReduce(w => w.word)(_ => 1)(_ + _)) })

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
