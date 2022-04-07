package classifier.utils

import scala.collection.mutable.ArrayBuffer

object StringUtils {
  def naiveTokenize(s: String): String = {
    s.trim
      .replaceAll(raw"[^A-Za-zА-Яа-яё0-9 +]", "")
  }
}
