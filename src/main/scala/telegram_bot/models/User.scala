package telegram_bot.models

/**
 * информация о пользователе
 *
 * @param id         идентификатор
 * @param first_name первое имя (задаётся у всех пользователей)
 * @param last_name  второе имя (есть не у всех пользователей)
 * @param username   юзернейм (есть не у всех пользователей)
 */
case class User(id: Long, first_name: String, last_name: Option[String], username: Option[String])
