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

  private def webJarAssets: Route = pathPrefix("webjars") {
    webJars
  }

  def home: Route = {
    path("") {
      get {
        complete(Html("<h1>Главная</h1><a href=\"/classify\">Классификатор</a>"))
      }
    }
  }

  def classifyFormData: Route = {
    path("classify_type") {
      post {
        formFields("text") { text =>
          onSuccess(bayesService.getTextClass(text)) { result =>
            complete(form(Some(text), Some(Html(result)), None))
          }
        }
      }
    }
  }

  def classifyFormDataWithHighlights: Route = {
    pathPrefix("classify") {
      pathEndOrSingleSlash {
        concat(
          get {
            complete(form(None, None, None))
          },
          post {
            formFields("text") { text =>
              onSuccess(bayesService.getTextClassWithHighlights(text)) { (classType, highlightedText) =>
                complete(form(Some(text), Some(Html(s"[$classType] : $highlightedText")), None))
              }
            }
          })
      }
    }
  }

  private def classifierRoutes: Route = {
    classifyFormData ~ classifyFormDataWithHighlights
  }

  def routes: Route = {
    webJarAssets ~ home ~ classifierRoutes
  }
}
