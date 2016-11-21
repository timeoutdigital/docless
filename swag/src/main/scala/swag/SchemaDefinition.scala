package swag

import io.circe.Json

case class SchemaDefinition(id: String, json: Json) {
  def ref = id
}
