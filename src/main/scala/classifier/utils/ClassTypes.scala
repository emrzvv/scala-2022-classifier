package classifier.utils

object ClassTypes {
  sealed trait ClassType {
    val csvValue: String
  }

  case object Negative extends ClassType {
    override val csvValue: String = "-1"

    override def toString: String = "негативный"
  }

  case object Positive extends ClassType {
    override val csvValue: String = "1"

    override def toString: String = "позитивный"
  }

  case object Neutral extends ClassType {
    override val csvValue: String = "0"

    override def toString: String = "нейтральный"
  }
}
