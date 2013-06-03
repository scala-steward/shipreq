package com.beardedlogic.usecase
package lib

import model._
import java.lang.{Long => JLong}

/**
 * @since 30/05/2013
 */
object TypeTags {
  // TODO doc TypeTags

  sealed trait TypeTag[B]
  type Tagged[T <: TypeTag[_]] = {type Tag = T}
  type @@[O, T <: TypeTag[_]] = O with T
  @inline final def tag[T <: TypeTag[Long]](long: JLong) = long.asInstanceOf[JLong @@ T]
  @inline final def tag[T <: TypeTag[Long]](long: scala.Long) = JLong.valueOf(long).asInstanceOf[JLong @@ T]
  @inline final def tag[T <: TypeTag[Long]](long: Int) = JLong.valueOf(long).asInstanceOf[JLong @@ T]

  implicit def taggedStringOrdering[T <: TypeTag[String]] = implicitly[Ordering[String]].asInstanceOf[Ordering[String @@ T]]

  // -------------------------------------------------------------------------------------------------------------------
  // String tags

  trait NormalisedRefs extends TypeTag[String]
  trait LocalId extends TypeTag[String]
  trait Label extends TypeTag[String]

  implicit class StringTypeExt(val s: String) extends AnyVal {
    def hasNormalisedRefs = s.asInstanceOf[String @@ NormalisedRefs]
    def asLocalId = s.asInstanceOf[String @@ LocalId]
    def asLabel = s.asInstanceOf[String @@ Label]
  }

  implicit class StringTypeExt2[M[_]](val s: M[String]) extends AnyVal {
    def asLocalIds = s.asInstanceOf[M[String @@ LocalId]]
    def asLabels = s.asInstanceOf[M[String @@ Label]]
  }

  implicit class StringTypeExt3[M[_]](val s: M[List[String]]) extends AnyVal {
    def asLocalIds = s.asInstanceOf[M[List[String @@ LocalId]]]
    def asLabels = s.asInstanceOf[M[List[String @@ Label]]]
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Long tags

  implicit class LongTypeExt(val x: Long) extends AnyVal {
    def tag[T <: TypeTag[Long]] = JLong.valueOf(x).asInstanceOf[JLong @@ T]
  }

  trait FieldKeyId extends TypeTag[Long]
  type Long_FieldKeyId = JLong @@ FieldKeyId
  @inline final implicit def FieldKeyToId(v: Value[DataType.FieldKey]) = tag[FieldKeyId](v.valueId)

  trait StepValueId extends TypeTag[Long]
  type Long_StepValueId = JLong @@ StepValueId
  @inline final implicit def StepValueIdExtractor(v: PlainValue[DataType.Step]) = v.valueId.tag[StepValueId]

  trait StepDataId extends TypeTag[Long]
  type Long_StepDataId = JLong @@ StepDataId
  @inline final implicit def StepDataIdExtractor(v: PlainValue[DataType.Step]) = v.dataId.tag[StepDataId]

  implicit class StepValueExt(val v: PlainValue[DataType.Step]) extends AnyVal {
    def taggedDataId: Long_StepDataId = StepDataIdExtractor(v)
    def taggedValueId: Long_StepValueId = StepValueIdExtractor(v)
  }
}
