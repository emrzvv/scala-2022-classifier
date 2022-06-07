package logger

import com.typesafe.scalalogging.Logger

object ServerLogger {
  val serverLogger: Logger = Logger("server-classifier-logger")
  val botLogger: Logger = Logger("telegram-bot-logger")
}
