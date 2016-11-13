package swala

import enumeratum._

sealed trait Scheme extends EnumEntry with EnumEntry.Lowercase

object Scheme extends Enum[Scheme] {
  case object Http extends Scheme
  case object Https extends Scheme
  case object Ws extends Scheme
  case object Wss extends Scheme

  override def values = findValues
}
