package com.timeout.docless.encoders

import java.time.{LocalDate, LocalDateTime}

import com.timeout.docless.JsonSchema
import shapeless.labelled._
import shapeless._
import io.circe._
import io.circe.syntax._
import cats.Monoid
import cats.syntax.monoid._

import scala.reflect.runtime.{universe => ru}
import shapeless.ops.record.{Keys, Values}

object Primitives {
  import JsonSchema.instance
  implicit val boolSchema: JsonSchema[Boolean] =
    instance[Boolean](Map("type" -> "boolean").asJsonObject)

  implicit val intSchema: JsonSchema[Int] = instance[Int](Map(
      "type" -> "integer",
      "format" -> "int32"
   ).asJsonObject)

  implicit val longSchema: JsonSchema[Long] = instance[Long](Map(
      "type" -> "integer",
      "format" -> "int64"
    ).asJsonObject)

  implicit val floatSchema: JsonSchema[Float] = instance[Float](Map(
      "type" -> "number",
      "format" -> "float"
    ).asJsonObject
  )

  implicit val doubleSchema: JsonSchema[Double] = instance[Double](Map(
      "type" -> "number",
      "format" -> "double"
    ).asJsonObject)

  implicit val strSchema: JsonSchema[String] =
    instance[String](Map("type" -> "string").asJsonObject)

  implicit val charSchema: JsonSchema[Char] =
    instance[Char](Map("type" -> "string").asJsonObject)

  implicit val byteSchema: JsonSchema[Byte] = instance[Byte](Map(
      "type" -> "string",
      "format" -> "byte"
    ).asJsonObject)

  implicit val symSchema: JsonSchema[Symbol] =
    instance[Symbol](Map("type" -> "string").asJsonObject)

  implicit val dateSchema: JsonSchema[LocalDate] = instance[LocalDate](Map(
    "type" -> "string",
    "format" -> "date"
  ).asJsonObject)

  implicit val dateTimeSchema: JsonSchema[LocalDateTime] = instance[LocalDateTime](Map(
    "type" -> "string",
    "format" -> "date-time"
  ).asJsonObject)

  implicit def listSchema[A: JsonSchema]: JsonSchema[List[A]] = instance[List[A]](JsonObject.fromMap(Map(
     "type" -> Json.fromString("array"),
     "items" -> implicitly[JsonSchema[A]].asJson
   )))

  implicit def optSchema[A: JsonSchema]: JsonSchema[Option[A]] =
    instance[Option[A]](implicitly[JsonSchema[A]].jsonObject)

  implicit val hNilSchema: JsonSchema[HNil] = instance(JsonObject.fromMap(Map.empty))

  implicit def hlistSchema[K <: Symbol, H, T <: HList](
    implicit
      witness: Witness.Aux[K],
      hSchema: Lazy[JsonSchema[H]],
      tSchema: JsonSchema[T]
  ): JsonSchema[FieldType[K, H] :: T] = instance {
    val hField = witness.value.name -> hSchema.value.asJson
    val tFields = tSchema.jsonObject.toList
    JsonObject.fromIterable(hField :: tFields)
  }

  implicit val cnilSchema: JsonSchema[CNil] =
    instance(sys.error("Unreachable code JsonSchema[CNil]"))

  implicit def coproductSchema[H <: enumeratum.EnumEntry, T <: Coproduct](
    implicit
      enum: Lazy[enumeratum.Enum[H]],
      tSchema: JsonSchema[T]
  ): JsonSchema[H :+: T] = {
    val enumValues = Json.arr(enum.value.values.map(h => Json.fromString(h.entryName)): _*)
    instance(JsonObject.fromMap(Map("enum" -> enumValues)))
  }

  implicit def genericSchema[A, R <: HList](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    rSchema: JsonSchema[R],
    required: JsonSchema.Required[R],
    tag: ru.WeakTypeTag[A]
  ): JsonSchema[A] = instance(JsonObject.fromMap(Map(
      "type" -> Json.fromString("object"),
      "required" -> Json.arr(required.properties.map(Json.fromString): _*),
      "properties" -> rSchema.jsonObject.asJson
    )))

  def genSchema[A: JsonSchema] = implicitly[JsonSchema[A]]
}
