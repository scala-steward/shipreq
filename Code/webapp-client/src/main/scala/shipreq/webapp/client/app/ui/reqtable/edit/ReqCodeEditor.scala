package shipreq.webapp.client.app.ui.reqtable.edit

import japgolly.scalajs.react.extra.{ReusableVal, Px}
import scalaz.{\/-, -\/}
import shipreq.base.util.{SetDiff, Util}
import shipreq.webapp.base.UiText
import shipreq.webapp.base.data._
import shipreq.webapp.base.protocol.UpdateContentCmd
import shipreq.webapp.base.text.PlainText
import shipreq.webapp.client.app.ui.{RemoteDataEditor, TextSeqEditor}
import shipreq.webapp.client.lib.ui.TextEditor
import TextSeqEditor._
import Validators.{reqCode => V}
import UpdateContentCmd.{PatchReqCodes, SetReqCodeGroupCode}

object ReqCodeEditor {

  type A = ReqCode.Value

  def mkAutoComplete(validationState: Px[V.VS]): Px[AutoComplete] =
    validationState.map(vs => ReusableVal.byRef(
      AutoComplete.reqCode.prefixes(vs.trie)))

  def mkParser(validationState: Px[V.VS]): Parser[A] = () => {
    val vs = validationState.value()
    V.code.correctAndValidate(vs, _)
  }

  // ===================================================================================================================
  object ForGroup {
    val editor = new TextSeqEditor[A, A]("ReqCode editor", Stream(_), TextEditor.Input)

    def apply(initial : Option[A],
              trie    : Px[ReqCode.Trie],
              setSelf : RemoteDataEditor.SetOpStateFor[String],
              commitFn: A => RemoteDataEditor.OnCommit): RemoteDataEditor.StateFor[String] = {

      def init            = initial.fold("")(PlainText.reqCode)
      val validationState = trie.map(V.VS(_, initial.toSet))
      val autoComplete    = mkAutoComplete(validationState)
      val parser          = mkParser(validationState)

      val validate: Vector[A] => ParseResult[A] =
        _.headOption match {
          case None    => -\/(Some(UiText.FieldNames.reqCode + " cannot be blank.")) // english
          case Some(c) => \/-(c)
        }

      val onCommit = RemoteDataEditor.CommitFilter(commitFn).ignoreIfEqualO(initial)

      RemoteDataEditor.default[String, String](
        init, liveCorrect, setSelf,
        (s, u, abort, commit) =>
          editor.Props(s, u, abort, parser, validate, v => commit(onCommit(v)), autoComplete.value(), cellStyle, cellErrorMsgStyle).apply)
    }

    @inline def liveCorrect(t: String) = V.code.liveCorrect(t)

    def edit(subjectId: ReqCodeId,
             initial  : A,
             trie     : Px[ReqCode.Trie],
             setSelf  : RemoteDataEditor.SetOpStateFor[String],
             commitFn : UpdateContentOnCommit) =
      apply(Some(initial), trie, setSelf, commitFn.cmap[A](SetReqCodeGroupCode(subjectId, _)))
  }

  // ===================================================================================================================
  object ForReqs {
    val lineSplitter = "\\s*[\n\r]\\s*".r.pattern

    val editor = new TextSeqEditor[A, SetDiff[A]]("ReqCode editor",
      s => lineSplitter.split(s.trim).toStream.filter(_.nonEmpty),
      TextEditor.TextArea)

    def edit(subjectId: ReqId,
             initial  : Set[A],
             trie     : Px[ReqCode.Trie],
             setSelf  : RemoteDataEditor.SetOpStateFor[String],
             commitFn : UpdateContentOnCommit): RemoteDataEditor.StateFor[String] = {

      def init            = initial.toVector.map(PlainText.reqCode).sorted mkString "\n"
      val validationState = trie.map(V.VS(_, initial))
      val autoComplete    = mkAutoComplete(validationState)
      val parser          = mkParser(validationState)

      val validate: Vector[A] => ParseResult[SetDiff[A]] =
        as => V.codeSet.correctAndValidateU(as.toSet).map(SetDiff.compare(initial, _))

      val onCommit = commitFn.setDiff[A](PatchReqCodes(subjectId, _))

      RemoteDataEditor.default[String, String](
        init, liveCorrect, setSelf,
        (s, u, abort, commit) =>
          editor.Props(s, u, abort, parser, validate, v => commit(onCommit(v)), autoComplete.value(), cellStyle, cellErrorMsgStyle).apply)
    }

    def liveCorrect(txt: String): String =
      if (txt.trim.isEmpty)
        ""
      else {
        val r = txt.split("[\n\r]").map(V.code.liveCorrect).mkString("\n")
        Util.fixBeforeAfter(txt, r)(_ endsWith "\n", _ + "\n")
      }
  }
}
