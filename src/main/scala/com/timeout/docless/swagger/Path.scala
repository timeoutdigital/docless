package com.timeout.docless.swagger

case class Path(id: String,
                parameters: List[OperationParameter] = Nil,
                get: Option[Operation] = None,
                put: Option[Operation] = None,
                post: Option[Operation] = None,
                delete: Option[Operation] = None,
                options: Option[Operation] = None,
                patch: Option[Operation] = None,
                head: Option[Operation] = None) extends ParamSetters[Path] {

  def Get(op: Operation): Path = copy(get = Some(op))
  def Delete(op: Operation): Path = copy(delete = Some(op))
  def Post(op: Operation): Path = copy(post = Some(op))
  def Put(op: Operation): Path = copy(put = Some(op))
  def Patch(op: Operation): Path = copy(patch = Some(op))
  def Head(op: Operation): Path = copy(head = Some(op))
  def Options(op: Operation): Path = copy(options = Some(op))

  override def withParams(param: OperationParameter*): Path =
    copy(parameters = param.toList)
}
