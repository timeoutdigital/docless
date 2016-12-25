package com.timeout.docless.swagger

import java.io._
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.timeout.docless.encoders.Swagger._
import io.circe._
import io.circe.syntax._
import org.scalatest.FreeSpec

import scala.collection.JavaConverters._

class SwaggerTest extends FreeSpec {
  "Can build and serialise a swagger object" in {
    val petstoreSchema = PetstoreSchema()
    val json = JsonLoader.fromResource("/swagger-schema.json")
    val schema = JsonSchemaFactory.byDefault().getJsonSchema(json)
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
