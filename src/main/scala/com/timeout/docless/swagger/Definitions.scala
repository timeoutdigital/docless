package com.timeout.docless.swagger

import com.timeout.docless.schema.JsonSchema

case class Definitions(get: JsonSchema.Definition*)
object Definitions {
  val empty = Definitions()
}
