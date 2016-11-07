package swala

import akka.http.scaladsl.model.MediaType

import Operation._

object Operation {
  sealed trait Scheme
  case object Http extends Scheme
  case object Https extends Scheme
  case object Ws extends Scheme
  case object Wss extends Scheme
}
case class Operation(responses: Responses,
                     parameters: List[OperationParameter],
                     consumes: Set[MediaType] = Set.empty,
                     produces: Set[MediaType] = Set.empty,
                     schemes: Set[Scheme] = Set.empty,
                     security: List[SecurityRequirement],
                     deprecated: Boolean = false,
                     id: Option[String] = None,
                     summary: Option[String] = None,
                     description: Option[String] = None,
                     externalDoc: Option[ExternalDocs],
                     tags: List[String] = Nil)
