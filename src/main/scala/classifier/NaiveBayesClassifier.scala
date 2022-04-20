package classifier

import math.exp
import utils.{ClassTypes, Utils}

class NaiveBayesClassifier(model: NaiveBayesModel) {
  def calculateProbability(classType: String, text: String): Double = {
    Utils.naiveTokenize(text).split(" ")
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

  def pickBestClassWithProbability(text: String): (String, Double) = {
    classifyNormal(text).toList.maxBy(_._2)
  }

  def pickBestClass(text: String): String = {
    val result: (String, Double) = pickBestClassWithProbability(text)
    if (result._2 < Utils.probabilityLevel) ClassTypes.csvNeutral else result._1
  }
}
