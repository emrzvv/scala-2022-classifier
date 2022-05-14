import server.AkkaServer
import telegram_bot.Bot

object Main extends App {
  if (args.length == 0) {
    println("oops, no bot token")
  } else {
    val bot = Bot(args(0))
  }
  AkkaServer()
}
