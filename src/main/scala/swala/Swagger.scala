package swala

import javax.activation.MimeType

import akka.http.scaladsl.model.Uri

case class Swagger(info: Info,
                   host: Uri.Host,
                   basePath: Uri.Path,
                   schemes: Set[Schema],
                   consumes: Set[MimeType],
                   produces: Set[MimeType],
                   paths: List[Path],
                   definitions: List[Schema] = Nil,
                   parameters: List[OperationParameter] = Nil,
                   )
