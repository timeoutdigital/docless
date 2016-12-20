package com.timeout.docless.swagger

import com.timeout.docless.JsonSchema
import com.timeout.docless.enumeratum.Schema
import enumeratum._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import io.circe._
import io.circe.syntax._
import io.circe.parser._

object JsonSchemaTest {
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

      val fooSchema = JsonSchema.genSchema[Foo]
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
  }

    "With types extending EnumEntry" - {
      "generates enum properties" in {
        val schema = JsonSchema.genSchema[X]
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

    "generates a union schema using the allOf keyword" in {


      val schema = JsonSchema.genSchema[ADT]

      parser.parse("""
        |{
        |  "type" : "object",
        |  "allOf" : [
        |    {
        |      "type": "object",
        |      "required" : [
        |        "a",
        |        "b"
        |      ],
        |      "properties" : {
        |        "a" : {
        |          "type" : "integer",
        |          "format": "int32"
        |        },
        |        "b" : {
        |          "type" : "string"
        |        },
        |        "c" : {
        |          "type" : "number",
        |          "format": "double"
        |        }
        |      }
        |    },
        |    {
        |      "type": "object",
        |      "required" : [
        |        "d",
        |        "e"
        |      ],
        |      "properties" : {
        |        "d" : {
        |          "type" : "string"
        |        },
        |        "e" : {
        |          "type" : "integer",
        |          "format": "int64"
        |        }
        |      }
        |    }
        |  ]
        |}
      """.stripMargin) should === (Right(schema.asJson))
    }
  }
}
