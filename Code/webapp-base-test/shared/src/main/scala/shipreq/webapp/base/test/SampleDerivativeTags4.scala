package shipreq.webapp.base.test

import shipreq.base.util.Enabled
import shipreq.webapp.base.data._
import shipreq.webapp.base.event._
import shipreq.webapp.base.test.UnsafeTypes._
import shipreq.webapp.base.test.WebappTestUtil._

/** This aims to catch a bunch of edge cases.
  *
  * A1 (default) -> B1 (empty)
  *
  * B2 (empty) -> A2 (default)
  *
  * A3 (default) -> C1 (N/A) -> B3 (empty)
  *
  * B4 (empty) -> C2 (N/A) -> A4 (default)
  *
  * A5 (manual) -> A6 (dead) -> A7 (default)
  *
  * A8 (default)
  *
  * A5 (manual) -> A9 (dead) -> A10 (manual x2) -> A11 (default)
  *
  * B5 (empty) -> B6 (empty)
  *
  * A12 (dead->default) -> B7 (empty)
  *
  * B8 (empty) -> A13 (dead->default)
  *
  * Diamond:
  * B9 (empty) -> A14 (default) -> B10 (empty)
  *            -> A15 (manual)  ->
  *
  * Different roots:
  * A16 (manual) -> A17 (dead) -> A18 (manual) -> B11 (empty)
  *                               A19 (manual) ->
  */
object SampleDerivativeTags4 {

  // field: dead | live
  // rule tag: dead | live

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
    val a12 = GenericReqId(112)
    val a13 = GenericReqId(113)
    val a14 = GenericReqId(114)
    val a15 = GenericReqId(115)
    val a16 = GenericReqId(116)
    val a17 = GenericReqId(117)
    val a18 = GenericReqId(118)
    val a19 = GenericReqId(119)

    val b1 = GenericReqId(201)
    val b2 = GenericReqId(202)
    val b3 = GenericReqId(203)
    val b4 = GenericReqId(204)
    val b5 = GenericReqId(205)
    val b6 = GenericReqId(206)
    val b7 = GenericReqId(207)
    val b8 = GenericReqId(208)
    val b9 = GenericReqId(209)
    val b10 = GenericReqId(210)
    val b11 = GenericReqId(211)

    val c1 = GenericReqId(301)
    val c2 = GenericReqId(302)
    val c3 = GenericReqId(303)

    val zField = CustomField.Tag.Id(1)
    val z      = TagGroupId(10)
    val z1     = ApplicableTagId(11)
    val z2     = ApplicableTagId(12)
    val z3     = ApplicableTagId(13)
    val z4     = ApplicableTagId(14) // (DEAD)
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
    applicableTagCreate(z4, "z4", parent = z),
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

    // A12 (dead->default) -> B7 (empty)
    genericReqCreate(a12, a, tags = z4),
    genericReqCreate(b7, b, impSrcs = a12),

    // B8 (empty) -> A13 (dead->default)
    genericReqCreate(b8, b),
    genericReqCreate(a13, a, impSrcs = b8, tags = z4),

    // Diamond:
    // B9 (empty) -> A14 (default) -> B10 (empty)
    //            -> A15 (manual)  ->
    genericReqCreate(b9, b),
    genericReqCreate(b10, b),
    genericReqCreate(a14, a, impSrcs = b9, impTgts = b10),
    genericReqCreate(a15, a, impSrcs = b9, impTgts = b10, tags = z3),

    // Different roots:
    // A16 (manual) -> A17 (dead) -> A18 (manual) -> B11 (empty)
    //                               A19 (manual) ->
    genericReqCreate(a16, a, tags = z2),
    genericReqCreate(a17, a, tags = z2, impSrcs = a16),
    genericReqCreate(a18, a, tags = z3, impSrcs = a17),
    genericReqCreate(a19, a, tags = z1),
    genericReqCreate(b11, b, impSrcs = Set(a18, a19)),
    reqsDelete(a17),

    // Delete tags
    Event.TagDelete(z4),
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
      |A-12
      |  + B-7: z1 (derived)
      |  + self: z1 (default)
      |  = {z1}
      |    {z1 z4} (ShowDead)
      |A-13
      |  + self: z1 (default)
      |  = {z1}
      |    {z1 z4} (ShowDead)
      |A-14
      |  + B-10: z1 (derived)
      |  + B-10: z3 (derived)
      |  = {z1 z3}
      |A-15
      |  + B-10: z1 (derived)
      |  + B-10: z3 (derived)
      |  + self: z3 (manual)
      |  = {z1 z3}
      |A-16
      |  + self: z2 (manual)
      |  = {z2}
      |A-17
      |  = {}
      |    {z2} (ShowDead)
      |A-18
      |  + B-11: z1 (derived)
      |  + B-11: z3 (derived)
      |  + self: z3 (manual)
      |  = {z1 z3}
      |A-19
      |  + B-11: z1 (derived)
      |  + B-11: z3 (derived)
      |  + self: z1 (manual)
      |  = {z1 z3}
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
      |B-7
      |  + A-12: z1 (default)
      |  + self: ∅
      |  = {z1}
      |B-8
      |  + A-13: z1 (default)
      |  + self: ∅
      |  = {z1}
      |B-9
      |  + A-14: z1 (derived)
      |  + A-14: z3 (derived)
      |  + A-15: z1 (derived)
      |  + A-15: z3 (manual)
      |  + B-10: z1 (derived)
      |  + B-10: z3 (derived)
      |  + self: ∅
      |  = {z1 z3}
      |B-10
      |  + A-14: z1 (default)
      |  + A-15: z3 (manual)
      |  + self: ∅
      |  = {z1 z3}
      |B-11
      |  + A-18: z3 (manual)
      |  + A-19: z1 (manual)
      |  + self: ∅
      |  = {z1 z3}
      |C-1
      |  = {}
      |C-2
      |  = {}
      |""".stripMargin

}
