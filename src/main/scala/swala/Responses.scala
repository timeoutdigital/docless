package swala

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.StatusCode

object Responses {
  case class Response(description: String,
                      headers: List[HttpHeader],
                      schema: Option[Schema] = None,
                      example: Option[String])
}
import Responses._
case class Responses(default: Response, byStatusCode: Map[StatusCode, Response])
