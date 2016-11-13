package swag.encoders

import java.time.{LocalDate, LocalDateTime}

import io.circe._
import io.circe.syntax._
import swag.JsonSchema
import swag.JsonSchema._
import enumeratum.{EnumEntry, Enum}

object Primitives {
  implicit val boolSchema: JsonSchema[Boolean] =
    instance[Boolean](Map("type" -> "boolean").asJson)

  implicit val intSchema: JsonSchema[Int] = instance[Int](Map(
      "type" -> "integer",
      "format" -> "int32"
   ).asJson)

  implicit val longSchema: JsonSchema[Long] = instance[Long](Map(
      "type" -> "integer",
      "format" -> "int64"
    ).asJson)

  implicit val floatSchema: JsonSchema[Float] = instance[Float](Map(
      "type" -> "number",
      "format" -> "float"
    ).asJson
  )

  implicit val doubleSchema: JsonSchema[Double] = instance[Double](Map(
      "type" -> "number",
      "format" -> "double"
    ).asJson)

  implicit val strSchema: JsonSchema[String] =
    instance[String](Map("type" -> "string").asJson)

  implicit val charSchema: JsonSchema[Char] =
    instance[Char](Map("type" -> "string").asJson)

  implicit val byteSchema: JsonSchema[Byte] = instance[Byte](Map(
      "type" -> "string",
      "format" -> "byte"
    ).asJson)

  implicit val symSchema: JsonSchema[Symbol] =
    instance[Symbol](Map("type" -> "string").asJson)

  implicit val dateSchema: JsonSchema[LocalDate] = instance[LocalDate](Map(
    "type" -> "string",
    "format" -> "date"
  ).asJson)

  implicit val dateTimeSchema: JsonSchema[LocalDateTime] = instance[LocalDateTime](Map(
    "type" -> "string",
    "format" -> "date-time"
  ).asJson)

  implicit def listSchema[A: JsonSchema]: JsonSchema[List[A]] = instance[List[A]](Json.obj(
      "type" -> Json.fromString("array"),
      "items" -> implicitly[JsonSchema[A]].asJson
   ).asJson)

  implicit def optSchema[A: JsonSchema]: JsonSchema[Option[A]] =
    instance[Option[A]](implicitly[JsonSchema[A]].asJson)

  implicit def enumSchema[A <: EnumEntry](implicit ev: Enum[A]): JsonSchema[Enum[A]] =
    instance[Enum[A]](Map(
      "enum" -> Json.arr(
        ev.values.map(e => Json.fromString(e.entryName)): _*
      )
    ).asJson)
}
