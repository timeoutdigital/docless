package swag

case class Operation(responses: Responses,
                     parameters: List[OperationParameter] = Nil,
                     consumes: Set[String] = Set.empty,
                     produces: Set[String] = Set.empty,
                     schemes: Set[Scheme] = Set.empty,
                     security: List[SecurityRequirement] = Nil,
                     deprecated: Boolean = false,
                     id: Option[String] = None,
                     summary: Option[String] = None,
                     description: Option[String] = None,
                     externalDoc: Option[ExternalDocs] = None,
                     tags: List[String] = Nil)
