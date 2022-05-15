package telegram_bot.models


case class Message(message_id: Long,
                       from: User,
                       date: Long,
                       chat: Chat,
                       text: Option[String])
