package classifier.entities

import classifier.utils.ClassTypes.ClassType

case class ClassificationWithStatisticsResult(classType: ClassType, highlightedText: String)
