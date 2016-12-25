package com.timeout.docless.swagger

import org.scalatest.{FreeSpec, Inside, Matchers}
import cats.data.NonEmptyList
import cats.data.Validated
import SchemaError._
import com.timeout.docless.schema.JsonSchema._
import com.timeout.docless.swagger.Method._
import com.timeout.docless.swagger.Path._

class PathGroupTest extends FreeSpec with Matchers {
  "PathGroup" - {
    val petstore = PetstoreSchema()
    val pet = PetstoreSchema.Schemas.pet

    val paths = Path("example") :: petstore.paths.get.toList
    val defs = petstore.definitions.get.toList
    val defsNoPet = defs.filterNot(_.id === pet.id)
    val params =  petstore.parameters.get.toList

    val group1 = PathGroup(paths, defs, params)
    val group2 = PathGroup(List(Path("extra")), Nil, Nil)
    val groupMissingErr = PathGroup(paths, defsNoPet, params)

    def err(path: String, m: Method): SchemaError =
      missingDefinition(ResponseRef(ArrayRef(pet), path, m))

    "aggregate" - {
      "when some definitions are missing" - {
        "returns the missing refs" in {
          PathGroup.aggregate(petstore.info, List(groupMissingErr)) should ===(
            Validated.invalid[NonEmptyList[SchemaError], APISchema](
              NonEmptyList.of(
                err("/pets", Get),
                err("/pets", Post),
                err("/pets/{id}", Get),
                err("/pets/{id}", Delete)
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
              schema.definitions.get should ===(group1.definitions ++ group2.definitions)
              schema.parameters.get should ===(group1.params ++ group2.params)
          }
        }
      }
    }
  }
}

