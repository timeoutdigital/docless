package swala

case class APISchema(info: Info,
                     host: String,
                     basePath: String,
                     swagger: String = "2.0",
                     paths: List[Path],
                     parameters: List[OperationParameter] = Nil,
                     schemes: Set[Scheme] = Set.empty,
                     consumes: Set[String] = Set.empty,
                     produces: Set[String] = Set.empty,
                     definitions: List[SchemaDefinition] = Nil)
