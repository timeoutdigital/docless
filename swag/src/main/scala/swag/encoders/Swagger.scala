package swag.encoders

import io.circe.Encoder
import swag._

import io.circe.generic.semiauto._
import encoders.Primitives._

object Swagger {
  implicit val externalDocsEncoder: Encoder[ExternalDocs] = deriveEncoder[ExternalDocs]
  implicit val contactEncoder: Encoder[Info.Contact] = deriveEncoder[Info.Contact]
  implicit val licenseEncoder: Encoder[Info.License] = deriveEncoder[Info.License]
  implicit val infoEncoder: Encoder[Info] = deriveEncoder[Info]
}
