package com.timeout.docless.swagger

case class SecurityDefinitions(get: SecurityScheme*)
object SecurityDefinitions {
  val empty = SecurityDefinitions()
}
