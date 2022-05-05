package rest

import akka.http.scaladsl.server.Route
import service.NaiveBayesService
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class RestAPI(bayesService: NaiveBayesService)(implicit ec: ExecutionContext) extends FailFastCirceSupport {
  def testRoute: Route = {
    path("test") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>TEST</h1>"))
      }
    }
  }

  def getClassRoute: Route = {
    path("get_class") {
      get {
        parameter("text".as[String]) { text =>
          val textClassFuture = bayesService.getTextClass(text)

          onSuccess(textClassFuture) {
            case value => complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>$value</h1>"))
            case _ => complete(StatusCodes.InternalServerError)
          }
        }
      }
    }
  }


  def routes: Route = pathPrefix("bayes") {
    testRoute ~ getClassRoute
  }
}
