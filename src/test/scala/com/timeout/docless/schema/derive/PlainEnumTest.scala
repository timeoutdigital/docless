package com.timeout.docless.schema.derive

import com.timeout.docless.schema.PlainEnum
import org.scalatest.{FreeSpec, Matchers}
import PlainEnum.IdFormat

class PlainEnumTest extends FreeSpec with Matchers {
  sealed trait TheEnum
  case object EnumA extends TheEnum
  case object EnumB extends TheEnum

  sealed trait TheADT
  case class X(a: Int) extends TheADT
  case class Y(a: Int) extends TheADT

  "PlainEnum" - {
    "handles sealed traits of case objects only" in {
      val enum = PlainEnum.deriveFor[TheEnum]
      enum.ids should ===(List("EnumA", "EnumB"))
    }

    "allows to override format to lowercase" in {
      implicit val format: IdFormat = IdFormat.LowerCase
      val enum = PlainEnum.deriveFor[TheEnum]
      enum.ids should ===(List("enuma", "enumb"))
    }

    "allows to override format to uppercase" in {
      implicit val format: IdFormat = IdFormat.UpperCase
      val enum = PlainEnum.deriveFor[TheEnum]
      enum.ids should ===(List("ENUMA", "ENUMB"))
    }

    "allows to override format to snake case" in {
      implicit val format: IdFormat = IdFormat.SnakeCase
      val enum = PlainEnum.deriveFor[TheEnum]
      enum.ids should ===(List("enum_a", "enum_b"))
    }

    "allows to override format to upper snake case" in {
      implicit val format: IdFormat = IdFormat.UpperSnakeCase
      val enum = PlainEnum.deriveFor[TheEnum]
      enum.ids should ===(List("ENUM_A", "ENUM_B"))
    }

    "doesn't typecheck on ADTs" in {
      """
        PlainEnum.deriveFor[TheADT]
      """.stripMargin shouldNot typeCheck
    }
  }
}
