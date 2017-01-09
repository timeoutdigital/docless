package com.timeout.docless.swagger

import org.scalatest.{FreeSpec, Inside, Matchers}
import cats.data.NonEmptyList
import cats.data.Validated
import SchemaError._
import com.timeout.docless.schema.JsonSchema
import com.timeout.docless.schema.JsonSchema._
import com.timeout.docless.swagger.Method._

class PathGroupTest extends FreeSpec with Matchers {
  "PathGroup" - {
    val petstore = PetstoreSchema()
    val pet      = PetstoreSchema.Schemas.pet

    val paths     = Path("/example") :: petstore.paths.get.toList
    val defs      = petstore.definitions.get.toList
    val defsNoPet = defs.filterNot(_.id === pet.id)
    val params    = petstore.parameters.get.toList

    val group1          = PathGroup(paths, defs, params)
    val group2          = PathGroup(List(Path("/extra")), Nil, Nil)
    val groupMissingErr = PathGroup(paths, defsNoPet, params)

    def err(path: String, m: Method, f: Definition => Ref): SchemaError =
      missingDefinition(RefWithContext.response(f(pet.definition), m, path))

    "aggregate" - {
      "when some top level definitions are missing" - {
        "returns the missing refs" in {
          PathGroup.aggregate(petstore.info, List(groupMissingErr)) should ===(
            Validated.invalid[NonEmptyList[SchemaError], APISchema](
              NonEmptyList.of(
                err("/pets", Get, ArrayRef.apply),
                err("/pets", Post, TypeRef.apply),
                err("/pets/{id}", Get, TypeRef.apply),
                err("/pets/{id}", Delete, TypeRef.apply)
              )
            )
          )
        }
      }
      "when some nested definitions are missing" - {
        val info = Info("example")
        case class Nested(name: String)
        case class TopLevel(nested: Nested)

        val schema = JsonSchema.deriveFor[TopLevel]
        val nested = schema.relatedDefinitions.head

        val paths = List(
          "/example".Post(
            Operation('_, "...")
              .withParams(BodyParameter(schema = Some(schema.asRef)))
          )
        )

        val withNested    = PathGroup(paths, schema.definitions.toList, Nil)
        val withoutNested = PathGroup(paths, List(schema.definition), Nil)

        "returns the missing refs" in {
          PathGroup.aggregate(info, List(withNested)).isValid shouldBe true
          PathGroup.aggregate(info, List(withoutNested)) should ===(
            Validated.invalid[NonEmptyList[SchemaError], APISchema](
              NonEmptyList.of(
                MissingDefinition(
                  RefWithContext.definition(nested.asRef, schema.definition)
                )
              )
            )
          )
        }
      }
      "when no definition is missing" - {
        "returns a valid api schema" in new Inside {
          inside(PathGroup.aggregate(petstore.info, List(group1, group2))) {
            case Validated.Valid(schema) =>
              schema.info should ===(petstore.info)
              schema.paths.get should ===(group1.paths ++ group2.paths)
              schema.definitions.get should ===(
                group1.definitions ++ group2.definitions
              )
              schema.parameters.get should ===(group1.params ++ group2.params)
          }
        }
      }
    }
  }
}
