package telegram_bot.models

case class User(id: Long, first_name: String, last_name: Option[String], username: Option[String]) extends HasId
