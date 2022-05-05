package rest

import akka.http.scaladsl.server.Route
import service.NaiveBayesService
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContext

class RestAPI(naiveBayesService: NaiveBayesService)(implicit ec: ExecutionContext) {
  def testRoute: Route = {
    path("test") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>TEST</h1>"))
      }
    }
  }

  def routes: Route = pathPrefix("bayes") {
    testRoute
  }
}
