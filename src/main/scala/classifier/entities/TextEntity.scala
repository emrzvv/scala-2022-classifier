package classifier.entities

import scala.collection.mutable.ArrayBuffer

case class TextEntity(classType: String, tokenizedText: ArrayBuffer[Term])
