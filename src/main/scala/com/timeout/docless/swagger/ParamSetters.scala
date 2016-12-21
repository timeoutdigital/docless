package com.timeout.docless.swagger

trait ParamSetters[T] {
  def withParams(param: OperationParameter*): T
}
