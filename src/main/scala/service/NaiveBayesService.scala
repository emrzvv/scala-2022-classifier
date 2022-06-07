package service

import actor.BayesActor._
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import classifier.entities.ClassificationWithStatisticsResult
import classifier.utils.ClassTypes.ClassType

import scala.language.postfixOps
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
 * сервис для классификации текстов
 *
 * @param bayesActor актор, отвечающий за обучение модели и классификацию текстов
 */
class NaiveBayesService(bayesActor: ActorRef) {
  implicit val timeout: Timeout = Timeout(10 seconds)

  /**
   * получаем только класс текста
   *
   * @param text текст
   * @return future от класса
   */
  def getTextClass(text: String): Future[ClassType] =
    (bayesActor ask GetTextClass(text)).mapTo[ClassType]

  /**
   * получаем класс текста и сам текст с промаркированными словами
   *
   * @param text текст
   * @return future от пары класс-выделенный_текст
   */
  def getTextClassWithHighlights(text: String): Future[ClassificationWithStatisticsResult] =
    (bayesActor ask GetTextClassWithHighlights(text)).mapTo[ClassificationWithStatisticsResult]
}