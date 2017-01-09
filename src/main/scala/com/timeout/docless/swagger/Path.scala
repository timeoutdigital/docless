package com.timeout.docless.swagger

import cats.syntax.foldable._
import cats.instances.list._
import cats.instances.map._

case class Path(id: String,
                parameters: List[OperationParameter] = Nil,
                operations: Map[Method, Operation] = Map.empty)
    extends ParamSetters[Path] {

  private def paramRef(p: OperationParameter): Option[RefWithContext] =
    p.schema.map(RefWithContext.param(_, id, p.name))

  def paramRefs: Set[RefWithContext] =
    parameters.flatMap(paramRef).toSet ++
      operations.foldMap(_.parameters.flatMap(paramRef))

  def responseRefs: Set[RefWithContext] =
    operations.flatMap {
      case (m, op) =>
        val resps = op.responses.default :: op.responses.byStatusCode.values.toList
        resps.flatMap { _.schema.map(RefWithContext.response(_, m, id)) }
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
