## Why not just using Swagger-core?

While being to some extent usable for Scala projects, [swagger-core](https://github.com/swagger-api/swagger-core) suffers from some serious limitations:

- It heavily relies on Java runtime reflection to generate Json schemas for your data models. This might be fine for plain Java objects, but it does not really play well with key scala idioms such as case classes and sealed trait hierarchies.

- Swagger is implemented through JAX-RS annotations. These provide way more limited means of abstraction and code reuse than a DSL directly embedded into Scala. 

## Installation 

Add the following to your `build.sbt`

```scala
resolvers += Resolver.bintrayRepo("topublic", "maven")

libraryDependencies + "com.timeout" %% "docless" % "0.2.0"
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

```tut:silent
sealed trait Contact
case class EmailAndPhoneNum(email: String, phoneNum: String) extends Contact
case class EmailOnly(email: String) extends Contact
case class PhoneOnly(phoneNum: String) extends Contact

object Contact {
  val schema = JsonSchema.deriveFor[Contact]
}
```
Arguably, a correct JSON schema encoding for ADTs would use the _oneOf_ keyword. However, for historical reasons Swagger encodes these data types using _allOf_.
```tut
Contact.schema.asJson
```

For ADTs, as well as for case classes, the `JsonSchema.relatedDefinitions`
method can be used to access the other definitions referenced in a schema:
```tut
Contact.schema.relatedDefinitions.map(_.id)
```

#### Enums support

Docless allows encoding any list of strings as JSON schema enums through the
`JsonSchema.enum` method:

```tut:silent
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
```tut
Diet.schema.asJson
```

Types that extend [enumeratum](https://github.com/lloydmeta/enumeratum) `EnumEntry` are also supported through the `EnumSchema` trait:

```tut:silent

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
This trait will define on the companion object an implicit instance of `JsonSchema[RPS]`:
```tut
RPS.schema.asJson
```

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

```tut:invisible
import com.timeout.docless.swagger._

val apiInfo = Info("Example API")

case class Dino(name: String, extinctedSinceYears: Long, diet: Diet)
```
```tut:silent
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
PathGroup.aggregate(apiInfo, List(PetsRoute, DinosRoute))
```

The `aggregate` method will also verify that the schema definitions referenced either in endpoint responses or in body parameters can be resolved. In the example above, the method returns a non-empty list with a single `ResponseRef` error, pointing to the missing `Dino` definition. On correct inputs, the method will return instead the resulting `APISchema` wrapped into a `cats.data.Validated.Valid`.

## Todo

- Review ADT support and possibly implement 'discriminator' fields as per Swagger 2.0 spec.
- Handle recursive types (e.g. linked lists, trees)
