package classifier

import classifier.utils.ClassTypes.{ClassType, Neutral}

import math.exp
import utils.{ClassTypes, Utils}


class NaiveBayesClassifier(model: NaiveBayesModel) {
  lazy val statistics: NaiveBayesStatistics = NaiveBayesStatistics(model)

  def calculateProbability(classType: ClassType, text: String): Double = {
    Utils.luceneTokenize(text)
      .map(_.word)
      .map(model.wordLogProbability(classType, _)).sum + model.classLogProbability(classType)
  }

  def classifyLog(text: String): Map[ClassType, Double] = {
    model.classes.map(classType => (classType, calculateProbability(classType, text))).toMap
  }

  def classifyNormal(text: String): Map[ClassType, Double] = {
    val classified: Map[ClassType, Double] = classifyLog(text)
    model.classes
      .map(classType =>
        (classType, 1.0 / (1.0 + (classified - classType).map({
          case (_, value) => exp(value - classified(classType))
        })
          .foldLeft(0.0)(_ + _)))).toMap
  }

  def pickBestClassWithProbability(text: String): (ClassType, Double) = {
    classifyNormal(text).toList.maxBy(_._2)
  }

  def pickBestClass(text: String): ClassType = {
    val result: (ClassType, Double) = pickBestClassWithProbability(text)
    if (result._2 < Utils.probabilityLevel) ClassTypes.Neutral else result._1
  }

  def pickBestClassWithHighlights(text: String): (ClassType, String) = {
    val classType: ClassType = pickBestClass(text)
    if (classType == Neutral) {
      (classType, text)
    } else {
      (classType, statistics.getHighlightedText(classType, text))
    }
  }
}
