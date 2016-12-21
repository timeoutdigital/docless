package com.timeout.docless.swagger

import com.timeout.docless.JsonSchema
import com.timeout.docless.enumeratum.Schema
import enumeratum._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import io.circe._
import scala.reflect.runtime.{universe => u}

object JsonSchemaTest {
  def id[T: u.WeakTypeTag] =
    implicitly[u.WeakTypeTag[T]].tpe.typeSymbol.fullName

  case class Foo(x: Int, y: String, z: Option[String]) {
    val otherVal = "not in schema"
  }

  sealed abstract class E extends EnumEntry

  object E extends Enum[E] with Schema[E] {
    case object E1 extends E
    case object E2 extends E
    override val values = findValues
  }

  sealed trait F extends EnumEntry
  object F extends Enum[F] with Schema[F] {
    case object F1 extends F
    case object F2 extends F
    override val values = findValues
  }

  case class X(e: E, f: F)

  sealed trait ADT
  case class Y(a: Int, b: Char, c: Option[Double]) extends ADT
  case class Z(d: Symbol, e: Long) extends ADT
  case class Z1(f: Symbol, g: Long) extends ADT
}

class JsonSchemaTest extends FreeSpec {
  import JsonSchemaTest._

  "genSchema" - {
    "derives schema instance " in {

      val fooSchema = JsonSchema.deriveFor[Foo]
      parser.parse(
        """
          |{
          |  "type": "object",
          |  "required" : [
          |    "x",
          |    "y"
          |  ],
          |  "properties" : {
          |    "x" : {
          |      "type" : "integer",
          |      "format": "int32"
          |    },
          |    "y" : {
          |      "type" : "string"
          |    },
          |    "z" : {
          |      "type" : "string"
          |    }
          |  }
          |}
          |
        """.stripMargin) should === (Right(fooSchema.asJson))
      fooSchema.id should === (id[Foo])
  }

    "with types extending enumeratum.EnumEntry" - {
      "encodes enums" in {
        val schema = JsonSchema.deriveFor[X]

        parser.parse(
          """
            |{
            |  "type": "object",
            |  "required" : [
            |    "e",
            |    "f"
            |  ],
            |  "properties" : {
            |    "e" : {
            |      "enum" : ["E1", "E2"]
            |    },
            |    "f" : {
            |      "enum" : ["F1", "F2"]
            |    }
            |  }
            |}
            |
          """.stripMargin) should === (Right(schema.asJson))
      }
    }

    "with ADTs" - {
      val ref = "$ref"
      val schema = JsonSchema.deriveFor[ADT]
      "generates a union schema using the allOf keyword" in {
        parser.parse(
          s"""
          {
            "type" : "object",
            "allOf" : [
              {
                "$ref": "${id[Y]}"
              },
              {
                "$ref": "${id[Z]}"
              },
              {
                "$ref": "${id[Z1]}"
              }
            ]
          }
        """.stripMargin) should === (Right(schema.asJson))
      }

      "provides JSON definitions of the coproduct" in {
        val ySchema = JsonSchema.deriveFor[Y]
        val zSchema = JsonSchema.deriveFor[Z]
        val z1Schema = JsonSchema.deriveFor[Z1]

        schema.definitions(0) should === (ySchema.definition)
        schema.definitions(1) should === (zSchema.definition)
        schema.definitions(2) should === (z1Schema.definition)
      }
    }
  }
}
