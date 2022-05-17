package telegram_bot.models


/**
 * case-class для разбора json ответа от telegram api
 *
 * @param ok     статус ответа от telegram api
 * @param result обновления
 */
case class TelegramResponse(ok: Boolean, result: Array[Update])
