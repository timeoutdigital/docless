package com.timeout.docless.swagger

import enumeratum._
import shapeless._

sealed trait Type extends EnumEntry with EnumEntry.Lowercase

object Type extends CirceEnum[Type] with Enum[Type] {

  override def values = findValues

  case object String extends Type

  case object Number extends Type

  case object Integer extends Type

  case object Boolean extends Type

  case object File extends Type

  trait Primitive[A] {
    def get: Type
  }

  object Primitive {
    def apply[A](t: Type): Primitive[A] = new Primitive[A] {
      override val get: Type = t
    }

    implicit val str: Primitive[String] = Primitive(String)

    implicit val int: Primitive[Int] = Primitive(Integer)

    implicit def num[N: Numeric](implicit ev: N <:!< Int): Primitive[N] =
      Primitive[N](Number)

    implicit def bool: Primitive[Boolean] = Primitive(Boolean)

    implicit val file: Primitive[java.io.File] = Primitive(File)
  }

}
