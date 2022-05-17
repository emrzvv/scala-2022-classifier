package classifier.utils

import classifier.entities.Term
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.tokenattributes.{CharTermAttribute, OffsetAttribute}

import java.nio.file.Paths
import scala.collection.mutable.ArrayBuffer


object Utils {
  val dataFolder: String = "/src/main/scala/classifier/data"
  val negativeCsvPath: String = Paths.get(".").toAbsolutePath.toString + dataFolder + "/negative.csv"
  val positiveCsvPath: String = Paths.get(".").toAbsolutePath.toString + dataFolder + "/positive.csv"

  val probabilityLevel: Double = 0.7

  val startHighlighter: String = "<b>"
  val endHighlighter: String = "</b>"
  val toHighlightAmount: Int = 3

  def naiveTokenize(s: String): String = {
    s.trim
      .replaceAll(raw"[^A-Za-zА-Яа-яё0-9 +]", "")
  }

  // альтернативное решение: применить кмп, найдя все вхождения паттернов @ и http в строку,
  // для каждого полученного вхождения пройтись до пробела, найти длину
  // конкатенировать строку слева и справа от исключаемого слова
  private def cleanGarbageNaive(s: String): String =
    s.split("\\s+").filterNot(word => word.startsWith("@") || word.startsWith("http")).mkString(" ")

  def luceneTokenize(s: String): ArrayBuffer[Term] = {
    val analyzer = new RussianAnalyzer()
    val ts = analyzer.tokenStream("text", cleanGarbageNaive(s))
    ts.reset()

    val out = new ArrayBuffer[Term]()

    // исключаем "мусорные" слова
    val toExclude = Set("rt", "эт", "прост", "хоч", "так", "поч", "са", "сво")

    while (ts.incrementToken()) {
      val word = ts.getAttribute(classOf[CharTermAttribute]).toString
      val offsets = ts.getAttribute(classOf[OffsetAttribute])
      if (!toExclude.contains(word)) out.addOne(Term(word, offsets.startOffset(), offsets.endOffset()))
    }
    out
  }
}
