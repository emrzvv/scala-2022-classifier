package classifier

import math.exp

import utils.StringUtils

class NaiveBayesClassifier(model: NaiveBayesModel) {
  def calculateProbability(classType: String, text: String): Double = {
    StringUtils.naiveTokenize(text).split(" ")
      .map(model.wordLogProbability(classType, _)).sum + model.classLogProbability(classType)
  }

  def classifyLog(text: String): Map[String, Double] = {
    model.classes.map(classType => (classType, calculateProbability(classType, text))).toMap
  }

  def classifyNormal(text: String): Map[String, Double] = {
    val classified: Map[String, Double] = classifyLog(text)
    model.classes
      .map(classType =>
        (classType, 1.0 / (1.0 + (classified - classType).map({
          case (_, value) => exp(value - classified(classType))})
          .foldLeft(0.0)(_ + _)))).toMap
  }

  def pickBestClass(text: String): (String, Double) = {
    classifyNormal(text).toList.maxBy(_._2)
  }
}
