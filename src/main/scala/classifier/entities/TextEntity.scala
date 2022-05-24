package classifier.entities

import classifier.utils.ClassTypes.ClassType

import scala.collection.mutable.ArrayBuffer

case class TextEntity(classType: ClassType, tokenizedText: ArrayBuffer[Term])
