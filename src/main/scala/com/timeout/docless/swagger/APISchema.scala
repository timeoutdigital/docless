package com.timeout.docless.swagger

import com.timeout.docless.schema.JsonSchema

case class APISchema(
    info: Info,
    host: String,
    basePath: String,
    swagger: String = "2.0",
    paths: Paths = Paths(Nil),
    parameters: OperationParameters = OperationParameters(Nil),
    schemes: Set[Scheme] = Set.empty,
    consumes: Set[String] = Set.empty,
    produces: Set[String] = Set.empty,
    definitions: Definitions = Definitions.empty
) extends ParamSetters[APISchema] {

  def withPaths(ps: Path*): APISchema =
    copy(paths = Paths(ps))

  def defining(ds: JsonSchema.Definition*) =
    copy(definitions = Definitions(ds: _*))

  override def withParams(param: OperationParameter*): APISchema =
    copy(parameters = OperationParameters(param))
}
