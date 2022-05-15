import actor.BayesActor
import akka.actor.{ActorRef, ActorSystem, Props}
import server.AkkaServer
import telegram_bot.Bot

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("main-system")
  val bayesActor: ActorRef = system.actorOf(Props[BayesActor], "bayes-actor")

  if (args.length == 0) {
    println("oops, no bot token")
  } else {

    val bot = Bot(args(0), bayesActor)

  }

  AkkaServer(bayesActor)
}
