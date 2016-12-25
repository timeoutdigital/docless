package com.timeout.docless.swagger

import com.timeout.docless.schema.JsonSchema.Ref
import cats.syntax.foldable._
import cats.instances.list._
import cats.instances.map._
import com.timeout.docless.swagger.Path._

object Path {
  sealed trait RefWithContext {
    def path: String
    def ref: Ref
  }

  case class ParamRef(ref: Ref, path: String, param: String)
      extends RefWithContext

  case class ResponseRef(ref: Ref, path: String, method: Method)
      extends RefWithContext
}

case class Path(id: String,
                parameters: List[OperationParameter] = Nil,
                operations: Map[Method, Operation] = Map.empty)
    extends ParamSetters[Path] {

  private def paramRef(p: OperationParameter): Option[ParamRef] =
    p.schema.map(ParamRef(_, id, p.name))

  def paramRefs: Set[RefWithContext] =
    parameters.flatMap(paramRef).toSet ++
      operations.foldMap(_.parameters.flatMap(paramRef))

  def responseRefs: Set[RefWithContext] =
    operations.flatMap {
      case (m, op) =>
        val resps = op.responses.default :: op.responses.byStatusCode.values.toList
        resps.flatMap(_.schema.map(ResponseRef(_, id, m)))
    }.toSet

  def refs: Set[RefWithContext] = responseRefs ++ paramRefs

  def Get(op: Operation): Path = setMethod(Method.Get, op)

  def Delete(op: Operation): Path = setMethod(Method.Delete, op)

  def Post(op: Operation): Path = setMethod(Method.Post, op)

  def Put(op: Operation): Path = setMethod(Method.Put, op)

  def Patch(op: Operation): Path = setMethod(Method.Patch, op)

  def Head(op: Operation): Path = setMethod(Method.Head, op)

  def Options(op: Operation): Path = setMethod(Method.Options, op)

  private def setMethod(m: Method, op: Operation): Path =
    copy(operations = operations + (m -> op))

  override def withParams(param: OperationParameter*): Path =
    copy(parameters = param.toList)
}
