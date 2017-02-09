package com.timeout.docless.swagger

sealed trait SecurityRequirement {
  def name: String
  def scope: List[String]
}

object SecurityRequirement {
  def apply(scheme: SecurityScheme) = new SecurityRequirement {
    override val name = scheme.name
    override val scope = Nil
  }

  def apply(oAuth2: OAuth2, oAuthScope: List[String]) = new SecurityRequirement {
    override val name = oAuth2.name
    override val scope = oAuthScope
  }
}
