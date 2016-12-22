package com.timeout.docless.schema

import reflect.runtime.{universe => ru}
import Function.const
import io.circe._
import io.circe.syntax._
import shapeless._
import shapeless.labelled.FieldType
import shapeless.ops.coproduct
import JsonSchema._
import cats.syntax.either._
import enumeratum.EnumEntry
import scala.annotation.implicitNotFound
import enumeratum.Enum

trait Auto {
  implicit val hNilSchema: JsonSchema[HNil] = inlineInstance(JsonObject.fromMap(Map.empty))

  implicit def hlistSchema[K <: Symbol, H, T <: HList](
    implicit
      witness: Witness.Aux[K],
      lazyHSchema: Lazy[JsonSchema[H]],
      tSchema: JsonSchema[T]
  ): JsonSchema[FieldType[K, H] :: T] = instance {
    val hSchema = lazyHSchema.value
    val hValue = if (hSchema.inline) hSchema.asJson else hSchema.asJsonRef
    val hField = witness.value.name -> hValue
    val tFields = tSchema.jsonObject.toList
    JsonObject.fromIterable(hField :: tFields)
  }

  implicit def cnilSchema: JsonSchema[CNil] =
    instance(sys.error("Unreachable code JsonSchema[CNil]"))

  implicit def coproductSchema[H, T <: Coproduct, L <: Nat](
    implicit
    hSchema: Lazy[JsonSchema[H]],
    tSchema: JsonSchema[T],
    tLength: coproduct.Length.Aux[T, L],
    ev: H <:!< EnumEntry
  ): JsonSchema[H :+: T] = {
    val prop = "allOf"
    val hJson = hSchema.value.asJsonRef
    val (tDefs, tJson) =
         if (tLength() == Nat._0)
           Nil -> Nil
         else {
           val c = tSchema.asJson.hcursor
           tSchema.coproductDefinitions -> c.get[List[Json]](prop)
             .valueOr(const(Nil))
         }

    instance(
      JsonObject.singleton(prop, Json.arr(hJson :: tJson: _*)),
      hSchema.value.definition :: tDefs
    )
  }

  implicit def genericSchema[A, R <: HList](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    rSchema: JsonSchema[R],
    fields: Required.Fields[R],
    tag: ru.WeakTypeTag[A]
  ): JsonSchema[A] = {
    instance[A](JsonObject.fromMap(Map(
      "type" -> Json.fromString("object"),
      "required" -> fields.asJson,
      "properties" -> rSchema.jsonObject.asJson
    )))
  }

  implicit def optSchema[A: JsonSchema]: JsonSchema[Option[A]] =
    inlineInstance[Option[A]](implicitly[JsonSchema[A]].jsonObject)

  @implicitNotFound(msg = "cannot derive coproduct")
  implicit def genericCoprodSchema[A, R <: Coproduct](
    implicit
    gen: Generic.Aux[A, R],
    rSchema: JsonSchema[R],
    tag: ru.WeakTypeTag[A]
  ): JsonSchema[A] =
    instance[A](
      rSchema.jsonObject.+:("type" -> "object".asJson),
      rSchema.coproductDefinitions)

  def deriveFor[A](implicit ev: JsonSchema[A]): JsonSchema[A] = ev
}

object Auto {
  trait EnumSchema[A <: EnumEntry] { this: Enum[A] =>
    implicit def jsonSchema(implicit tag: ru.WeakTypeTag[A]): JsonSchema[A] =
      JsonSchema.enum(values = this.values.map(_.entryName))
  }
}
