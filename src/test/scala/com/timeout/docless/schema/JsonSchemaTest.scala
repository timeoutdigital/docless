package com.timeout.docless.schema

import com.timeout.docless.schema.derive.{Combinator, Config}
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

  sealed abstract class E extends EnumEntry

  case class Foo(x: Int, y: String, z: Option[String]) {
    val otherVal = "not in schema"
  }

  case class Nested(name: String, foo: Foo)
  case class NestedOpt(name: String, fooOpt: Option[Foo])

  case class X(e: E, f: F)

  object E extends Enum[E] with EnumSchema[E] {
    override val values = findValues
    case object E1 extends E
    case object E2 extends E
  }

  sealed trait F extends EnumEntry
  object F extends Enum[F] with EnumSchema[F] {
    override val values = findValues
    case object F1 extends F
    case object F2 extends F
  }

  sealed trait TheEnum
  case object Enum1 extends TheEnum
  case object Enum2 extends TheEnum

  sealed trait TheADT
  case class A(a: Int, b: Char, c: Option[Double]) extends TheADT
  case class B(d: Symbol, e: Long) extends TheADT
  case class C(foo: Foo, g: Long) extends TheADT
  case class D(enum: TheEnum) extends TheADT
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

    "handles non primitive types" in {
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
      schema.relatedDefinitions should ===(Set(fs.NamedDefinition("foo")))
    }

    "handles non-primitive types as options" in {
      implicit val fs: JsonSchema[Foo] = fooSchema

      val schema = JsonSchema.deriveFor[NestedOpt]
      parser.parse(s"""
                      |{
                      |  "type": "object",
                      |  "required" : [
                      |    "name"
                      |  ],
                      |  "properties" : {
                      |    "name" : {
                      |      "type" : "string"
                      |    },
                      |    "fooOpt" : {
                      |      "$ref" : "#/definitions/${id[Foo]}"
                      |    }
                      |  }
                      |}
                      |
          """.stripMargin) should ===(Right(schema.asJson))

      schema.id should ===(id[NestedOpt])
      schema.relatedDefinitions should ===(Set(fs.NamedDefinition("fooOpt")))
    }

    "with types extending enumeratum.EnumEntry" - {
      "does not derive automatically" in {
        """
          sealed trait WontCompile extends EnumEntry
          object WontCompile extends Enum[WontCompile] {
            case object A extends WontCompile
            case object B extends WontCompile
            override def values = findValues

            val schema = JsonSchema.deriveFor[WontCompile]
          }

        """.stripMargin shouldNot typeCheck
      }
      "derives an enum when the EnumSchema[T] trait is extended" in {
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
    "with sealed traits of case objects" - {
      "generates an enumerable" in {
        val schema = JsonSchema.deriveEnum[TheEnum]

        schema.id should ===(id[TheEnum])
        parser.parse("""
          |{
          |  "enum" : ["Enum1", "Enum2"]
          |}
        """.stripMargin) should ===(Right(schema.asJson))
      }
    }

    "with ADTs" - {
      "generates a schema using the allOf keyword" in {
        val schema = JsonSchema.deriveFor[TheADT]
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
              },
              {
                "$ref": "#/definitions/${id[D]}"
              }
            ]
          }
        """.stripMargin) should ===(Right(schema.asJson))
      }
      "generates a schema using the oneOf keyword" in {
        implicit val conf = Config(Combinator.OneOf)
        val schema = JsonSchema.deriveFor[TheADT]
        parser.parse(s"""
          {
            "type" : "object",
            "oneOf" : [
              {
                "$ref": "#/definitions/${id[A]}"
              },
              {
                "$ref": "#/definitions/${id[B]}"
              },
              {
                "$ref": "#/definitions/${id[C]}"
              },
              {
                "$ref": "#/definitions/${id[D]}"
              }
            ]
          }
        """.stripMargin) should ===(Right(schema.asJson))
      }

      "provides JSON definitions of the coproduct" in {
        implicit val fs: JsonSchema[Foo] = fooSchema
        implicit val theEnumSchema: JsonSchema[TheEnum] = JsonSchema.deriveEnum[TheEnum]

        val schema  = JsonSchema.deriveFor[TheADT]
        val aSchema = JsonSchema.deriveFor[A]
        val bSchema = JsonSchema.deriveFor[B]
        val cSchema = JsonSchema.deriveFor[C]
        val dSchema = JsonSchema.deriveFor[D]

        schema.relatedDefinitions should ===(Set(
          aSchema.definition,
          bSchema.definition,
          cSchema.definition,
          dSchema.definition))
      }
    }
  }
}
