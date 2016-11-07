package swala

object Format {
  case object Int32 extends Format
  case object Int64 extends Format
  case object Float extends Format
  case object Double extends Format
  case object Byte extends Format
  case object Binary extends Format
  case object Date extends Format
  case object DateTime extends Format
  case object Password extends Format
}

sealed trait Format
