package swag

import swag.JsonSchema.Definition

import scala.reflect.macros.blackbox
import scala.reflect.runtime.{universe => ru}
import scala.language.experimental.macros
import io.circe.Json

trait JsonSchema[A] {
  def id: String
  def asJson: Json
  def definition: JsonSchema.Definition =
    Definition(id, asJson.mapObject(_.remove("id")))
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

  def instance[A](obj: Json)(implicit tag: ru.WeakTypeTag[A]): JsonSchema[A] = new JsonSchema[A] {
    override def id = tag.tpe.typeSymbol.fullName
    override def asJson = obj
  }

  def genSchema[T]: JsonSchema[T] = macro genSchemaImpl[T]

  def genSchemaImpl[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[JsonSchema[T]] = {
    import c.universe._

    def abort(msg: String) = c.abort(c.enclosingPosition, _:String)

    val tpe = c.weakTypeOf[T]
    val builder = new SchemaBuilder[c.universe.type](c.universe)
    val id = tpe.typeSymbol.fullName
    val klass = tpe.typeSymbol.asClass
    lazy val isADT = klass.isSealed && (klass.isAbstract || klass.isTrait)

    val json =
      if (klass.isCaseClass) {
        builder.properties(tpe)
      } else if (isADT) {
        builder.subclassProperties(klass)
          .getOrElse(sys.error(s"Cannod find subclasses for ${klass.name}"))
      } else {
        sys.error("only case classes and sealed traits are supported!")
      }

    c.Expr[JsonSchema[T]] {
      q"""
         new JsonSchema[$tpe] {
           override def id: String = $id
           override def asJson: Json =
             $json.deepMerge(Json.obj("id" -> id.asJson))
         }
       """
    }
  }
}
