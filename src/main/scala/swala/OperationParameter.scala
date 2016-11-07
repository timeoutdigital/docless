package swala

import scala.util.matching.Regex

object OperationParameter {
  sealed trait In
  case object Path extends In
  case object Query extends In
  case object Header extends In
  case object Form extends In

  sealed trait Type
  case object String extends Type
  case object Number extends Type
  case object Integer extends Type
  case object Boolean extends Type
  case object File extends Type

  sealed trait CollectionFormat
  case object CSV extends CollectionFormat
  case object SSV extends CollectionFormat
  case object TSV extends CollectionFormat
  case object Pipes extends CollectionFormat
  case object Multi extends CollectionFormat
}

trait OperationParameter {
  def name: String
  def required: Boolean
  def description: Option[String]
}

final case class BodyParameter(required: Boolean,
                               description: Option[String] = None,
                               name: String = "body",
                               schema: Option[Schema] = None) extends OperationParameter

final case class ArrayParameter(required: Boolean,
                                name: String,
                                in: OperationParameter.In,
                                description: Option[String] = None,
                                itemType: OperationParameter.Type = OperationParameter.String,
                                collectionFormat: Option[OperationParameter.CollectionFormat] = None,
                                minMax: Option[Range] = None,
                                format: Option[Regex] = None,
                                default: Option[String] = None) extends OperationParameter

final case class Parameter(required: Boolean,
                           name: String,
                           in: OperationParameter.In,
                           description: Option[String] = None,
                           typ: OperationParameter.Type = OperationParameter.String,
                           format: Option[Regex] = None,
                           default: Option[String] = None ) extends OperationParameter
