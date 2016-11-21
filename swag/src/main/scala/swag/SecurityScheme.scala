package swag

import enumeratum._

sealed trait SecurityScheme {
  def description: Option[String]
  def name: String
}

case class Basic(name: String,
                 description: Option[String] = None) extends SecurityScheme

case class ApiKey(name: String,
                  in: SecurityScheme.ApiKey.In,
                  description: Option[String] = None) extends SecurityScheme

case class OAuth2(name: String,
                  flow: SecurityScheme.OAuth2.Flow,
                  autorizationUrl: Option[String] = None,
                  tokenUrl: Option[String] = None,
                  scopes: Map[String, String] = Map.empty,
                  description: Option[String] = None) extends SecurityScheme

object SecurityScheme {

  object ApiKey {
    sealed trait In extends EnumEntry with EnumEntry.Lowercase

    object In extends Enum[In] with CirceEnum[In] {
      case object Query extends In
      case object Header extends In
      override def values = findValues
    }

  }

  object OAuth2 {
    sealed trait Flow extends EnumEntry with EnumEntry.Lowercase

    object Flow extends Enum[Flow] with CirceEnum[Flow] {
      case object Implicit extends Flow
      case object Password extends Flow
      case object Application extends Flow
      case object AccessCode extends Flow

      override def values = findValues
    }
  }
}
