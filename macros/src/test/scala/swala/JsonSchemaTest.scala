package swala

import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import io.circe._
import io.circe.syntax._
import io.circe.parser._

class JsonSchemaTest extends FreeSpec {
  implicit val intSchema = new JsonSchema[Int] {
    override def asJson = Map("type" -> "integer").asJson
  }
  implicit val strSchema = new JsonSchema[String] {
    override def asJson = Map("type" -> "string").asJson
  }
  implicit val symSchema = new JsonSchema[Symbol] {
    override def asJson = Map("type" -> "string").asJson
  }
  implicit def optSchema[A: JsonSchema] = new JsonSchema[Option[A]] {
    override def asJson = implicitly[JsonSchema[A]].asJson
  }

  "the genSchema macro" - {
    "generates schema instance " in {
      case class Foo(x: Int, y: String, z: Option[String], private val a: Int) {
        val otherVal = "not in schema"
      }

      val fooSchema = JsonSchema.genSchema[Foo]
        parser.parse(
          """
            |{
            |  "id": "swala.JsonSchemaTest.Foo",
            |  "required" : [
            |    "x",
            |    "y"
            |  ],
            |  "properties" : {
            |    "x" : {
            |      "type" : "integer"
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
          """.stripMargin).toOption should === (Some(fooSchema.asJson))
    }

    "generates a union schema using the allOf keyword" in {
      sealed trait X
      case class Y(a: Int, b: String, c: Option[Int]) extends X
      case class Z(d: String, e: Int) extends X

      object X {
        val schema = JsonSchema.genSchema[X]
      }

      parser.parse("""
        |{
        |  "id" : "swala.JsonSchemaTest.X",
        |  "type" : "object",
        |  "allOf" : [
        |    {
        |      "required" : [
        |        "a",
        |        "b"
        |      ],
        |      "properties" : {
        |        "a" : {
        |          "type" : "integer"
        |        },
        |        "b" : {
        |          "type" : "string"
        |        },
        |        "c" : {
        |          "type" : "integer"
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
        |          "type" : "integer"
        |        }
        |      }
        |    }
        |  ]
        |}
      """.stripMargin).toOption should === (Some(X.schema.asJson))
    }
  }
}
