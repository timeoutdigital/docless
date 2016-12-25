package com.timeout.docless.swagger

case class Operation(responses: Responses = Responses.default,
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
                     tags: List[String] = Nil)
    extends ParamSetters[Operation] {

  override def withParams(ps: OperationParameter*): Operation =
    copy(parameters = ps.toList)

  def withDescription(desc: String) = copy(description = Some(desc))

  def responding(default: Responses.Response)(
      rs: (Int, Responses.Response)*): Operation =
    copy(responses = Responses(default, rs.toMap))
}

object Operation {
  def apply(id: Symbol, _summary: String): Operation =
    Operation(operationId = Some(id.name.toString()), summary = Some(_summary))
}
