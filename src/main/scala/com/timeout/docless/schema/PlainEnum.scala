package com.timeout.docless.schema

import java.util.regex.Pattern

import scala.reflect.runtime.{universe => ru}
import enumeratum.EnumEntry
import shapeless._
import shapeless.labelled._
import shapeless.ops.hlist

trait PlainEnum[A] {
  def ids: List[String]
}

object PlainEnum {
  sealed trait IdFormat {
    def apply(s: String): String
  }

  object IdFormat {
    case object SnakeCase extends IdFormat {
      override def apply(s: String) = snakify(s)
    }

    case object UpperSnakeCase extends IdFormat {
      override def apply(s: String) = snakify(s).toUpperCase()
    }

    case object UpperCase extends IdFormat {
      override def apply(s: String) = s.toUpperCase
    }

    case object LowerCase extends IdFormat {
      override def apply(s: String) = s.toLowerCase
    }

    case object Default extends IdFormat {
      override def apply(s: String) = s
    }

    /**
      *
      * Verbatim copy of Enumeratum's snake case implementation.
      *
      * Original implementations:
      * - https://github.com/lloydmeta/enumeratum/blob/445f12577c1f8c66de94a43be797546e569fdc44/enumeratum-core/src/main/scala/enumeratum/EnumEntry.scala#L39
      * - https://github.com/lift/framework/blob/a3075e0676d60861425281427aa5f57c02c3b0bc/core/util/src/main/scala/net/liftweb/util/StringHelpers.scala#L91
      */
    private val snakifyRegexp1 = Pattern.compile("([A-Z]+)([A-Z][a-z])")
    private val snakifyRegexp2 = Pattern.compile("([a-z\\d])([A-Z])")
    private val snakifyReplacement = "$1_$2"

    private def snakify(s: String): String = {
      val first = snakifyRegexp1.matcher(s).replaceAll(snakifyReplacement)
      snakifyRegexp2.matcher(first).replaceAll(snakifyReplacement).toLowerCase
    }

    implicit val default: IdFormat = Default
  }

  def instance[A](_ids: List[String]): PlainEnum[A] = new PlainEnum[A] {
    override def ids = _ids
  }

  implicit val cnilEnum: PlainEnum[CNil] = instance(Nil)

  implicit def coprodEnum[K <: Symbol, H, T <: Coproduct, HL <: HList, N <: Nat](
    implicit
    witness: Witness.Aux[K],
    gen: Generic.Aux[H, HL],
    hLen: hlist.Length.Aux[HL, N],
    lazyEnum: Lazy[PlainEnum[T]],
    zeroLen: N =:= Nat._0,
    format: IdFormat
  ): PlainEnum[FieldType[K, H] :+: T] =
    instance(format(witness.value.name) :: lazyEnum.value.ids)

  implicit def genericPlainEnum[A, R <: Coproduct](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    enum: PlainEnum[R],
    format: IdFormat,
    ev: A <:!< EnumEntry
  ): PlainEnum[A] = instance(enum.ids)

  def deriveFor[A](implicit ev: PlainEnum[A]): PlainEnum[A] = ev
}
