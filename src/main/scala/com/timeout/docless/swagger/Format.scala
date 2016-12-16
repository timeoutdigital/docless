package com.timeout.docless.swagger

import enumeratum._

sealed trait Format extends EnumEntry with EnumEntry.Lowercase

object Format extends CirceEnum[Format] with Enum[Format] {
  case object Int32 extends Format
  case object Int64 extends Format
  case object Float extends Format
  case object Double extends Format
  case object Byte extends Format
  case object Binary extends Format
  case object Boolean extends Format
  case object Date extends Format
  case object DateTime extends Format {
    override def entryName = "date-time"
  }
  case object Password extends Format

  override def values = findValues
}