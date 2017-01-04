package com.timeout.docless.schema

import enumeratum.{EnumEntry, Enum}
import scala.reflect.runtime.{universe => ru}

trait EnumSchema[A <: EnumEntry] { this: Enum[A] =>
  implicit def schema(implicit tag: ru.WeakTypeTag[A]): JsonSchema[A] =
    JsonSchema.enum(values = this.values.map(_.entryName))
}
