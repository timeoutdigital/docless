# Docless

[![Build Status](https://travis-ci.org/timeoutdigital/docless.svg?branch=master)](https://travis-ci.org/timeoutdigital/docless)
[![Maven Central](https://img.shields.io/maven-central/v/com.timeout/docless_2.12.svg)](http://search.maven.org/#search|ga|1|com.timeout.docless)

A scala DSL to generate JSON schema and [swagger](http://swagger.io) documentation for your web services.

-   [Why not just using Swagger-core?](#why-not-just-using-swagger-core)
-   [Installation](#installation)
    -   [JSON schema derivation](#json-schema-derivation)
    -   [Algebraic data types](#algebraic-data-types)
    -   [Swagger DSL](#swagger-dsl)
    -   [Aggregating documentation from multiple
        modules](#aggregating-documentation-from-multiple-modules)
-   [Known issues](#known-issues)

Why not just using Swagger-core?
--------------------------------

While being to some extent usable for Scala projects,
[swagger-core](https://github.com/swagger-api/swagger-core) suffers from
some serious limitations:

-   It heavily relies on Java runtime reflection to generate Json
    schemas for your data models. This might be fine for plain Java
    objects, but it does not really play well with key scala idioms such
    as case classes and sealed trait hierarchies.

-   Swagger is implemented through JAX-RS annotations. These provide way
    more limited means of abstraction and code reuse than a DSL directly
    embedded into Scala.

Installation
------------

Add the following to your `build.sbt`

```scala
libraryDependencies += "com.timeout" %% "docless" % doclessVersion
```

### JSON schema derivation

This project uses Shapeless to automatically derive JSON schemas for
case classes and ADTs at compile time. By scraping unnecessary
boilerplate code, this approach helps keeping documentation in sync with
the relevant business entities.

```scala
import com.timeout.docless.schema._

case class Pet(id: Int, name: String, tag: Option[String])

val petSchema = JsonSchema.deriveFor[Pet]
```

#### Case classes

Given a case class, generating a JSON schema is as easy as calling the
`deriveFor` method and supplying the class as type parameter.

```scala
scala> petSchema.asJson
res2: io.circe.Json =
{
  "type" : "object",
  "required" : [
    "id",
    "name"
  ],
  "properties" : {
    "id" : {
      "type" : "integer",
      "format" : "int32"
    },
    "name" : {
      "type" : "string"
    },
    "tag" : {
      "type" : "string"
    }
  }
}
```

The generated schema can be serialised to JSON by calling the `asJson`
method, which will return a
[Circe](https://github.com/travisbrown/circe) JSON ast.

### Algebraic data types

Arguably, the idea of ADT or sum type is best expressed using JsonSchema
*oneOf* keyword. However, as Swagger UI seems to only support the
`allOf`,\
this library uses the latter as default. This can be easily overriden by
defining an implicit instance of `derive.Config` in the local scope:

```scala
import com.timeout.docless.schema.derive.{Config, Combinator}

sealed trait Contact
case class EmailAndPhoneNum(email: String, phoneNum: String) extends Contact
case class EmailOnly(email: String) extends Contact
case class PhoneOnly(phoneNum: String) extends Contact

object Contact {
  implicit val conf: Config = Config(Combinator.OneOf)
  val schema = JsonSchema.deriveFor[Contact]
}
```

```scala
scala> Contact.schema.asJson
res5: io.circe.Json =
{
  "type" : "object",
  "oneOf" : [
    {
      "$ref" : "#/definitions/EmailAndPhoneNum"
    },
    {
      "$ref" : "#/definitions/EmailOnly"
    },
    {
      "$ref" : "#/definitions/PhoneOnly"
    }
  ]
}
```

For ADTs, as well as for case classes, the
`JsonSchema.relatedDefinitions`\
method can be used to access the child definitions referenced in a
schema:

```scala
scala> Contact.schema.relatedDefinitions.map(_.id)
res6: scala.collection.immutable.Set[String] = Set(PhoneOnly, EmailOnly, EmailAndPhoneNum)
```

#### Enumerable support

Docless can automatically derive a Json schema enum for sum types
consisting of case objects only:

```scala

sealed trait Diet

case object Herbivore extends Diet
case object Carnivore extends Diet
case object Omnivore extends Diet
```

Enumeration values can be automatically converted into a string
identifier\
using one of the pre-defined formats.

```scala
import com.timeout.docless.schema.PlainEnum.IdFormat

implicit val format: IdFormat = IdFormat.SnakeCase
val schema = JsonSchema.deriveEnum[Diet]
```

```scala
scala> schema.asJson
res10: io.circe.Json =
{
  "enum" : [
    "herbivore",
    "carnivore",
    "omnivore"
  ]
}
```

Finally, types that extend
[enumeratum](https://github.com/lloydmeta/enumeratum) `EnumEntry` are
also supported through the `EnumSchema` trait:

```scala
import enumeratum._
import com.timeout.docless.schema.EnumSchema

sealed trait RPS extends EnumEntry with EnumEntry.Snakecase 

object RPS extends Enum[RPS] with EnumSchema[RPS] {
  case object Rock extends RPS
  case object Paper extends RPS
  case object Scissors extends RPS
  
  override def values = findValues
}
```

This trait will define on the companion object an implicit instance of\
`JsonSchema[RPS]`.

### Swagger DSL

Docless provides a native scala implementation of the Swagger 2.0 model
together with a DSL to easily manipulate and transform it.

```scala

import com.timeout.docless.swagger._
import com.timeout.docless.schema._

object PetsRoute extends PathGroup {
  val petResp = petSchema.asResponse("The pet")

  val petIdParam = Parameter
    .path(
      name = "id",
      description = Some("The pet id"),
      format = Some(Format.Int32)
    ).as[Int]

  override val definitions = List(petSchema, errSchema).map(_.definition)

  override val paths = List(
    "/pets/{id}"
       .Get(
         Operation(
           summary = Some("info for a specific pet")
         ).withParams(petIdParam)
          .responding(errorResponse)(200 -> petResp)
       )
       .Delete(
         Operation() //...
       )
 )
 
}
```

This not only provides better means of abstraction that JSON or YAML
(i.e. binding, high order functions, implicit conversions, etc.), but it
also allows to integrate API documentation more tightly to the
application code.

### Aggregating documentation from multiple modules

Aside for using Circe for JSON serialisation, Docless is not coupled to
any specific Scala web framework. Nevertheless, it does provide a
generic facility to enrich separate code modules with Swagger metadata,
being these routes, controllers, or whatever else your framework calls
them.

```scala
import com.timeout.docless.swagger._

case class Dino(name: String, extinctedSinceYears: Long, diet: Diet)

object DinosRoute extends PathGroup {

  val dinoSchema = JsonSchema.deriveFor[Dino]
  val dinoId = Parameter.path("id").as[Int]
  val dinoResp = dinoSchema.asResponse("A dinosaur!")

  override def definitions = Nil //<= this should be instead: `dinoSchema.definitions.toList`

  override def paths = List(
    "/dinos/{id}"
      .Get(
        Operation(
          summary = Some("info for a specific pet")
        ).withParams(dinoId)
         .responding(errorResponse)(200 -> dinoResp)
      )
    )
}
```

The `PathGroup` trait allows any Scala class or object to publish a list
of endpoint paths and schema definitions. The `aggregate` method in the
`PathGroup` companion object can then be used to merge the supplied
groups into a single Swagger API description.

```scala
scala> val apiInfo = Info("Example API")
apiInfo: com.timeout.docless.swagger.Info = Info(Example API,1.0,None,None,None,None)

scala> PathGroup.aggregate(apiInfo, List(PetsRoute, DinosRoute))
res15: cats.data.ValidatedNel[com.timeout.docless.swagger.SchemaError,com.timeout.docless.swagger.APISchema] = Invalid(NonEmptyList(MissingDefinition(RefWithContext(TypeRef(Dino,None),ResponseContext(Get,/dinos/{id})))))
```

The `aggregate` method will also verify that the schema definitions
referenced either in endpoint responses or in body parameters can be
resolved. In the example above, the method returns a non-empty list with
a single `ResponseRef` error, pointing to the missing `Dino` definition.
On correct inputs, the method will return instead the resulting
`APISchema` wrapped into a `cats.data.Validated.Valid`.

Known issues
------------

Currently Docless does not support recursive types (e.g. trees or linked
lists). As a way around, one can always define them manually using the
`JsonSchema.instance[A]` method.
