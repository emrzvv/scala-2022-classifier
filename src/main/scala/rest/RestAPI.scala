package rest

import org.mdedetrich.akka.http.WebJarsSupport._
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.server.Route
import service.NaiveBayesService
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directive.addDirectiveApply
import akka.http.scaladsl.server.Directives._
import play.twirl.api.Html
import views.html.form

import scala.concurrent.ExecutionContext

class RestAPI(bayesService: NaiveBayesService)(implicit ec: ExecutionContext) {
  implicit val twirlMarshaller: ToEntityMarshaller[Html] =
    Marshaller.withFixedContentType(ContentTypes.`text/html(UTF-8)`) { html =>
      HttpEntity(ContentTypes.`text/html(UTF-8)`, html.body)
    }

  private def classifyForm =
    formFields("text") { text =>
      onSuccess(bayesService.getTextClassWithHighlights(text)) { result =>
        complete(form(Some(text),
          Some(Html(s"[${result.classType.toString}] : ${result.highlightedText}")),
          None))
      }
    }

  def routes: Route = {
    pathPrefix("webjars") {
      webJars
    } ~
      pathSingleSlash {
        get {
          complete(Html("<h1>Главная</h1><a href=\"/classify\">Классификатор</a>"))
        }
      } ~
      pathPrefix("classify") {
        pathEndOrSingleSlash {
          concat(
            get {
              complete(form(None, None, None))
            },
            post {
              classifyForm
            })
        }
      }
  }
}
