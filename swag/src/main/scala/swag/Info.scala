package swag


object Info {
  case class Contact(name: Option[String] = None,
                     url: Option[String] = None,
                     email: Option[String] = None)

  case class License(name: String,
                     url: Option[String] = None)
}


case class Info(title: String,
                version: String = "1.0",
                description: Option[String] = None,
                termsOfService: Option[String] = None,
                contact: Option[Info.Contact] = None,
                license: Option[Info.License] = None)
