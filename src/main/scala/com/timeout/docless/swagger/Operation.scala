package com.timeout.docless.swagger

case class Operation(responses: Responses,
                     parameters: List[OperationParameter] = Nil,
                     consumes: Set[String] = Set.empty,
                     produces: Set[String] = Set.empty,
                     schemes: Set[Scheme] = Set.empty,
                     security: List[String] = Nil,
                     deprecated: Boolean = false,
                     operationId: Option[String] = None,
                     summary: Option[String] = None,
                     description: Option[String] = None,
                     externalDoc: Option[ExternalDocs] = None,
                     tags: List[String] = Nil) {

  def withParams(ps: OperationParameter*): Operation =
    copy(parameters = ps.toList)

  def responding(default: Responses.Response,
                 byStatus: Map[Int, Responses.Response] = Map.empty): Operation =
    copy(responses = Responses(default, byStatus))
}

