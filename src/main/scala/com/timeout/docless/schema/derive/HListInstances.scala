package com.timeout.docless.schema.derive

import com.timeout.docless.schema._
import JsonSchema._
import shapeless._
import io.circe._
import io.circe.syntax._
import shapeless.labelled.FieldType
import reflect.runtime.{universe => ru}

trait HListInstances {
  implicit val hNilSchema: JsonSchema[HNil] = inlineInstance(
    JsonObject.fromMap(Map.empty)
  )

  implicit def hlistSchema[K <: Symbol, H, T <: HList](
      implicit witness: Witness.Aux[K],
      lazyHSchema: Lazy[JsonSchema[H]],
      lazyTSchema: Lazy[JsonSchema[T]]
  ): JsonSchema[FieldType[K, H] :: T] = instanceAndRelated {
    val hSchema = lazyHSchema.value
    val tSchema = lazyTSchema.value
    val (hValue, related) =
      if (hSchema.inline)
        hSchema.asJson -> tSchema.relatedDefinitions
      else
        hSchema.asJsonRef -> (tSchema.relatedDefinitions + hSchema.definition)

    val hField  = witness.value.name -> hValue
    val tFields = tSchema.jsonObject.toList

    JsonObject.fromIterable(hField :: tFields) -> related
  }

  implicit def genericSchema[A, R <: HList](
      implicit gen: LabelledGeneric.Aux[A, R],
      rSchema: JsonSchema[R],
      fields: Required.Fields[R],
      tag: ru.WeakTypeTag[A]
  ): JsonSchema[A] =
    instanceAndRelated[A] {
      JsonObject.fromMap(
        Map(
          "type"       -> Json.fromString("object"),
          "required"   -> fields.asJson,
          "properties" -> rSchema.jsonObject.asJson
        )
      ) -> rSchema.relatedDefinitions
    }

  def deriveFor[A](implicit ev: JsonSchema[A]): JsonSchema[A] = ev
}
