package swag

import enumeratum._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import io.circe._
import swag.encoders.Primitives._
import io.circe.syntax._
import io.circe.parser._

class JsonSchemaTest extends FreeSpec {
  "the genSchema macro" - {
    "generates schema instance " in {

      case class Foo(x: Int, y: String, z: Option[String], private val a: Int) {
        val otherVal = "not in schema"
      }

      val fooSchema = JsonSchema.genSchema[Foo]
      parser.parse(
        """
          |{
          |  "id": "swag.JsonSchemaTest.Foo",
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
      sealed abstract class E extends EnumEntry

      object E extends Enum[E] {
        case object E1 extends E
        case object E2 extends E
        override val values = findValues
      }

      sealed trait F extends EnumEntry
      object F extends Enum[F] {
        case object F1 extends F
        case object F2 extends F
        override val values = findValues
      }

      case class X(e: E, f: F)

      "generates enum properties" in {
        val schema = JsonSchema.genSchema[X]
        parser.parse(
          """
            |{
            |  "id": "swag.JsonSchemaTest.X",
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
      sealed trait X
      case class Y(a: Int, b: Char, c: Option[Double]) extends X
      case class Z(d: Symbol, e: Long) extends X

      object X {
        val schema = JsonSchema.genSchema[X]
      }

      parser.parse("""
        |{
        |  "id" : "swag.JsonSchemaTest.X",
        |  "type" : "object",
        |  "allOf" : [
        |    {
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
      """.stripMargin) should === (Right(X.schema.asJson))


    }


  }
}
