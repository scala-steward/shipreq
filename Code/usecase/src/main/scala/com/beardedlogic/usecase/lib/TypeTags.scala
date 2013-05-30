package com.beardedlogic.usecase
package lib

import model._
import java.lang.{Long => JLong}

/**
 * @since 30/05/2013
 */
object TypeTags {

  sealed trait TypeTag
  type Tagged[T <: TypeTag] = {type Tag = T}
  type @@[O, T <: TypeTag] = O with T
  @inline final def tag[T <: TypeTag](long: JLong) = long.asInstanceOf[JLong @@ T]
  @inline final def tag[T <: TypeTag](long: scala.Long) = JLong.valueOf(long).asInstanceOf[JLong @@ T]
  @inline final def tag[T <: TypeTag](long: Int) = JLong.valueOf(long).asInstanceOf[JLong @@ T]

  implicit class StringTypeExt(val s: String) extends AnyVal {
    def hasNormalisedRefs = s.asInstanceOf[String @@ NormalisedRefs]
    def asLocalStepId = s.asInstanceOf[String @@ LocalStepId]
  }

  implicit class LongTypeExt(val x: Long) extends AnyVal {
    def tag[T <: TypeTag] = JLong.valueOf(x).asInstanceOf[JLong @@ T]
  }

  trait NormalisedRefs extends TypeTag
  trait LocalStepId extends TypeTag

  trait FieldKeyId extends TypeTag
  type Long_FieldKeyId = JLong @@ FieldKeyId
  @inline final implicit def FieldKeyToId(v: Value[DataType.FieldKey]) = tag[FieldKeyId](v.valueId)

  trait StepId extends TypeTag
  type Long_StepId = JLong @@ StepId
}
