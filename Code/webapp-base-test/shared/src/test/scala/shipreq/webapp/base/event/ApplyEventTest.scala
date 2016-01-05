package shipreq.webapp.base.event

import nyaya.gen._
import nyaya.prop._
import nyaya.test.PropTest._
import scala.annotation.tailrec
import scala.collection.immutable.NumericRange
import scalaz.{-\/, \/-}
import utest._
import shipreq.base.test.BaseTestUtil._
import shipreq.webapp.base.data._
import shipreq.webapp.base.event.ApplyEvent.LogicVer
import shipreq.webapp.base.hash.{HashScheme, HashRec}
import shipreq.webapp.base.hash.HashTestUtil.hashSchemes
import shipreq.base.util.univEqOps

object ApplyEventTest extends TestSuite {

  /*
//  case class State(reqCodes: Int, events: Vector[Event])

  case class State(reqCodes: Int)

  object RandomEventStream extends StateGen.Fix[State] {

    val initialState = State(0, Vector.empty)

    // different scopes
    // different hashScheme
    // different logicVer

    def blah(s: State): Gen[State] = {
      val rcn = s.reqCodes + 1
      val rcv = NonEmptyVector one ReqCode.Node(rcn.toString)
      val e = CreateReqCodeGroup(ReqCodeId(rcn), ReqCodeGroupGD.Code(rcv))
      val s2 = s.copy(reqCodes = rcn, events = s.events :+ e)
      Gen pure s2
    }

    //    genS()


  }
*/

  /*
  def changePoints(changes: Int, seqLength: Int): Gen[Vector[Int]] =
    if (changes == 0)
      Gen pure Vector.empty
    else if (changes < 0)
      sys.error(s"Can't have a negative number of changes ($changes).")
    else if (seqLength < (changes + 1))
      sys.error(s"For $changes changes, a minimum seqLength of ${changes + 1} is required.")
    else {
      val tmp = seqLength - 1 - changes
      Gen { ctx =>
        val v = Vector.newBuilder[Int]
        var prev = 0
        for (change <- 1 to changes) {
          prev = Gen.chooseInt(prev + 1, tmp + change) run ctx
          v += prev
        }
        v.result()
      }
    }

  def changePoints2(maxChanges: Int, seqLength: Int): Gen[Vector[Int]] =
    if (maxChanges < 1 || seqLength < 1)
      Gen pure Vector.empty
    else {
      val max = seqLength - 1
      Gen { ctx =>
        val v = Vector.newBuilder[Int]
        var prev = 0
        for (change <- 1 to maxChanges) {
          prev = Gen.chooseInt(prev, max) run ctx
          v += prev
        }
        v.result()
      }
    }

  class ChangePoints(changes: Vector[Int]) {

    def forChange(changeIndex: Int): Int =
      changes(changeIndex)

    def atPos(pos: Int): Int = {
      require(pos >= 0, "pos must be ≥ 0")
      @tailrec def go(change: Int): Int =
        if (change < 0)
      {
        val start = changes(change)
        if (start)
      }
      go(0)
    }
  }
  */

  val OldLogicVer1 = LogicVer(1)
  val OldLogicVer2 = LogicVer(2)
  val LogicVers = OldLogicVer1 +: OldLogicVer2 +: LogicVer.all

  override def tests = TestSuite {
    'applyVerified {

      val p1 = Project.empty
      val e1 = DeleteStaticField(StaticField.StepGraph)

      val p2 = ApplyEvent.untrusted.apply1(e1)(p1) match {
        case \/-(p) => p
        case -\/(x) => fail(s"Init failed: $x")
      }

      val hrs = HashRec.changes(p1, p2)
      assert(hrs.nonEmpty)
      val ve = VerifiedEvent(e1, hrs)

      'pass {
        ApplyEvent.untrusted.applyVerified(List(ve))(p1) match {
          case \/-(p) => assertEq(p, p2)
          case -\/(e) => fail(s"applyVerified failed: $e")
        }
      }

      'fail {
        val vef = ve.copy(hashRecs = ve.hashRecs.map(r => HashRec(r.scope, r.logicVer, r.scheme)(r.hash.map(_ + 1))))
        ApplyEvent.untrusted.applyVerified(List(vef))(p1) match {
          case \/-(p) => fail(s"applyVerified passed when it shouldn't have.")
          case -\/(e) => e
        }
      }
    }
    'stuff {

      def log(s: => String): Unit = println(s)
      //def log(s: => String): Unit = ()

      val genLogicVerSeq   = Gen.orderedSeq(LogicVers  .whole, 0, dropElems = true, emptyResult = false)
      val genHashSchemeSeq = Gen.orderedSeq(hashSchemes.whole, 0, dropElems = true, emptyResult = false)

      def gen(initProject: Project = Project.empty) =
        Gen { ctx =>

          var lvs = genLogicVerSeq run ctx
          var hss = genHashSchemeSeq run ctx
          val lvCount = lvs.length
          val hsCount = hss.length

          var p = initProject
          var stats = EventStats.empty
          var ves = Vector.empty[VerifiedEvent]

          var lv = LogicVer('z')
          var hs: HashScheme = null
          def advanceLogicVer(): Unit = {lv = lvs.head; lvs = lvs.tail}
          def advanceHashScheme(): Unit = {hs = hss.head; hss = hss.tail}
          advanceLogicVer()
          advanceHashScheme()

          def addEvent(): Unit = {
            val ((s2, p2), e) = GenSuccEvent(p).applicableEventS(stats)(EventStats.observeFn) run ctx

            // Mutate project to simulate old application-logic
            val p3 = if (lv ==* LogicVer.Current) p2 else
              Project.idCeilings.modify(IdCeilings.customField.modify(_ + lv.value.toInt))(p2)

            val hr = HashRec.__changes(HashRec.defaultHashScopes, lv, hs, p, p3)
            ves :+= VerifiedEvent(e, hr)
            stats = s2
            p = p3
          }

          while (lvs.nonEmpty || hss.nonEmpty) {
            if (ctx.nextBit())
              addEvent()
            else {
              if (lvs.nonEmpty && ctx.nextBit()) advanceLogicVer()
              if (hss.nonEmpty && ctx.nextBit()) advanceHashScheme()
            }
          }

          for (_ <- 0 until ctx.nextInt3())
            addEvent()

          if (ves.isEmpty)
            addEvent()

//          println(s"${ves.size} @ $lvCount/$hsCount")
//          println(stats.report)
//          println()
//          log(
//            (s"Generated: ${ves.length} events." +:
//              ves.map(v => s"  - ${EventStats name v.event} - ${v.hashRecs.map(r => s"L${r.logicVer.value.toInt} H${r.scheme.id.value.toInt}").mkString("[",", ", "]")}"))
//              .mkString("\n") + "\n")

          ves
      }

      val g = gen()

      val prop = Prop.atom[VerifiedEvents]("", ves => {
        val r = ApplyEvent.trusted.applyVerified(ves)(Project.empty)
        if (r.isLeft)
          log(
            ( s"Generated: ${ves.length} events." +:
              ves.map(v => s"  - ${EventStats name v.event} - ${v.hashRecs.map(r => s"L${r.logicVer.value.toInt} H${r.scheme.id.value.toInt} ${r.scope}").mkString("[",", ", "]")}"))
              .mkString("\n") + "\n")

        r.swap.toOption
      })

      //g.mustSatisfy(prop)(defaultPropSettings.setSeed(84).setSampleSize(10).setDebug)
      g.bugHunt(944)(prop)

      // TODO should also wipe some hashrecs to demonstrate manual intervention

    }
  }
}
