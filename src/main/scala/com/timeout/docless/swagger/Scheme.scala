package com.timeout.docless.swagger

import enumeratum._

sealed trait Scheme extends EnumEntry with EnumEntry.Lowercase

object Scheme extends CirceEnum[Scheme] with Enum[Scheme] {
  case object Http  extends Scheme
  case object Https extends Scheme
  case object Ws    extends Scheme
  case object Wss   extends Scheme

  override def values = findValues
}
