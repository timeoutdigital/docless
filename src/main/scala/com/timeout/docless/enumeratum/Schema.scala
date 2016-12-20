package com.timeout.docless.enumeratum
import com.timeout.docless.JsonSchema
import enumeratum.EnumEntry
import enumeratum.Enum
import io.circe.{Json, JsonObject}

import scala.reflect.ClassTag

trait Schema[A <: EnumEntry] { this: Enum[A] =>
  implicit def jsonSchema(implicit tag: ClassTag[A]): JsonSchema[A] = JsonSchema.instance[A] {
    val vs = values.map(e => Json.fromString(e.entryName))
    JsonObject.singleton("enum", Json.arr(vs: _*))
  }
}
