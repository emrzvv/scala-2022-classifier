import actor.BayesActor
import akka.actor.{ActorRef, ActorSystem, Props}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.specs2.Specification
import org.specs2.matcher.Matchers
import org.specs2.specification.core.SpecStructure
import rest.RestAPI
import service.NaiveBayesService

import scala.concurrent.ExecutionContext.Implicits.global

class RestAPITest extends Specification
  with Matchers
  with FailFastCirceSupport {

  implicit val system: ActorSystem = ActorSystem("my-system")
  val bayesActor: ActorRef = system.actorOf(Props[BayesActor])
  val bayesService = new NaiveBayesService(bayesActor)
  val restApi = new RestAPI(bayesService)


  override def is: SpecStructure = """"""
}
