package classifier.entities

import classifier.utils.ClassTypes.ClassType

case class ClassificationResult(classType: ClassType, probability: Double)
