## Why not just using Swagger-core?

While being to some extent usable for Scala projects, [swagger-core](https://github.com/swagger-api/swagger-core) suffers from some serious limitations:

- It heavily relies on Java runtime reflection to generate Json schemas for your data models. This might be fine for plain Java objects, but it does not really play well with key scala idioms such as case classes and sealed trait hierarchies.

- Swagger is implemented through JAX-RS annotations. These provide way more limited means of abstraction and code reuse than a DSL directly embedded into Scala. 

## Installation 

Add the following to your `build.sbt`

```scala

libraryDependencies += "com.timeout" %% "docless" % doclessVersion
```
### JSON schema derivation

This project uses Shapeless to automatically derive JSON schemas for case classes and ADTs at compile time. By scraping unnecessary boilerplate code, this approach helps keeping documentation in sync with the relevant business entities.

```tut:silent
import com.timeout.docless.schema._

case class Pet(id: Int, name: String, tag: Option[String])

val petSchema = JsonSchema.deriveFor[Pet]
```

#### Case classes

Given a case class, generating a JSON schema is as easy as calling the `deriveFor` method and supplying the class as type parameter.

```tut
petSchema.asJson
```

The generated schema can be serialised to JSON by calling the `asJson` method, which will return a [Circe](https://github.com/travisbrown/circe) JSON ast. 

### Algebraic data types 

Arguably, the idea of ADT or sum type is best expressed using JsonSchema _oneOf_ keyword. However, as Swagger UI seems to only support the `allOf`,
this library uses the latter as default. This can be easily overriden by defining an implicit instance of `derive.Config` in the local scope:


```tut:silent
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


```tut
Contact.schema.asJson
```

For ADTs, as well as for case classes, the `JsonSchema.relatedDefinitions`
method can be used to access the child definitions referenced in a schema:
```tut
Contact.schema.relatedDefinitions.map(_.id)
```

#### Enums support

Docless can automatically derive a Json schema enum for sum types consisting of case objects only:

```tut:silent
sealed trait Diet

case object Herbivore extends Diet
case object Carnivore extends Diet
case object Omnivore extends Diet
```
Enumeration values can be automatically converted into a string identifier
using one of the pre-defined formats.

```tut
import com.timeout.docless.schema.PlainEnum.IdFormat

implicit val format: IdFormat = IdFormat.SnakeCase
val schema = JsonSchema.deriveEnum[Diet]
schema.asJson
```

Additionally, the popular library [enumeratum](https://github.com/lloydmeta/enumeratum) is also supported through the [EnumSchema`](https://github.com/timeoutdigital/docless/blob/master/src/main/scala/com/timeout/docless/schema/EnumSchema.scala) trait.
The trait can be simply mixed into the enum companion object and will automatically provide a Json Schema instance.

### Swagger DSL

Docless provides a native scala implementation of the Swagger 2.0 model together with a DSL to easily manipulate and transform it.

```tut:invisible
case class Error(code: Int, message: Option[String])
val errSchema = JsonSchema.deriveFor[Error]
val errorResponse = errSchema.asResponse("A server error")
```

```tut:silent
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
This not only provides better means of abstraction that JSON or YAML (i.e. binding, high order functions, implicit conversions, etc.), but it also allows to integrate API documentation more tightly to the application code.

### Aggregating documentation from multiple modules

Aside for using Circe for JSON serialisation, Docless is not coupled to any specific Scala web framework. Nevertheless, it does provide a generic facility to enrich separate code modules with Swagger metadata, being these routes, controllers, or whatever else your framework calls them.

```tut:silent
import com.timeout.docless.swagger._

case class Dino(name: String, extinctedSinceYears: Long, diet: Diet)

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
The `PathGroup` trait allows any Scala class or object to publish a list of endpoint paths and schema definitions. The `aggregate` method in the `PathGroup` companion object can then be used to merge the supplied groups into a single Swagger API description.

```tut
val apiInfo = Info("Example API")
PathGroup.aggregate(apiInfo, List(PetsRoute, DinosRoute))
```

The `aggregate` method will also verify that the schema definitions referenced either in endpoint responses or in body parameters can be resolved. In the example above, the method returns a non-empty list with a single `ResponseRef` error, pointing to the missing `Dino` definition. On correct inputs, the method will return instead the resulting `APISchema` wrapped into a `cats.data.Validated.Valid`.

## Known issues

Currently Docless does not support recursive types (e.g. trees or linked lists). As a way around, one can always define them manually using the `JsonSchema.instance[A]` method.
