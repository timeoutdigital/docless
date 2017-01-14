package com.timeout.docless.schema

import enumeratum._
import io.circe._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._

import scala.reflect.runtime.{universe => u}

object JsonSchemaTest {
  val ref = "$ref"

  def id[T: u.WeakTypeTag] =
    getClass.getCanonicalName.replace('$', '.') +
      implicitly[u.WeakTypeTag[T]].tpe.typeSymbol.name

  sealed trait F extends EnumEntry

  sealed trait ADT

  sealed abstract class E extends EnumEntry

  case class Foo(x: Int, y: String, z: Option[String]) {
    val otherVal = "not in schema"
  }

  case class Nested(name: String, foo: Foo)

  case class X(e: E, f: F)

  case class A(a: Int, b: Char, c: Option[Double]) extends ADT

  case class B(d: Symbol, e: Long) extends ADT

  case class C(foo: Foo, g: Long) extends ADT

  object E extends Enum[E] with EnumSchema[E] {

    override val values = findValues

    case object E1 extends E

    case object E2 extends E
  }

  object F extends Enum[F] with EnumSchema[F] {

    override val values = findValues

    case object F1 extends F

    case object F2 extends F
  }

}

class JsonSchemaTest extends FreeSpec {

  import JsonSchemaTest._

  val fooSchema = JsonSchema.deriveFor[Foo]

  "automatic derivation" - {
    "handles plain case classes" in {
      parser.parse("""
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
        """.stripMargin) should ===(Right(fooSchema.asJson))
      fooSchema.id should ===(id[Foo])
    }

    "handles nested case classes" in {
      implicit val fs: JsonSchema[Foo] = fooSchema

      val schema = JsonSchema.deriveFor[Nested]
      parser.parse(s"""
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
                      |      "$ref" : "#/definitions/${id[Foo]}"
                      |    }
                      |  }
                      |}
                      |
          """.stripMargin) should ===(Right(schema.asJson))

      schema.id should ===(id[Nested])
      schema.relatedDefinitions should ===(Set(fs.namedDefinition("foo")))
    }

    "with types extending enumeratum.EnumEntry" - {
      "does not derive automatically" in {
        """
          sealed trait WontCompile extends EnumEntry
          object WontCompile extends Enum[WontCompile] {
            object A extends WontCompile
            object B extends WontCompile
            override def values = findValues

            val schema = JsonSchema.deriveFor[WontCompile]
          }

        """.stripMargin shouldNot typeCheck
      }
      "encodes enums when the EnumSchema[T] trait is extended" in {
        val schema = JsonSchema.deriveFor[X]

        parser.parse("""
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
          """.stripMargin) should ===(Right(schema.asJson))
      }
    }

    "with ADTs" - {
      "generates a union schema using the allOf keyword" in {
        val schema = JsonSchema.deriveFor[ADT]
        parser.parse(s"""
          {
            "type" : "object",
            "allOf" : [
              {
                "$ref": "#/definitions/${id[A]}"
              },
              {
                "$ref": "#/definitions/${id[B]}"
              },
              {
                "$ref": "#/definitions/${id[C]}"
              }
            ]
          }
        """.stripMargin) should ===(Right(schema.asJson))
      }

      "provides JSON definitions of the coproduct" in {
        implicit val fs: JsonSchema[Foo] = fooSchema

        val schema  = JsonSchema.deriveFor[ADT]
        val aSchema = JsonSchema.deriveFor[A]
        val bSchema = JsonSchema.deriveFor[B]
        val cSchema = JsonSchema.deriveFor[C]

        schema.relatedDefinitions should ===(
          Set(
            aSchema.definition,
            bSchema.definition,
            cSchema.definition,
            fooSchema.namedDefinition("foo")
          )
        )
      }
    }
  }
}
