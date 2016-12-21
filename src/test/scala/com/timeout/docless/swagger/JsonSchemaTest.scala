package com.timeout.docless.swagger

import com.timeout.docless.JsonSchema
import com.timeout.docless.enumeratum.Schema
import enumeratum._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import io.circe._
import scala.reflect.runtime.{universe => u}

object JsonSchemaTest {
  val ref = "$ref"
  def id[T: u.WeakTypeTag] =
    getClass.getCanonicalName.replace('$','.') +
      implicitly[u.WeakTypeTag[T]].tpe.typeSymbol.name

  case class Foo(x: Int, y: String, z: Option[String]) {
    val otherVal = "not in schema"
  }

  case class Nested(name: String, foo: Foo)

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

  val fooSchema = JsonSchema.deriveFor[Foo]

  "genSchema" - {
    "handles plain case classes" in {
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

    "handles nested case classes" in {
      implicit val fs: JsonSchema[Foo] = fooSchema
      val schema = JsonSchema.deriveFor[Nested]
      parser.parse(
        s"""
          |{
          |  "type": "object",
          |  "required" : [
          |    "name",
          |    "foo"
          |  ],
          |  "properties" : {
          |    "name" : {
          |      "type" : "string"
          |    },
          |    "foo" : {
          |      "$ref" : "${id[Foo]}"
          |    }
          |  }
          |}
          |
          """.stripMargin) should === (Right(schema.asJson))
      schema.id should ===(id[Nested])
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
      "generates a union schema using the allOf keyword" in {
        val schema = JsonSchema.deriveFor[ADT]
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
        val schema = JsonSchema.deriveFor[ADT]
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
