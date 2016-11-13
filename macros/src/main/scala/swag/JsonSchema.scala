package swag

import scala.reflect.macros.blackbox
import scala.language.experimental.macros
import io.circe.Json

trait JsonSchema[A] {
  def id: Option[String] = None
  def asJson: Json
}

object JsonSchema {
  def instance[A](obj: Json): JsonSchema[A] = new JsonSchema[A] {
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
           override def id: Option[String] = Some($id)
           override def asJson: Json =
             $json.deepMerge(Json.obj("id" -> id.asJson))
         }
       """
    }
  }
}
