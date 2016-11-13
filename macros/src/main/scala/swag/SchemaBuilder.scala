package swag

import io.circe.Json
import scala.reflect.api.Universe

class SchemaBuilder[U <: Universe](val u: U) {
  import u._

  def subclassProperties(k: ClassSymbol): Option[Tree] = {
    if (k.knownDirectSubclasses.isEmpty) None
    else {
      val props = k.knownDirectSubclasses.map(sk => properties(sk.typeSignature))

      val ast = q"""
        Json.obj(
          "type" -> Json.fromString("object"),
          "allOf" -> Json.fromValues($props)
        )
      """
      Some(ast)
    }
  }

  def properties(t: Type): Tree = {
    val params = t.members.filter {
      m => m.asTerm.isGetter && m.asTerm.isCaseAccessor && m.asTerm.isPublic
    }.toList.reverse

    val required = params.filterNot(p => isOptional(p.typeSignature)).map { f =>
      s"${f.name.toString}"
    }

    val props = params.map { p =>
      val fieldType = p.typeSignature.finalResultType
      q"${p.asTerm.name.toString} -> implicitly[JsonSchema[$fieldType]].asJson"
    }

    q"""
      Json.obj(
        "required" -> $required.asJson,
        "properties" -> Json.fromFields($props)
      )
    """
  }

  private def isOptional(t: Type): Boolean = t match {
    case NullaryMethodType(TypeRef(ThisType(prefix), sym, _)) =>
      prefix.name.toString == "scala" && sym.name.toString == "Option"
    case x => false
  }
}
