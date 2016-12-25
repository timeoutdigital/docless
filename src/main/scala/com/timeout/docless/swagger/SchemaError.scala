package com.timeout.docless.swagger

import cats.Show
import com.timeout.docless.swagger.Path.{ParamRef, RefWithContext, ResponseRef}

sealed trait SchemaError

object SchemaError {
  case class MissingDefinition(context: RefWithContext) extends SchemaError
  def missingDefinition(ctx: RefWithContext): SchemaError = MissingDefinition(ctx)

  implicit val mdShow: Show[SchemaError] = Show.show {
    case MissingDefinition(ParamRef(r, path, param)) =>
      s"$path: cannot find definition ${r.id} for parameter name $param"
    case MissingDefinition(ResponseRef(r, path, method)) =>
      s"$path: cannot find response definition ${r.id} for method ${method.entryName}"
  }
}
