package com.timeout.docless.swagger

import cats.Show
import RefWithContext._

sealed trait SchemaError

object SchemaError {
  case class MissingDefinition(context: RefWithContext) extends SchemaError
  def missingDefinition(ctx: RefWithContext): SchemaError =
    MissingDefinition(ctx)

  implicit val mdShow: Show[SchemaError] = Show.show {
    case MissingDefinition(RefWithContext(r, DefinitionContext(d))) =>
      s"${d.id}: cannot find a field definition for: '${r.id}'"
    case MissingDefinition(RefWithContext(r, ParamContext(param, path))) =>
      s"$path: cannot find definition '${r.id}' for parameter name '$param'"
    case MissingDefinition(RefWithContext(r, ResponseContext(method, path))) =>
      s"$path: cannot find response definition '${r.id}' for method '${method.entryName}'"
  }
}
