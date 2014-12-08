package shipreq.webapp.client.util.ui.tablespec2

import japgolly.scalajs.react.ScalazReact.ReactS
import scalaz.Scalaz.Id
import utest._
import Editors._
import TestUtil.SampleData_Person._

object EditorTest extends TestSuite {

  /*
  CallbackH
  - pmodB
  - paddST

  EditorI
  - modCallbackH

  Editor
  - modCallbacksA
  - modCallbacks
  - pmodB
  - paddST
  - zoomU

  Editors
  - text{Input,Area}
  - checkbox

  Editor exts
  ===========
  - applyOnEditFinishedK

  (v=Modifier)
  - renderOptionalError
  - applyInputValidation{,L,U}    < validator{,U}

  (a=b, v=Modifier)
  - applyValidator{,U}            < validator{,U}

  (b=fv, c=f)
  - applyRowUpdateAndRevert       < new & saved store
   */

  type TestEditor[B, M[_], S, C] = Editor[CallbackH[B, M, S, C], B, M, S, C, CallbackH[B, M, S, C], CallbackH[B, M, S, C]]
  type TestEditorI[B, M[_], S, C] = EditorI[CallbackH[B, M, S, C], B, M, S, C, CallbackH[B, M, S, C]]

  type SimpleTestEditor[B] = TestEditor[B, Id, Unit, Unit]
  type SimpleTestEditorI[B] = TestEditorI[B, Id, Unit, Unit]

  def TestEditorI[B, M[_], S, C](a: CallbackH[B, M, S, C]): TestEditorI[B, M, S, C] =
    EditorI(a, "", Some(identity[CallbackH[B, M, S, C]]))

  def TestEditor[B, M[_], S, C]: TestEditor[B, M, S, C] =
    Editor(i => { val cbh = i.data; i.editable.fold(cbh)(_(cbh)) })

  def SimpleTestEditorI[B](b: CallbackEvent[B]): SimpleTestEditorI[B] =
    TestEditorI(CallbackH(b, ReactS.ret(()), ()))

  def SimpleTestEditor[B]: SimpleTestEditor[B] =
    TestEditor[B, Id, Unit, Unit]

  def testSimpleEditor[B](e: SimpleTestEditor[B])(i: CallbackEvent[B], expect: CallbackEvent[B]): Unit = {
    val o = e.render(SimpleTestEditorI(i))
    val actual = o.event
    assert(actual == expect)
  }

  def testSimpleEditorB[B](e: SimpleTestEditor[B])(b: B, onChange: B, onEditFinished: B): Unit = {
    testSimpleEditor(e)(OnChange(b), OnChange(onChange))
    testSimpleEditor(e)(OnEditFinished(b), OnEditFinished(onEditFinished))
  }

  override def tests = TestSuite {

    'applyLiveCorrection {
      val e = SimpleTestEditor[String].applyLiveCorrection(usernameV)
      testSimpleEditorB(e)("HeHe ", onChange = "hehe ", onEditFinished = "HeHe ")
    }

    'applyPostCorrection {
      val e = SimpleTestEditor[String].applyPostCorrection(usernameVU.cp)(_ => ())
      testSimpleEditorB(e)("hehe ", onChange = "hehe ", onEditFinished = "hehe")
    }

    'applyPostCorrectionU {
      val e = SimpleTestEditor[String].applyPostCorrectionU(usernameVU.cp)
      testSimpleEditorB(e)("hehe ", onChange = "hehe ", onEditFinished = "hehe")
    }

    'combo {
      val e = SimpleTestEditor[String].applyLiveCorrection(usernameV).applyPostCorrectionU(usernameVU.cp)
      testSimpleEditorB(e)("HeHe ", onChange = "hehe ", onEditFinished = "hehe")
    }
  }
}
