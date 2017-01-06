package com.timeout.docless.swagger

import com.timeout.docless.schema.JsonSchema
import enumeratum._

object OperationParameter {
  sealed trait In extends EnumEntry with EnumEntry.Lowercase

  object In extends Enum[In] with CirceEnum[In] {
    case object Path   extends In
    case object Query  extends In
    case object Header extends In
    case object Form   extends In

    override def values = findValues
  }
}

trait OperationParameter extends HasSchema {
  def name: String
  def required: Boolean
  def description: Option[String]
  def mandatory: OperationParameter
  def schema: Option[JsonSchema.Ref] = None

  protected def setType[T <: Type](t: T): OperationParameter

  def as[T](implicit ev: Type.Primitive[T]) = setType(ev.get)
}

case class BodyParameter(description: Option[String] = None,
                         required: Boolean = false,
                         name: String = "body",
                         override val schema: Option[JsonSchema.Ref] = None)
    extends OperationParameter {
  override def mandatory                = copy(required = true)
  override def setType[T <: Type](t: T) = this
}

case class ArrayParameter(name: String,
                          required: Boolean = false,
                          in: OperationParameter.In,
                          description: Option[String] = None,
                          itemType: Type = Type.String,
                          collectionFormat: Option[CollectionFormat] = None,
                          minMax: Option[Range] = None,
                          format: Option[Format] = None)
    extends OperationParameter {

  def setType[T <: Type](t: T) = copy(`itemType` = t)

  override def mandatory = copy(required = true)
}

case class Parameter(name: String,
                     required: Boolean = false,
                     in: OperationParameter.In,
                     description: Option[String] = None,
                     `type`: Type = Type.String,
                     format: Option[Format] = None)
    extends OperationParameter {

  def setType[T <: Type](t: T) = copy(`type` = t)
  override def mandatory       = copy(required = true)
}

object Parameter {
  def query(name: String,
            required: Boolean = false,
            description: Option[String] = None,
            `type`: Type = Type.String,
            format: Option[Format] = None) =
    apply(
      name,
      required = false,
      OperationParameter.In.Query,
      description,
      `type`,
      format
    )

  def path(name: String,
           description: Option[String] = None,
           `type`: Type = Type.String,
           format: Option[Format] = None,
           default: Option[String] = None) =
    apply(
      name,
      required = true,
      OperationParameter.In.Path,
      description,
      `type`,
      format
    )
}
