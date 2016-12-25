package com.timeout.docless.swagger

import com.timeout.docless.schema.JsonSchema

object Responses {
  type HeaderName = String
  case class Header(`type`: Type,
                    format: Option[Format] = None,
                    description: Option[String] = None)

  case class Response(description: String,
                      headers: Map[HeaderName, Header] = Map.empty,
                      schema: Option[JsonSchema.Ref] = None,
                      example: Option[String] = None) extends HasSchema {

    def withHeaders(hs: (HeaderName, Header)*) = copy(headers = hs.toMap)
  }

  val default = Responses(
    default = Response(description = "An internal server error")
  )
}

case class Responses(default: Responses.Response,
                     byStatusCode: Map[Int, Responses.Response] = Map.empty) {
  def withStatusCodes(rs: (Int, Responses.Response)*): Responses =
    copy(byStatusCode = rs.toMap)
}
