package com.timeout.docless.swagger

import com.timeout.docless.JsonSchema

case class APISchema(info: Info,
                     host: String,
                     basePath: String,
                     swagger: String = "2.0",
                     paths: Paths = Paths(),
                     parameters: OperationParameters = OperationParameters(),
                     schemes: Set[Scheme] = Set.empty,
                     consumes: Set[String] = Set.empty,
                     produces: Set[String] = Set.empty,
                     definitions: Definitions = Definitions.empty) {

                       def withPaths(ps: Path*): APISchema =
                         copy(paths = Paths(ps: _*))

                       def defining(ds: JsonSchema.Definition*) =
                         copy(definitions = Definitions(ds: _*))
                     }
