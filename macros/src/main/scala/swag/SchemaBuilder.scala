package swag

import enumeratum.EnumEntry

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
      val fieldName = p.asTerm.name.toString
      val fieldVal = if (fieldType <:< typeOf[EnumEntry])
        enumEntry(fieldType, t)
      else
        q"implicitly[JsonSchema[$fieldType]].asJson"

      q"$fieldName -> $fieldVal"
    }

    q"""
      Json.obj(
        "type" -> Json.fromString("object"),
        "required" -> $required.asJson,
        "properties" -> Json.fromFields($props)
      )
    """
  }

  private def enumEntry(t: Type, context: Type): Tree = {
    val companion = t match {
      case TypeRef(_, sym, _) => TermName(sym.name.toString)
      case other => sys.error(s"Unable to handle $context member $t. Tree: ${showRaw(other)}")
    }
    q"""
      Map(
        "enum" -> Json.arr(
          $companion.values.map(e => Json.fromString(e.entryName)): _*
        )
       ).asJson
    """
  }

  private def isOptional(t: Type): Boolean = t match {
    case NullaryMethodType(TypeRef(ThisType(prefix), sym, _)) =>
      prefix.name.toString == "scala" && sym.name.toString == "Option"
    case _ => false
  }
}
