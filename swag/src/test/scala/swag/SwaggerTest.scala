package swag

import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.ProcessingReport
import org.scalatest.FreeSpec

import cats.syntax.either._
import cats.instances.try_._
import swag.Info.License
import swag.JsonSchema.{TypeRef, ArrayRef}
import swag.Responses.{Header, Response}
import swag.encoders.Swagger._
import io.circe._
import io.circe.syntax._
import scala.io.Source
import swag.encoders.Primitives._
import scala.util.Try


import com.github.fge.jsonschema.main.JsonSchemaFactory
import collection.JavaConverters._
import java.io._

class SwaggerTest extends FreeSpec {
  "Can build and serialise a swagger object" in {
    case class Pet(id: Int, name: String, tag: Option[String])
    case class Error(code: Int, message: Option[String])

    object Defs {
      val pet = JsonSchema.genSchema[Pet]
      val error = JsonSchema.genSchema[Error]
      val all = List(pet, error).map(_.definition)
    }

    val errorResponse = Response(
      description = "API error",
      schema = Some(TypeRef(Defs.error))
    )

    val petIdParam = Parameter.path(
      name = "id",
      description = Some("The pet id"),
      format = Some(Format.Int32)
    ).asInteger

    val petstoreSchema = APISchema(
      info = Info(title = "Swagger petstore", license = Some(License(name = "MIT"))),
      host = "petstore.swagger.io",
      basePath  = "/v1",
      schemes = Set(Scheme.Http),
      consumes = Set("application/json"),
      produces = Set("application/json")

    ).defining(Defs.all: _*)
     .withPaths (
       Path("/pets")
         .Get(Operation(
            operationId = Some("listPets"),
            tags = List("pets"),
            summary = Some("List all pets"),
            responses = Responses(
              default = errorResponse,
              byStatusCode = Map(
                200 -> Response(
                  schema = Some(ArrayRef(Defs.pet)),
                  description = "A paged array of pets",
                  headers = Map(
                    "x-next" -> Header(
                      `type` = Type.String,
                      description = Some("A link to the next page of responses")
                    )
                  )
                )
              )
            )).withParams(Parameter.query(
             name = "limit",
             description = Some("How many items to return at one time (max 100)"),
             format = Some(Format.Int32)
           ).as(Type.Integer))

         ).Post(Operation(
            operationId = Some("createPets"),
            tags = List("pets"),
            summary = Some("Create a pet"),

            responses = Responses(
              default = errorResponse,
              byStatusCode = Map(
                201 -> Response(
                  description = "pet response",
                  schema = Some(ArrayRef(Defs.pet))
                )
              )
            )
       )),

        Path(id="/pets/{id}")
          .Get(Operation(
            parameters = List(petIdParam),
            operationId = Some("showPetById"),
            tags = List("pets"),
            summary = Some("info for a specific pet"),
            responses = Responses(
              default = errorResponse,
              byStatusCode = Map(
                200 -> Response(
                  description = "pet response",
                  schema = Some(ArrayRef(Defs.pet))
                )
              )
            ))
          ).Delete(Operation(
            operationId = Some("deletePetById"),
            tags = List("pets"),
            summary = Some("deletes a single pet"),
            responses = Responses(
              default = errorResponse,
              byStatusCode = Map(
                204 -> Response(
                  description = "pet response",
                  schema = Some(ArrayRef(Defs.pet))
                )
              )
            )
          ))
    )

    val json = JsonLoader.fromResource("/swagger-schema.json")
    val schema = JsonSchemaFactory.byDefault().getJsonSchema(json)
    val example = Source.fromFile("petstore-simple.json").mkString
    val printer = Printer.spaces2.copy(dropNullKeys = true)
    val jsonS = printer.pretty(petstoreSchema.asJson)
    val report = schema.validate(JsonLoader.fromString(jsonS))

    if (!report.isSuccess) {
      println(jsonS)
      println("============== Validation errors ================================")
      val errors = report.asScala.toList
      errors.foreach(println)
      fail()
    } else {
      val pw = new PrintWriter(new File("dist/petstore.json"))
      pw.write(jsonS)
      pw.close()
    }
  }
}
