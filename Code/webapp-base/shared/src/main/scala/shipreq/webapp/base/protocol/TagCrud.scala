package shipreq.webapp.base.protocol

import scalaz.\&/
import shipreq.base.util.UnivEq
import shipreq.webapp.base.data._
import shipreq.webapp.base.util.TypeclassDerivation._
import boopickle._, BoopickleMacros._, BinCodecGeneric._, BinCodecData._
import TagInTree.Relations

object TagCrud {

  sealed trait Values

  final case class TagGroupValues(name: String,
                                  mutexChildren: MutexChildren,
                                  desc: Option[String]) extends Values

  final case class ApplicableTagValues(name: String,
                                       key: HashRefKey,
                                       desc: Option[String]) extends Values

  implicit lazy val equalValues: UnivEq[Values] = {import AutoDerive._; deriveUnivEq}

  import AutoDerive._
  implicit val tagGroupValueEquality     : UnivEq[TagGroupValues]      = deriveUnivEq
  implicit val applicableTagValueEquality: UnivEq[ApplicableTagValues] = deriveUnivEq

  implicit val pickleTagPovRelations    : Pickler[Relations]           = pickleCaseClass
  implicit val pickleTagGroupValues     : Pickler[TagGroupValues]      = pickleCaseClass
  implicit val pickleApplicableTagValues: Pickler[ApplicableTagValues] = pickleCaseClass
  implicit val pickleTagValues          : Pickler[Values]              = pickleADT

  object Fn extends Crudable.CAux[TagId, Values \&/ Relations]
}