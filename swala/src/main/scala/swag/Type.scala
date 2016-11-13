package swag

import enumeratum._

sealed trait Type extends EnumEntry with EnumEntry.Lowercase

object Type extends Enum[Type] {
  case object String extends Type
  case object Number extends Type
  case object Integer extends Type
  case object Boolean extends Type
  case object File extends Type

  override def values = findValues
}
