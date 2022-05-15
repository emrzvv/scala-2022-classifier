package telegram_bot.models

sealed trait Message {
  def message_id: Long
  def from: User
  def date: Long
  def chat: Chat
  def reply_to_message: Option[Message]
}

case class TextMessage(message_id: Long,
                       from: User,
                       date: Long,
                       chat: Chat,
                       text: String)
