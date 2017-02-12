package com.timeout.docless.schema

import java.time.{LocalDate, LocalDateTime}

import com.timeout.docless.schema.JsonSchema._
import io.circe._
import io.circe.syntax._
import scala.reflect.runtime.{universe => ru}

trait Primitives {
  implicit val boolSchema: JsonSchema[Boolean] =
    inlineInstance[Boolean](Map("type" -> "boolean").asJsonObject)

  implicit val intSchema: JsonSchema[Int] = inlineInstance[Int](
    Map(
      "type"   -> "integer",
      "format" -> "int32"
    ).asJsonObject
  )

  implicit val longSchema: JsonSchema[Long] = inlineInstance[Long](
    Map(
      "type"   -> "integer",
      "format" -> "int64"
    ).asJsonObject
  )

  implicit val floatSchema: JsonSchema[Float] = inlineInstance[Float](
    Map(
      "type"   -> "number",
      "format" -> "float"
    ).asJsonObject
  )

  implicit val doubleSchema: JsonSchema[Double] = inlineInstance[Double](
    Map(
      "type"   -> "number",
      "format" -> "double"
    ).asJsonObject
  )

  implicit val strSchema: JsonSchema[String] =
    inlineInstance[String](Map("type" -> "string").asJsonObject)

  implicit val charSchema: JsonSchema[Char] =
    inlineInstance[Char](Map("type" -> "string").asJsonObject)

  implicit val byteSchema: JsonSchema[Byte] = inlineInstance[Byte](
    Map(
      "type"   -> "string",
      "format" -> "byte"
    ).asJsonObject
  )

  implicit val symSchema: JsonSchema[Symbol] =
    inlineInstance[Symbol](Map("type" -> "string").asJsonObject)

  implicit val dateSchema: JsonSchema[LocalDate] = inlineInstance[LocalDate](
    Map(
      "type"   -> "string",
      "format" -> "date"
    ).asJsonObject
  )

  implicit val dateTimeSchema: JsonSchema[LocalDateTime] =
    inlineInstance[LocalDateTime](
      Map(
        "type"   -> "string",
        "format" -> "date-time"
      ).asJsonObject
    )

  implicit def listSchema[A: JsonSchema]: JsonSchema[List[A]] = {
    val schema = implicitly[JsonSchema[A]]
    inlineInstance[List[A]](
      JsonObject.fromMap(
        Map(
          "type"  -> Json.fromString("array"),
          "items" -> (if (schema.inline) schema.asJson else schema.asJsonRef)
        )
      )
    )
  }

  implicit def optSchema[A](implicit ev: JsonSchema[A], tag: ru.WeakTypeTag[A]): JsonSchema[Option[A]] =
    if (ev.inline) inlineInstance[Option[A]](ev.jsonObject)
    else functorInstance[Option, A](ev.jsonObject)(tag)


  implicit def mapSchema[K, V](implicit kPattern: PatternProperty[K],
                               vSchema: JsonSchema[V]): JsonSchema[Map[K, V]] =
    inlineInstance {
      JsonObject.fromMap(
        Map(
          "patternProperties" -> JsonObject
            .singleton(
              kPattern.regex.toString,
              vSchema.asJson
            )
            .asJson
        )
      )
    }

}
