package com.timeout.docless.schema.derive

import com.timeout.docless.schema._
import JsonSchema._
import enumeratum.EnumEntry
import io.circe._
import io.circe.syntax._
import cats.syntax.either._
import shapeless._
import shapeless.ops.coproduct

import reflect.runtime.{universe => ru}

trait CoprodInstances {
  implicit def cnilSchema: JsonSchema[CNil] =
    instance { sys.error("Unreachable code JsonSchema[CNil]") }

  implicit def coproductSchema[H, T <: Coproduct, L <: Nat](
      implicit lazyHSchema: Lazy[JsonSchema[H]],
      tSchema: JsonSchema[T],
      tLength: coproduct.Length.Aux[T, L],
      ev: H <:!< EnumEntry
  ): JsonSchema[H :+: T] = {
    val prop    = "allOf"
    val hSchema = lazyHSchema.value
    val hJson   = hSchema.asJsonRef
    instanceAndRelated {
      if (tLength() == Nat._0)
        JsonObject.singleton(prop, Json.arr(hJson)) -> hSchema.definitions
      else {
        val c    = tSchema.asJson.hcursor
        val arr  = hJson :: c.get[List[Json]](prop).valueOr(_ => Nil)
        val defs = tSchema.relatedDefinitions + hSchema.definition
        JsonObject.singleton(prop, Json.arr(arr: _*)) -> defs
      }
    }
  }

  implicit def genericCoprodSchema[A, R <: Coproduct](
      implicit gen: Generic.Aux[A, R],
      rSchema: JsonSchema[R],
      tag: ru.WeakTypeTag[A]
  ): JsonSchema[A] =
    instanceAndRelated[A] {
      rSchema.jsonObject.+:(
        "type" -> "object".asJson
      ) -> rSchema.relatedDefinitions
    }
}
