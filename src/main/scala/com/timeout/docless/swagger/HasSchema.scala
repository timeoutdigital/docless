package com.timeout.docless.swagger

import com.timeout.docless.schema.JsonSchema

trait HasSchema {
  def schema: Option[JsonSchema.Ref]
}
