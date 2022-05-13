import actor.BayesActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentTypes, FormData, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import rest.RestAPI
import service.NaiveBayesService
import play.twirl.api.Html
import views.html.form

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class RestAPITest extends AnyWordSpec
  with Matchers
  with ScalatestRouteTest {

  implicit val twirlMarshaller: ToEntityMarshaller[Html] =
    Marshaller.withFixedContentType(ContentTypes.`text/html(UTF-8)`) { html =>
      HttpEntity(ContentTypes.`text/html(UTF-8)`, html.body)
    }

  implicit val timeout: RouteTestTimeout = RouteTestTimeout(10 seconds)

  val bayesActor: ActorRef = system.actorOf(Props[BayesActor])
  val bayesService = new NaiveBayesService(bayesActor)
  val restApi = new RestAPI(bayesService)
  val routes: Route = restApi.routes

  val positiveText: String = "ура ура привет хорошего дня"

  "AkkaServer" should {
    "has statuscode ok for GET request /" in {
      Get("/") ~> routes ~> check {
        status === StatusCodes.OK
      }
    }

    "has statuscode ok for GET request /webjars/bootstrap.min.css" in {
      Get("/webjars/bootstrap.min.css") ~> routes ~> check {
        status === StatusCodes.OK
      }
    }

    "has statuscode ok for GET request /classify" in {
      Get("/classify", form(None, None, None)) ~> routes ~> check {
        status === StatusCodes.OK
      }
    }

    "hast statuscode ok for POST request /classify and classify inputted text as positive" in {
      Post("/classify", FormData("text" -> positiveText)) ~> routes ~> check {
        status === StatusCodes.OK
        val result = bayesService.getTextClassWithHighlights(positiveText)
        Await.result(result, 10 seconds) === "позитивный"
      }
    }
  }
}
