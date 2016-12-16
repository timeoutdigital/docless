package com.timeout.docless.swagger

import com.timeout.docless.JsonSchema

object Responses {
  type HeaderName = String
  case class Header(`type`: Type,
                    format: Option[Format] = None,
                    description: Option[String] = None)

  case class Response(description: String,
                      headers: Map[HeaderName, Header] = Map.empty,
                      schema: Option[JsonSchema.Ref] = None,
                      example: Option[String] = None)
}

case class Responses(default: Responses.Response,
                     byStatusCode: Map[Int, Responses.Response] = Map.empty)
