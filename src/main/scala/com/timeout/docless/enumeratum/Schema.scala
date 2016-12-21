package com.timeout.docless.enumeratum
import com.timeout.docless.JsonSchema
import enumeratum.EnumEntry
import enumeratum.Enum
import io.circe.{Json, JsonObject}
import scala.reflect.runtime.{universe => ru}

trait Schema[A <: EnumEntry] { this: Enum[A] =>
  implicit def jsonSchema(implicit tag: ru.WeakTypeTag[A]): JsonSchema[A] = JsonSchema.inlineInstance {
    val vs = values.map(e => Json.fromString(e.entryName))
    JsonObject.singleton("enum", Json.arr(vs: _*))
  }
}
