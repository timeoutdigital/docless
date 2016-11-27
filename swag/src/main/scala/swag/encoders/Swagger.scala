package swag.encoders

import io.circe.{Encoder, Json}
import io.circe.syntax._
import swag.JsonSchema.{TypeRef, ArrayRef, Ref}
import swag._

import io.circe.generic.semiauto._
import encoders.Primitives._

object Swagger {

  implicit val externalDocsEncoder: Encoder[ExternalDocs] = deriveEncoder[ExternalDocs]
  implicit val contactEncoder: Encoder[Info.Contact] = deriveEncoder[Info.Contact]
  implicit val licenseEncoder: Encoder[Info.License] = deriveEncoder[Info.License]
  implicit val infoEncoder: Encoder[Info] = deriveEncoder[Info]
  implicit val externalDocEnc = deriveEncoder[ExternalDocs]

  implicit val securitySchemeEncoder = Encoder.instance[SecurityScheme]{ s =>
    val common = Json.obj(
      "name" -> s.name.asJson,
      "description" -> s.description.asJson
    )
    val other = s match {
      case Basic(_, _) =>
        Json.obj("type" -> "basic".asJson)
      case ApiKey(_, in, _) =>
        Json.obj("type" -> "api_key".asJson, "in" -> in.asJson)
      case OAuth2(_, flow, authUrl, tokenUrl, scopes, _) =>
        Json.obj(
          "type" -> "oauth2".asJson,
          "flow" -> flow.asJson,
          "authorizationUrl" -> authUrl.asJson,
          "tokenUrl" -> tokenUrl.asJson,
          "scopes" -> scopes.asJson
        )
    }
    common.deepMerge(other)
  }

  implicit val operationParameterEnc = Encoder.instance[OperationParameter] { p =>
    val common = Json.obj(
      "name" -> p.name.asJson,
      "required" -> p.required.asJson,
      "description" -> p.description.asJson
    )
    val other: Json = p match {
      case BodyParameter(_, _, _, schema) =>
        Json.obj("schema" -> schema.map(_.id).asJson)
      case Parameter(_, _, in, _, typ, format) =>
        Json.obj(
          "in" -> in.asJson,
          "type" -> typ.asJson,
          "format" -> format.asJson
        )
      case ArrayParameter(_, _, in, _, itemType, cFormat, minMax, format) =>
        Json.obj(
          "in" -> in.asJson,
          "type" -> "array".asJson,
          "items" -> Json.obj("type" -> itemType.asJson),
          "collectionFormat" -> cFormat.asJson,
          "format" -> format.asJson
        )
    }
    common.deepMerge(other)
  }

  implicit val definitionsEnc = Encoder.instance[Definitions] { defs =>
    defs.get.map(d => d.id -> d.json).toMap.asJson
  }

  implicit val schemaRefEnc = Encoder.instance[JsonSchema.Ref] { d =>
    val common = Json.obj(
      "$ref" -> Json.fromString(s"#/definitions/${d.id}")
    )
    val arrayType = d match {
      case ArrayRef(_) => Json.obj("type" -> Json.fromString("array"))
      case _ => Json.obj()
    }

    common.deepMerge(arrayType)
  }

  implicit val headerEnc = deriveEncoder[Responses.Header]
  implicit val responseEnc = deriveEncoder[Responses.Response]
  implicit val responsesEnc = Encoder.instance[Responses] { rs =>
    rs.byStatusCode.map { case (code, resp) => code -> resp.asJson}.asJson
  }
  implicit val securityReqEnc = Encoder.instance[SecurityRequirement] { sr =>
    Encoder[Map[String,List[String]]].apply(sr.bySchema)
  }
  implicit val opParamsEnc = Encoder.instance[OperationParameters] { params =>
    params.get.map(p => p.name -> p.asJson).toMap.asJson
  }
  implicit val operationEnc = deriveEncoder[Operation].mapJsonObject(_.remove("id"))

  implicit val pathEnc = deriveEncoder[Path].mapJsonObject(_.remove("id"))

  implicit val pathsEnc = Encoder.instance[Paths] { paths =>
    paths.get.map(d => d.id -> d.asJson).toMap.asJson
  }

  implicit val apiSchema = deriveEncoder[APISchema]
}
