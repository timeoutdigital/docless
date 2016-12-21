package com.timeout.docless

import io.circe._
import io.circe.syntax._

import scala.reflect.runtime.{universe => ru}
import shapeless._
import shapeless.labelled._
import java.time.{LocalDate, LocalDateTime}
import cats.syntax.either._
import shapeless.ops.coproduct
import Function.const

trait JsonSchema[A] {
  def id: String
  def coproductDefinitions: List[JsonSchema.Definition]
  def jsonObject: JsonObject
  def asJson: Json = jsonObject.asJson

  def ref: JsonObject =
    JsonObject.singleton("$ref", Json.fromString(id))

  def definition: JsonSchema.Definition =
    JsonSchema.Definition(id, asJson)

  def definitions: List[JsonSchema.Definition] =
    coproductDefinitions :+ definition
}

object JsonSchema {
  case class Definition(id: String, json: Json) {
    def asRef: Ref = TypeRef(id)
  }

  sealed trait Ref {
    def id: String
  }

  case class TypeRef(id: String) extends Ref
  object TypeRef {
    def apply(definition: Definition): TypeRef = TypeRef(definition.id)
    def apply(schema: JsonSchema[_]): TypeRef = TypeRef(schema.id)
  }

  case class ArrayRef(id: String) extends Ref
  object ArrayRef {
    def apply(definition: Definition): ArrayRef = ArrayRef(definition.id)
    def apply(schema: JsonSchema[_]): ArrayRef = ArrayRef(schema.id)
  }

  def instance[A](obj: => JsonObject, defs: List[Definition] = Nil)(implicit tag: ru.WeakTypeTag[A]): JsonSchema[A] = new JsonSchema[A] {
    override def id = tag.tpe.typeSymbol.fullName
    override def jsonObject = obj
    override def coproductDefinitions = defs
  }


  trait Required[A] {
    def isRequired: Boolean = true
  }

  object Required {
    implicit def optIsRequired[A]: Required[Option[A]] = new Required[Option[A]] {
      override def isRequired = false
    }

    implicit def otherRequired[A]: Required[A] = new Required[A] {
      override def isRequired = true
    }

    def isRequired[A: Required] = implicitly[Required[A]].isRequired

    trait Fields[A] {
      def get: List[String]
      def asJson: Json = Json.arr(get.map(Json.fromString): _*)
    }

    object Fields {
      def instance[A](fs: List[String]): Fields[A] = new Fields[A] {
        override def get: List[String] = fs
      }

      implicit val hnilFields: Fields[HNil] = instance(Nil)

      implicit def hlistFields[K <: Symbol, H, T <: HList](implicit
        witness: Witness.Aux[K],
        req: Lazy[Required[H]],
        tFields: Fields[T])
      : Fields[FieldType[K, H] :: T] = instance {
        if (req.value.isRequired)
          witness.value.name :: tFields.get
        else
          tFields.get
      }

      implicit def genericFields[A, R](implicit
        gen: LabelledGeneric.Aux[A, R],
        rfs: Fields[R])
      : Fields[A] = instance(rfs.get)

      def apply[L](implicit ev: Fields[L]): List[String] = ev.get
    }
  }

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

  implicit def cnilSchema: JsonSchema[CNil] =
    instance(sys.error("Unreachable code JsonSchema[CNil]"))

  implicit def coproductSchema[H, T <: Coproduct, L <: Nat](
    implicit
    hSchema: Lazy[JsonSchema[H]],
    tSchema: JsonSchema[T],
    tLength: coproduct.Length.Aux[T, L]
  ): JsonSchema[H :+: T] = {
    val prop = "allOf"
    val hDef = Definition(hSchema.value.id, hSchema.value.asJson)
    val hJson = hSchema.value.ref.asJson

    val (tDefs, tJson) =
         if (tLength() == Nat._0)
           Nil -> Nil
         else
           tSchema.coproductDefinitions -> tSchema.asJson.hcursor.get[List[Json]](prop).valueOr(const(Nil))

    instance(
      JsonObject.singleton(prop, Json.arr(hJson :: tJson: _*))
    , hDef :: tDefs)
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

  implicit def genericCoprodSchema[A, R <: Coproduct](
    implicit
    gen: Generic.Aux[A, R],
    rSchema: JsonSchema[R],
    tag: ru.WeakTypeTag[A]
  ): JsonSchema[A] =
    instance[A](
      rSchema.jsonObject.+:("type" -> "object".asJson),
      rSchema.coproductDefinitions)

  def deriveFor[A: JsonSchema] = implicitly[JsonSchema[A]]
}
