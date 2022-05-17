package classifier

import classifier.entities.{Term, TextEntity}
import com.github.tototoshi.csv.{CSVReader, DefaultCSVFormat}
import utils.Utils

import scala.collection.mutable.ArrayBuffer

/**
 * обучающий алгоритм классификации
 */
class NaiveBayesLearningAlgorithm() {
  /**
   * токенизированная выборка
   */
  private val examples: ArrayBuffer[TextEntity] = ArrayBuffer.newBuilder[TextEntity].result()

  def dictionary(): Set[String] = examples.flatMap(e => e.tokenizedText.map(term => term.word)).toSet

  def addExample(example: TextEntity): Unit =
    examples.addOne(example)

  def addExample(classType: String, text: String): Unit =
    examples.addOne(entities.TextEntity(classType, Utils.luceneTokenize(text)))

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

  /**
   * чтение текстов из выборки в ./src/main/scala/classifier/data и запись
   *
   * @param path абсолютный путь файла с текстами
   */
  def addExamplesFromCsv(path: String): Unit = {
    implicit val format: DefaultCSVFormat = new DefaultCSVFormat {
      override val escapeChar: Char = '\"'
      override val delimiter: Char = ';'
    }

    val reader = CSVReader.open(path)

    addAllExamples(reader.all().map(col => entities.TextEntity(col(4), Utils.luceneTokenize(col(3)))))
  }
}
