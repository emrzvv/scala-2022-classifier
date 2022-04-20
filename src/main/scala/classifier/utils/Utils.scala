package classifier.utils


object Utils {
  val probabilityLevel: Double = 0.7

  def naiveTokenize(s: String): String = {
    s.trim
      .replaceAll(raw"[^A-Za-zА-Яа-яё0-9 +]", "")
  }
}
