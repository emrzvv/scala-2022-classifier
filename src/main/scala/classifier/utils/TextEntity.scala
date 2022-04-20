package classifier.utils

import scala.collection.mutable.ArrayBuffer

case class TextEntity(classType: String, tokenizedText: ArrayBuffer[Term])
