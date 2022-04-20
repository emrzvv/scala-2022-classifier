package classifier.utils

import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.tokenattributes.{CharTermAttribute, OffsetAttribute}

import scala.collection.mutable.ArrayBuffer


object Utils {
  val probabilityLevel: Double = 0.7

  def naiveTokenize(s: String): String = {
    s.trim
      .replaceAll(raw"[^A-Za-zА-Яа-яё0-9 +]", "")
  }

  def luceneTokenize(s: String): ArrayBuffer[Term] = {
    val analyzer = new RussianAnalyzer()
    val ts = analyzer.tokenStream("text", s)
    ts.reset()

    val out = new ArrayBuffer[Term]()

    while (ts.incrementToken()) {
      val word = ts.getAttribute(classOf[CharTermAttribute]).toString
      val offsets = ts.getAttribute(classOf[OffsetAttribute])
      out.addOne(Term(word, offsets.startOffset(), offsets.endOffset()))
    }
    out
  }
}
