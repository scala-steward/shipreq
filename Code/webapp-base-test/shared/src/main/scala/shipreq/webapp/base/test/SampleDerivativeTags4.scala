package shipreq.webapp.base.test

import shipreq.base.util.Enabled
import shipreq.webapp.base.data._
import shipreq.webapp.base.event._
import shipreq.webapp.base.test.UnsafeTypes._
import shipreq.webapp.base.test.WebappTestUtil._

/** This aims to catch a bunch of edge cases.
 *
 *  A1 (default) -> B1 (empty)
 *
 *  B2 (empty) -> A2 (default)
 *
 *  A3 (default) -> C1 (N/A) -> B3 (empty)
 *
 *  B4 (empty) -> C2 (N/A) -> A4 (default)
 *
 *  A5 (manual) -> A6 (dead) -> A7 (default)
 *
 *  A8 (default)
 *
 *  A5 (manual) -> A9 (dead) -> A10 (manual x2) -> A11 (default)
 *
 *  B5 (empty) -> B6 (empty)
 */
object SampleDerivativeTags4 {

  // tag: dead | live
  // field: dead | live
  // rule tag: dead | live
  // graph shape: diamond

  object Values {
    val a = CustomReqTypeId(1)
    val b = CustomReqTypeId(2)
    val c = CustomReqTypeId(3)

    val a1 = GenericReqId(101)
    val a2 = GenericReqId(102)
    val a3 = GenericReqId(103)
    val a4 = GenericReqId(104)
    val a5 = GenericReqId(105)
    val a6 = GenericReqId(106)
    val a7 = GenericReqId(107)
    val a8 = GenericReqId(108)
    val a9 = GenericReqId(109)
    val a10 = GenericReqId(110)
    val a11 = GenericReqId(111)

    val b1 = GenericReqId(201)
    val b2 = GenericReqId(202)
    val b3 = GenericReqId(203)
    val b4 = GenericReqId(204)
    val b5 = GenericReqId(205)
    val b6 = GenericReqId(206)

    val c1 = GenericReqId(301)
    val c2 = GenericReqId(302)
    val c3 = GenericReqId(303)

    val zField = CustomField.Tag.Id(1)
    val z      = TagGroupId(10)
    val z1     = ApplicableTagId(11)
    val z2     = ApplicableTagId(12)
    val z3     = ApplicableTagId(13)
  }

  import TestEvent._
  import Values._

  val zRules =
    FieldReqTypeRules.empty
      .defaultTo(z1)(a)
      .notApplicable(c)

  val zDerivativeTags = DerivativeTags(Enabled, Map(
  ))

  val project = applyEventsSuccessfully(Project.empty,

    Event.CustomReqTypeCreate(a, CustomReqTypeGD("A", "A", Optional, ∅)),
    Event.CustomReqTypeCreate(b, CustomReqTypeGD("B", "B", Optional, ∅)),
    Event.CustomReqTypeCreate(c, CustomReqTypeGD("C", "C", Optional, ∅)),

    tagGroupCreate(z, "Z"),
    applicableTagCreate(z1, "z1", parent = z),
    applicableTagCreate(z2, "z2", parent = z),
    applicableTagCreate(z3, "z3", parent = z),
    fieldCustomTagCreate(zField, z, zRules, zDerivativeTags),

    // a1 (default) -> b1 (empty)
    genericReqCreate(a1, a),
    genericReqCreate(b1, b, impSrcs = a1),

    // b2 (empty) -> a2 (default)
    genericReqCreate(b2, b),
    genericReqCreate(a2, a, impSrcs = b2),

    // a3 (default) -> c1 (N/A) -> b3 (empty)
    genericReqCreate(a3, a),
    genericReqCreate(c1, c, impSrcs = a3),
    genericReqCreate(b3, b, impSrcs = c1),

    // b4 (empty) -> c2 (N/A) -> a4 (default)
    genericReqCreate(b4, b),
    genericReqCreate(c2, c, impSrcs = b4),
    genericReqCreate(a4, a, impSrcs = c2),

    // A5 (manual) -> A6 (dead) -> A7 (default)
    genericReqCreate(a5, a, tags = z2),
    genericReqCreate(a6, a, impSrcs = a5),
    genericReqCreate(a7, a, impSrcs = a6),
    reqsDelete(a6),

    // A8 (default)
    genericReqCreate(a8, a),

    // A5 (manual) -> A9 (dead) -> A10 (manual x2) -> A11 (default)
    genericReqCreate(a9, a, impSrcs = a5),
    genericReqCreate(a10, a, impSrcs = a9, tags = Set(z2, z3)),
    genericReqCreate(a11, a, impSrcs = a10),
    reqsDelete(a9),

    // B5 (empty) -> B6 (empty)
    genericReqCreate(b5, b),
    genericReqCreate(b6, b, impSrcs = b5),
  )

  def virtualTagsZ =
    """A-1
      |  + B-1: z1 (derived)
      |  + self: z1 (default)
      |  = {z1}
      |A-2
      |  + self: z1 (default)
      |  = {z1}
      |A-3
      |  + B-3: z1 (derived)
      |  + self: z1 (default)
      |  = {z1}
      |A-4
      |  + self: z1 (default)
      |  = {z1}
      |A-5
      |  + self: z2 (manual)
      |  = {z2}
      |A-6
      |  = {}
      |A-7
      |  + self: z1 (default)
      |  = {z1}
      |A-8
      |  // + self: z1 (default) // omitted because derivative tags not applied to this isolated req
      |  = {z1}
      |A-9
      |  = {}
      |A-10
      |  + A-11: z2 (derived)
      |  + A-11: z3 (derived)
      |  + self: z2 (manual)
      |  + self: z3 (manual)
      |  = {z2 z3}
      |A-11
      |  + A-10: z2 (manual)
      |  + A-10: z3 (manual)
      |  = {z2 z3}
      |B-1
      |  + A-1: z1 (default)
      |  + self: ∅
      |  = {z1}
      |B-2
      |  + A-2: z1 (default)
      |  + self: ∅
      |  = {z1}
      |B-3
      |  + A-3: z1 (default)
      |  + self: ∅
      |  = {z1}
      |B-4
      |  + A-4: z1 (default)
      |  + self: ∅
      |  = {z1}
      |B-5
      |  + B-6: ∅
      |  + self: ∅
      |  = {}
      |B-6
      |  + self: ∅
      |  = {}
      |C-1
      |  = {}
      |C-2
      |  = {}
      |""".stripMargin

}
