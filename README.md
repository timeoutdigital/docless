# Docless

[![Build Status](https://travis-ci.org/timeoutdigital/docless.svg?branch=master)](https://travis-ci.org/timeoutdigital/docless)

A scala DSL to generate JSON schema and [swagger](http://swagger.io) documentation for your web services.

-   [Why not just using Swagger-core?](#why-not-just-using-swagger-core)
    -   [JSON schema derivation](#json-schema-derivation)
    -   [Algebric data types](#algebric-data-types)
    -   [Swagger DSL](#swagger-dsl)
    -   [Aggregating documentation from multiple
        modules](#aggregating-documentation-from-multiple-modules)
-   [Todo](#todo)

Why not just using Swagger-core?
--------------------------------

While being to some extend usable for Scala projects,
[swagger-core](https://github.com/swagger-api/swagger-core) suffers from
some serious limitations:

-   It heavily relies on Java runtime reflection to generate Json
    schemas for your data models. This might be fine for plain Java
    objects, but it does not really play well with case classes and
    sealed trait hierarchies, which are key Scala idioms.

-   Swagger is built on top of JAX-RS annotations. These provide way
    more limited means of abstraction and code reuse than a DSL directly
    embedded into Scala.

    Installation
    ------------

Add the following to your `build.sbt`

``` {.scala}
resolvers += Resolver.bintrayRepo("topublic", "maven")

libraryDependencies + "com.timeout" %% "docless" % "0.1.0"
```

### JSON schema derivation

This project uses Shapeless to automatically derive JSON schemas for
case classes and ADTs at compile time. By scraping unnecessary
boilerplate code, this approach helps keeping documentation in sync with
the relevant business entities.

``` {.scala}
import com.timeout.docless.schema._

case class Pet(id: Int, name: String, tag: Option[String])

val petSchema = JsonSchema.deriveFor[Pet]
```

#### Case classes

Given a case class, generating a JSON schema is as easy as calling the
`deriveFor` method supplying `Pet` as the type parameter.

``` {.scala}
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

### Algebric data types

``` {.scala}
sealed trait Contact
case class EmailAndPhoneNum(email: String, phoneNum: String) extends Contact
case class EmailOnly(email: String) extends Contact
case class PhoneOnly(phoneNum: String) extends Contact

object Contact {
  val schema = JsonSchema.deriveFor[Contact]
}
```

Arguably, a correct JSON schema encoding for ADTs would use the *oneOf*
keyword (sum types). However, swagger encodes these data types using
*allOf*, which corresponds instead to union types.

``` {.scala}
scala> Contact.schema.asJson
res4: io.circe.Json =
{
  "type" : "object",
  "allOf" : [
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
method can be used to access all the other definitions referenced in our
type

``` {.scala}
scala> Contact.schema.relatedDefinitions
res5: Set[com.timeout.docless.schema.JsonSchema.Definition] =
Set(Definition(PhoneOnly,{
  "type" : "object",
  "required" : [
    "phoneNum"
  ],
  "properties" : {
    "phoneNum" : {
      "type" : "string"
    }
  }
}), Definition(EmailOnly,{
  "type" : "object",
  "required" : [
    "email"
  ],
  "properties" : {
    "email" : {
      "type" : "string"
    }
  }
}), Definition(EmailAndPhoneNum,{
  "type" : "object",
  "required" : [
    "email",
    "phoneNum"
  ],
  "properties" : {
    "email" : {
      "type" : "string"
    },
    "phoneNum" : {
      "type" : "string"
    }
  }
}))
```

#### Enums support

Docless supports encoding plain Scala enumerations as JSON schema enums
through the\
`JsonSchema.enum` method:

``` {.scala}
 sealed trait Diet {
  def id: String
}

object Diet {
  case object Herbivore extends Diet {
    override val id = "herbivore"
  }
  case object Carnivore extends Diet {
    override val id = "carnivore"
  }
  case object Omnivore extends Diet {
    override val id = "omnivore"
  }
  
  val values = Seq(Herbivore, Carnivore, Omnivore).map(_.id)
  
  implicit val schema = JsonSchema.enum(Diet.values)
}
```

``` {.scala}
scala> Diet.schema.asJson
res7: io.circe.Json =
{
  "enum" : [
    "herbivore",
    "carnivore",
    "omnivore"
  ]
}
```

Alternatively, types that extend
[enumeratum](https://github.com/lloydmeta/enumeratum)\
`EnumEntry` are also supported through the `EnumSchema` trait:

``` {.scala}

import enumeratum._
import com.timeout.docless.schema.Auto.EnumSchema

sealed trait RPS extends EnumEntry with EnumEntry.Snakecase 

object RPS extends Enum[RPS] with EnumSchema[RPS] {
  case object Rock extends RPS
  case object Paper extends RPS
  case object Scissors extends RPS
  
  override def values = findValues
}
```

This trait will define on the companion object an implicit
`JsonSchema[RPS]` instance:

``` {.scala}
scala> RPS.schema.asJson
res11: io.circe.Json =
{
  "enum" : [
    "Rock",
    "Paper",
    "Scissors"
  ]
}
```

### Swagger DSL

Docless provides a native scala implementation of the Swagger 2.0
specification together with a DSL which allows to easily manipulate and
transform such model.

``` {.scala}
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

This not only provides better means for abstraction that JSON or YAML
(i.e. variable binding, high order functions, implicit conversions,
etc.), but allows to integrate API documentation more tightly to the
application code.

### Aggregating documentation from multiple modules

Aside for using Circe for JSON serialisation, Docless is not coupled to
any specific Scala application framework. Nevertheless, it does provide
a generic facility to enrich separate code modules with Swagger metadata
(i.e. routes, controllers, or whatever else your framework calls them).

``` {.scala}
object DinosRoute extends PathGroup {

  val dinoSchema = JsonSchema.deriveFor[Dino]
  val dinoId = Parameter.path("id").as[Int]
  val dinoResp = dinoSchema.asResponse("A dinosaur!")

  override def definitions = Nil // <= this should be Dino.defintions

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
groups into a single Swagger file.

``` {.scala}
scala> PathGroup.aggregate(apiInfo, List(PetsRoute, DinosRoute))
res13: cats.data.ValidatedNel[com.timeout.docless.swagger.SchemaError,com.timeout.docless.swagger.APISchema] = Invalid(NonEmptyList(MissingDefinition(ResponseRef(TypeRef(Dino),/dinos/{id},Get))))
```

The `aggregate` method will also verify that the schema definitions
referenced either in endpoint responses or in body parameters can be
resolved. In the example above, the method returns a non-empty list with
a single `ResponseRef` error, pointing to the missing `Dino` definition.
On correct inputs, the method will return instead the resulting
`APISchema` wrapped into a `cats.data.Validated.Valid`.

Todo
----

-   Review ADT support and possibly implement ‘discriminator’ fields as
    per Swagger 2.0 spec.
-   Handle recursive types (e.g. linked lists, trees)
