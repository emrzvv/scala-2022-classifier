package telegram_bot.models

/**
 * case-class для разбора обновления в ответе от telegram api
 *
 * @param update_id идентификатор обновления
 * @param message   содержание обновления
 */
case class Update(update_id: Long, message: Option[Message])
