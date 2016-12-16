package com.timeout.docless

import cats.Functor
import io.circe._
import io.circe.syntax._

import scala.reflect.runtime.{universe => ru}
import shapeless._
import shapeless.labelled._
import shapeless.ops.record.{Keys, Values}

trait JsonSchema[A] {
  def id: String
  def asJson: Json = jsonObject.asJson

  def jsonObject: JsonObject

  def mapObject(f: JsonObject => JsonObject): JsonSchema[A] = {
    val self = this
    new JsonSchema[A] {
      override def id: String = self.id
      override def jsonObject: JsonObject = f(self.jsonObject)
    }
  }

  def definition: JsonSchema.Definition =
    JsonSchema.Definition(id, asJson)
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

  def instance[A](obj: => JsonObject, idS: Option[String] = None)(implicit tag: ru.WeakTypeTag[A]): JsonSchema[A] = new JsonSchema[A] {
    override def id = idS.getOrElse(tag.tpe.typeSymbol.fullName)
    override def jsonObject = obj
  }


  trait Required[A] {
    def isRequired: Boolean = true
    def properties: List[String]
    def combine(that: Required[_]): Required[A] =
      Required((properties ++ that.properties).distinct)
  }

  object Required {
    def apply[A](ks: List[String]) = new Required[A] {
      override def properties = ks
    }

    implicit def optionRequired[A]: Required[Option[A]] = new Required[Option[A]] {
      override def isRequired = false
      override def properties: List[String] = Nil
    }

    implicit val hNilRequired: Required[HNil] = Required(Nil)

    implicit def hlistIsRequired[K <: Symbol, H, T <: HList](
      implicit
      witness: Witness.Aux[K],
      tag: ru.WeakTypeTag[H],
      tRequired: Lazy[Required[T]])
    : Required[FieldType[K, H] :: T] = {
      val props =
        if (ru.weakTypeOf[H] <:< ru.weakTypeOf[Option[_]])
          Nil
        else
          List(witness.value.name)

      Required(props).combine(tRequired.value)
    }
  }

}
