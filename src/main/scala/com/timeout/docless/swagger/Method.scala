package com.timeout.docless.swagger

import enumeratum._

sealed trait Method extends EnumEntry with EnumEntry.Lowercase

object Method extends Enum[Method] {
  case object Get     extends Method
  case object Delete  extends Method
  case object Post    extends Method
  case object Put     extends Method
  case object Patch   extends Method
  case object Head    extends Method
  case object Options extends Method
  override def values = findValues
}
