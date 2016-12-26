package com.timeout.docless.schema

import io.circe._
import shapeless._
import shapeless.labelled.FieldType

trait Required[A] {
  def isRequired: Boolean = true
}

object Required {
  implicit def optIsRequired[A]: Required[Option[A]] =
    new Required[Option[A]] {
      override def isRequired = false
    }

  implicit def otherRequired[A]: Required[A] = new Required[A] {
    override def isRequired = true
  }

  def isRequired[A](implicit ev: Required[A]): Boolean = ev.isRequired

  trait Fields[A] {
    def get: List[String]
    def asJson: Json = Json.arr(get.map(Json.fromString): _*)
  }

  object Fields {
    def instance[A](fs: List[String]): Fields[A] = new Fields[A] {
      override def get: List[String] = fs
    }

    implicit val hnilFields: Fields[HNil] = instance(Nil)

    implicit def hlistFields[K <: Symbol, H, T <: HList](
        implicit witness: Witness.Aux[K],
        req: Lazy[Required[H]],
        tFields: Fields[T]
    ): Fields[FieldType[K, H] :: T] = instance {
      if (req.value.isRequired)
        witness.value.name :: tFields.get
      else
        tFields.get
    }

    implicit def genericFields[A, R](implicit gen: LabelledGeneric.Aux[A, R],
                                     rfs: Fields[R]): Fields[A] =
      instance(rfs.get)

    def apply[L](implicit ev: Fields[L]): List[String] = ev.get
  }
}
