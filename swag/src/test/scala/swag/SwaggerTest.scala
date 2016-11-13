package swag

import org.scalatest.FreeSpec

import swag.Info.License
import swag.Responses.{Header, Response}
import swag.encoders.Swagger._
import io.circe._
import io.circe.syntax._

class SwaggerTest extends FreeSpec {
  "json encode formats" - {
    val binary: Format = Format.Binary
    println(s"got a format: ${binary.asJson}")
  }
  "Can build and serialise a swagger object" in {
    APISchema(
      info = Info(title = "Swagger petstore", license = Some(License(name = "MIT"))),
      host = "petstore.swagger.io",
      basePath  = "/v1",
      schemes = Set(Scheme.Http),
      consumes = Set("application/json"),
      produces = Set("application/json"),
      paths = List(
        Path(
          ref = "/pets",

          get = Some(Operation(
            id = Some("listPets"),
            tags = List("pets"),
            summary = Some("List all pets"),

            parameters = List(
              Parameter.query(
                name = "limit",
                description = Some("How many items to return at one time (max 100)"),
                format = Some(Format.Int32)
              ).as(Type.Integer)
            ),

            responses = Responses(
              default = Response("default"), //TODO: implement default response
              byStatusCode = Map(
                200 -> Response(
                  schema = None, //TODO: add schema here
                  description = "A paged array of pets",
                  headers = Map(
                    "x-next" -> Header(
                      `type` = Type.String,
                      description = Some("A link to the next page of responses")
                    )
                  )
                )
              )
            )
          )),

          post = Some(Operation(
            id = Some("createPets"),
            tags = List("pets"),
            summary = Some("Create a pet"),

            responses = Responses(
              default = Response("default"), //TODO: implement default response
              byStatusCode = Map(
                201 -> Response(
                  description = "Null response"
                )
              )
            )
          ))
        ),

        Path(
          ref = "/pets/{petId}",
          get = Some(Operation(
            id = Some("showPetById"),
            tags = List("pets"),
            summary = Some("info for a specific pet"),

            parameters = List(
              Parameter.path(
                required = true,
                name = "petId",
                description = Some("How many items to return at one time (max 100)"),
                format = Some(Format.Int32)
              ).asInteger
            ),

            responses = Responses(
              default = Response("default") //TODO: implement default response
            ))
          )
        )
      )
    )
  }
}
