package com.timeout.docless.schema

package object derive {
  sealed trait Combinator
  case object Combinator {
    case object OneOf extends Combinator
    case object AllOf extends Combinator
  }

  case class Config(schemaCombinator: Combinator)
  object Config {
    implicit val default: Config = Config(Combinator.AllOf)
  }
}
