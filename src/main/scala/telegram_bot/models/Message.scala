package telegram_bot.models

/**
 * case-class для разбора сообщения обновления
 *
 * @param message_id идентификатор сообщений
 * @param from информация о том, кем отправлено сообщение
 * @param date дата
 * @param chat информация о чате
 * @param text текст сообщения.
 */
case class Message(message_id: Option[Long],
                       from: Option[User],
                       date: Option[Long],
                       chat: Option[Chat],
                       text: Option[String])
