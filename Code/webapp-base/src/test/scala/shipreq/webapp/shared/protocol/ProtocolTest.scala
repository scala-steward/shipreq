package shipreq.webapp.shared.protocol

import scalaz.Leibniz.===
import utest._
import upickle._
import shipreq.webapp.shared.data._
import Routine.Remote, Routines._
import DeletionAction._

object ProtocolTest extends TestSuite {

  def kitR[R <: Routine.Desc](r: R) = {
    import r.{ri, wi, ro, wo}
    new KitIO[r.I, r.O]
  }

  def kitEP[I](ep: JsEntryPoint[I, _]) = {
    import ep.{ri, wi}
    new KitIO[I, Unit]
  }

  class KitIO[I: Reader : Writer, O: Reader : Writer] {
    private def c(code: String, m: Any) = s"\033[${code}m$m\033[0m"

    def testI(is: I*): Unit =
      is.foreach(testA(_, (a, j) => s"  C ⇒ ${c("36", a)}\n    ⇒ ${c("34;1", j)} ⇒ S"))

    def testO(os: O*): Unit =
      os.foreach(testA(_, (a, j) => s"  S ⇒ ${c("36", a)}\n    ⇒ ${c("34;1", j)} ⇒ C"))

    def testIO(is: I*)(implicit ev: I === O): Unit = {
      testI(is: _*)
      testO(ev.subst(is): _*)
    }

    def testA[A: Reader : Writer](a: A, f: (String, String) => String) = {
      val j = write(a)
      println(f(a.toString, j))
      val b = read[A](j)
      assert(b == a)
    }
  }

  object TestData {
    // TODO this is bullshit, need properties :(
    // TODO or at least use applicative and give each field multiple values
    object customReqType {
      def id = CustomReqType.Id(123654)
      def mn = ReqType.Mnemonic("BR")
      def mn2 = ReqType.Mnemonic("X")
      def mn3 = ReqType.Mnemonic("Y")
      val s = "hehe"
      def c1 = CustomReqType(id, mn, Set(mn2, mn3), s, ImplicationRequired, Dead)
      def nv = (mn, s, ImplicationRequired)
    }
  }

  override def tests = TestSuite {

    'Routines {
      'CustomReqTypeOps {
        import TestData.customReqType._
        val C = Routines.CustomReqTypeCrud
        val kit = kitR(C)
        'create  - kit.testI( C.create(nv) )
        'update  - kit.testI( C.update(id, nv) )
        'softDel - kit.testI( C.delete(id, SoftDel) )
        'hardDel - kit.testI( C.delete(id, HardDel) )
        'restore - kit.testI( C.delete(id, Restore) )
      }
    }

    'JsEntryPoints {
      import JsEntryPoint._

      'reactExamples {
        kitEP(reactExamples).testI(ForCfgReqType(Remote("x", Routines.CustomReqTypeCrud)))
      }
    }

    'Δ {
      import delta._
      val r1 = Rev(3)
      val r2 = Rev(4)
      def test1[P <: Partition]: P => RemoteDeltaG = {
        case p@ Partition.CustomReqTypes =>
          import TestData.customReqType._
          RemoteDeltaG(p, r1, r2)(List(id), List(c1))
      }

      def test2(dg: RemoteDeltaG) = {
        val d = List(dg)
        kitR(Routines.CustomReqTypeCrud).testO(d)
      }
      def test[P <: Partition](p: P) = test2(test1(p))

      'CustomReqTypes - test(Partition.CustomReqTypes)
    }
  }
}
