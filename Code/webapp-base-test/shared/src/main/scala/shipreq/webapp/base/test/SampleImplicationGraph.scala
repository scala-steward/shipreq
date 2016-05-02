package shipreq.webapp.base.test

import shipreq.webapp.base.data._
import ProjectDsl._
import UnsafeTypes._

/**
 * mf1 → fr1 ̉↘
 *             fr2 → fr3
 *     ↗ mf2 ↗
 * br1             ↗ mf4 → fr6
 *     ↘ br2 → mf3
 *                 ↘ fr4 → fr5 → mf5
 */
object SampleImplicationGraph {

  val mf1: GenericReqId = 11
  val mf2: GenericReqId = 12
  val mf3: GenericReqId = 13
  val mf4: GenericReqId = 14
  val mf5: GenericReqId = 15

  val br1: GenericReqId = 21
  val br2: GenericReqId = 22

  val fr1: GenericReqId = 31
  val fr2: GenericReqId = 32
  val fr3: GenericReqId = 33
  val fr4: GenericReqId = 34
  val fr5: GenericReqId = 35
  val fr6: GenericReqId = 36

  object IdsAsInts {
    val mf1: Int = SampleImplicationGraph.mf1.value
    val mf2: Int = SampleImplicationGraph.mf2.value
    val mf3: Int = SampleImplicationGraph.mf3.value
    val mf4: Int = SampleImplicationGraph.mf4.value
    val mf5: Int = SampleImplicationGraph.mf5.value

    val br1: Int = SampleImplicationGraph.br1.value
    val br2: Int = SampleImplicationGraph.br2.value

    val fr1: Int = SampleImplicationGraph.fr1.value
    val fr2: Int = SampleImplicationGraph.fr2.value
    val fr3: Int = SampleImplicationGraph.fr3.value
    val fr4: Int = SampleImplicationGraph.fr4.value
    val fr5: Int = SampleImplicationGraph.fr5.value
    val fr6: Int = SampleImplicationGraph.fr6.value
  }

  lazy val projectDsl = {
    def t(i: GenericReqId, rt: CustomReqTypeId, tgts: ReqId*) = GReq(id = i, reqType = rt, impTgts = tgts.toSet)
    import SampleProject.Values._

    ( t(mf1, mf, fr1)
    + t(mf2, mf, fr2)
    + t(mf3, mf, mf4, fr4)
    + t(mf4, mf, fr6)
    + t(mf5, mf)

    + t(br1, br, mf2, br2)
    + t(br2, br, mf3)

    + t(fr1, fr, fr2)
    + t(fr2, fr, fr3)
    + t(fr3, fr)
    + t(fr4, fr, fr5)
    + t(fr5, fr, mf5)
    + t(fr6, fr)
    )
  }

  lazy val project =
    projectDsl ! SampleProject.project
}
