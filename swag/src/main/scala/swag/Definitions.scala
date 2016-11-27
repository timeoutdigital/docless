package swag

case class Definitions(get: JsonSchema.Definition*)
object Definitions {
  val empty = Definitions()
}
