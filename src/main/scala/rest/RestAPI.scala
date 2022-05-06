package rest

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.server.Route
import service.NaiveBayesService
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directive.addDirectiveApply
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import play.twirl.api.Html
import views.html.form

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class RestAPI(bayesService: NaiveBayesService)(implicit ec: ExecutionContext) extends FailFastCirceSupport {
  implicit val twirlMarshaller: ToEntityMarshaller[Html] =
    Marshaller.withFixedContentType(ContentTypes.`text/html(UTF-8)`) { html =>
      HttpEntity(ContentTypes.`text/html(UTF-8)`, html.body)
    }

  def testRoute: Route = {
    path("test") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>TEST</h1>"))
      }
    }
  }

  def getClassRoute: Route = {
    path("get_class_p") {
      get {
        parameter("text".as[String]) { text =>
          val textClassFuture = bayesService.getTextClass(text)
          onSuccess(textClassFuture) { value =>
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>$value</h1>"))
          }
        }
      }
    }
  }

  def getClassForm: Route = {
    path("get_class") {
      complete(form(None, None, None))
    }
  }

  def classifyFormData: Route = {
    path("classify_type") {
      post {
        formFields("text", "category", "debug") { (text, _, _) =>
          onSuccess(bayesService.getTextClass(text)).map {
            case "-1" => "негативный"
            case "0" => "нейтральный"
            case "1" => "позитивный"
          }.apply(result => complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>$result</h1>")))
        }
      }
    }
  }

  def classifyFormDataWithHighlights: Route = {
    path("classify") {
      post {
        formFields("text", "category", "debug") { (text, _, _) =>

          onSuccess(bayesService.getTextClassWithHighlights(text)).apply { (classType, text) =>
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
              s"<div><h1>$classType</h1></div>" +
              s"<div><p>$text</p></div>"))
          }
        }
      }
    }
  }

  def routes: Route = pathPrefix("bayes") {
    testRoute ~ getClassRoute ~ getClassForm ~ classifyFormData ~ classifyFormDataWithHighlights
  }
}
