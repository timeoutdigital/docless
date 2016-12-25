package com.timeout.docless.encoders

import com.timeout.docless.swagger._
import com.timeout.docless.schema.JsonSchema
import com.timeout.docless.schema.JsonSchema.{ArrayRef, TypeRef}

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

object Swagger {

  implicit val externalDocsEncoder: Encoder[ExternalDocs] =
    deriveEncoder[ExternalDocs]
  implicit val contactEncoder: Encoder[Info.Contact] =
    deriveEncoder[Info.Contact]
  implicit val licenseEncoder: Encoder[Info.License] =
    deriveEncoder[Info.License]
  implicit val infoEncoder: Encoder[Info] = deriveEncoder[Info]
  implicit val externalDocEnc = deriveEncoder[ExternalDocs]

  implicit val securitySchemeEncoder = Encoder.instance[SecurityScheme] { s =>
    val common = Map(
      "name" -> s.name.asJson,
      "description" -> s.description.asJson
    )

    val other = s match {
      case Basic(_, _) =>
        Map("type" -> "basic".asJson)
      case ApiKey(_, in, _) =>
        Map("type" -> "api_key".asJson, "in" -> in.asJson)
      case OAuth2(_, flow, authUrl, tokenUrl, scopes, _) =>
        Map(
          "type" -> "oauth2".asJson,
          "flow" -> flow.asJson,
          "authorizationUrl" -> authUrl.asJson,
          "tokenUrl" -> tokenUrl.asJson,
          "scopes" -> scopes.asJson
        )
    }
    Json.fromFields(common ++ other)
  }

  implicit val operationParameterEnc: Encoder[OperationParameter] =
    Encoder.instance[OperationParameter] { p =>
      val common = Map(
        "name" -> p.name.asJson,
        "required" -> p.required.asJson,
        "description" -> p.description.asJson
      )
      val other = p match {
        case BodyParameter(_, _, _, schema) =>
          Map("schema" -> schema.asJson, "in" -> "body".asJson)
        case Parameter(_, _, in, _, typ, format) =>
          Map(
            "in" -> in.asJson,
            "type" -> typ.asJson,
            "format" -> format.asJson
          )
        case ArrayParameter(_, _, in, _, itemType, cFormat, minMax, format) =>
          Map(
            "in" -> in.asJson,
            "type" -> "array".asJson,
            "items" -> Json.obj("type" -> itemType.asJson),
            "collectionFormat" -> cFormat.asJson,
            "format" -> format.asJson
          )
      }
      Json.fromFields(common ++ other)
    }

  implicit val definitionsEnc = Encoder.instance[Definitions] { defs =>
    defs.get.map(d => d.id -> d.json).toMap.asJson
  }

  implicit val schemaRefEnc = Encoder.instance[JsonSchema.Ref] {
    case ArrayRef(id) =>
      Json.obj(
        "type" -> Json.fromString("array"),
        "items" -> Json.obj(
          "$ref" -> Json.fromString(s"#/definitions/$id")
        )
      )
    case TypeRef(id) =>
      Json.obj("$ref" -> Json.fromString(s"#/definitions/$id"))
  }

  implicit val headerEnc = deriveEncoder[Responses.Header]
  implicit val responseEnc = deriveEncoder[Responses.Response]
  implicit val responsesEnc = Encoder.instance[Responses] { rs =>
    rs.byStatusCode.map { case (code, resp) => code -> resp.asJson }.asJson
  }
  implicit val securityReqEnc = Encoder.instance[SecurityRequirement] { sr =>
    Encoder[Map[String, List[String]]].apply(sr.bySchema)
  }
  implicit val opParamsEnc = Encoder.instance[OperationParameters] { params =>
    params.get.map(p => p.name -> p.asJson).toMap.asJson
  }
  implicit val operationEnc =
    deriveEncoder[Operation].mapJsonObject(_.remove("id"))

  implicit val pathEnc = Encoder.instance[Path] { p =>
    val obj = JsonObject.singleton("parameters", p.parameters.asJson)
    p.operations
      .foldLeft(obj) {
        case (acc, (method, op)) =>
          acc.+:(method.entryName -> op.asJson)
      }
      .asJson
  }

  implicit val pathsEnc = Encoder.instance[Paths] { paths =>
    paths.get.map(d => d.id -> d.asJson).toMap.asJson
  }

  implicit val apiSchema = deriveEncoder[APISchema]
}
