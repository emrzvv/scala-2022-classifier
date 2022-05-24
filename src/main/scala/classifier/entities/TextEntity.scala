package classifier.entities

import classifier.utils.ClassTypes.ClassType

case class TextEntity(classType: ClassType, tokenizedText: Vector[Term])
