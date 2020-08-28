package shipreq.webapp.base.test

import shipreq.base.util._
import shipreq.webapp.base.data._
import shipreq.webapp.base.event._
import shipreq.webapp.base.test.UnsafeTypes._
import shipreq.webapp.base.test.WebappTestUtil._

/** This is another collection of edge cases.
  *
  * A1 (conflicting in tags) -> A2 (empty)
  *
  * A3 (conflicting in text & tags) -> A4 (empty)
  */
object SampleDerivativeTags5 {

  object Values {
    val a = CustomReqTypeId(1)

    val a1 = GenericReqId(101)
    val a2 = GenericReqId(102)
    val a3 = GenericReqId(103)
    val a4 = GenericReqId(104)

    val zField = CustomField.Tag.Id(1)
    val z      = TagGroupId(10)
    val z1     = ApplicableTagId(11)
    val z2     = ApplicableTagId(12)

    val yField = CustomField.Tag.Id(2)
    val y      = TagGroupId(20)
    val y1     = ApplicableTagId(21)
  }

  import TestEvent._
  import Values._

  val zRules = FieldReqTypeRules.empty
  val zDerivativeTags = DerivativeTags(Enabled, Map.empty)

  val yRules = FieldReqTypeRules.empty
  val yDerivativeTags = DerivativeTags(Enabled, Map.empty)

  val project = applyEventsSuccessfully(Project.empty,
    Event.FieldStaticAdd(StaticField.AllTags),

    Event.CustomReqTypeCreate(a, CustomReqTypeGD("A", "A", Optional, ∅)),

    tagGroupCreate(z, "Z", exclusivity = Exclusive),
    applicableTagCreate(z1, "z1", parent = z),
    applicableTagCreate(z2, "z2", parent = z),
    fieldCustomTagCreate(zField, z, zRules, zDerivativeTags),

    tagGroupCreate(y, "Y"),
    applicableTagCreate(y1, "y1", parent = y),
    fieldCustomTagCreate(yField, y, yRules, yDerivativeTags),

    // A1 (conflicting in tags) -> A2 (empty)
    genericReqCreate(a1, a, tags = Set(z1, z2, y1)),
    genericReqCreate(a2, a, impSrcs = a1),

    // A3 (conflicting in text & tags) -> A4 (empty)
    genericReqCreate(a3, a, titleTagRef = z1, tags = Set(z2, y1)),
    genericReqCreate(a4, a, impSrcs = a3),
  )

  def virtualTagsZ =
    """A-1
      |  + A-2: ∅
      |  + self: z1 (manual)
      |  + self: z2 (manual)
      |  = {z1! z2!}
      |A-2
      |  + self: ∅
      |  = {}
      |A-3
      |  + A-4: ∅
      |  + self: z1 (text)
      |  + self: z2 (manual)
      |  = {z1#! z2!}
      |A-4
      |  + self: ∅
      |  = {}
      |""".stripMargin

  def virtualTagsY =
    """A-1
      |  + A-2: y1 (derived)
      |  + self: y1 (manual)
      |  = {y1}
      |A-2
      |  + A-1: y1 (manual)
      |  + self: ∅
      |  = {y1+}
      |A-3
      |  + A-4: y1 (derived)
      |  + self: y1 (manual)
      |  = {y1}
      |A-4
      |  + A-3: y1 (manual)
      |  + self: ∅
      |  = {y1+}
      |""".stripMargin
}
