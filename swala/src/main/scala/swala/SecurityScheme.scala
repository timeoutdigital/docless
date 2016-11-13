package swala

import akka.http.scaladsl.model.Uri

object SecurityScheme {
  object ApiKey {
    sealed trait In
    case object Query extends In
    case object Header extends In
  }

  object OAuth2 {
    sealed trait Flow
    case object Implicit extends Flow
    case object Password extends Flow
    case object Application extends Flow
    case object AccessCode extends Flow
  }
}

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
                  autorizationUrl: Option[Uri] = None,
                  tokenUrl: Option[Uri] = None,
                  scopes: Map[String, String] = Map.empty,
                  description: Option[String] = None) extends SecurityScheme
