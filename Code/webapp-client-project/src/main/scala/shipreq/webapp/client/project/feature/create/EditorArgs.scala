package shipreq.webapp.client.project.feature.create

import japgolly.scalajs.react._
import shipreq.webapp.base.data._
import shipreq.webapp.base.feature.{EditControlsFeature, PreviewFeature}
import shipreq.webapp.base.text.{PlainText, TextSearch}
import shipreq.webapp.client.project.feature.create.Feature.PreviewId
import shipreq.webapp.client.project.widgets.ProjectWidgets

object EditorArgs {

  def empty(f: FieldKey)
           (previewRW     : PreviewFeature.ReadWrite.Composite[PreviewId],
            project       : Project,
            plainText     : PlainText.ForProject.AnyCtx,
            textSearch    : TextSearch,
            projectWidgets: ProjectWidgets.NoCtx): f.Args =
    apply(f)(
      previewRW      = previewRW,
      project        = project,
      plainText      = plainText,
      textSearch     = textSearch,
      projectWidgets = projectWidgets,
      abort          = None,
      abortVerb      = "",
      autoFocus      = false,
      commit         = None,
      commitVerb     = "",
      extraControls  = EditControlsFeature.ExtraControls.empty,
    )

  def apply(f: FieldKey)
           (previewRW     : PreviewFeature.ReadWrite.Composite[PreviewId],
            project       : Project,
            plainText     : PlainText.ForProject.AnyCtx,
            textSearch    : TextSearch,
            projectWidgets: ProjectWidgets.NoCtx,
            abort         : Option[Callback],
            abortVerb     : String,
            autoFocus     : Boolean,
            commit        : Option[Callback],
            commitVerb    : String,
            extraControls : EditControlsFeature.ExtraControls): f.Args = {

    val forReqCodeEditor = (_: Any) => ForReqCodeEditor(
      trie           = project.content.reqCodes.trie,
      abort          = abort,
      abortVerb      = abortVerb,
      autoFocus      = autoFocus,
      commit         = commit,
      commitVerb     = commitVerb,
      extraControls  = extraControls,
    )

    val forImplicationEditor = (_: Any) => ForImplicationEditor(
      project        = project,
      plainText      = plainText,
      textSearch     = textSearch,
      abort          = abort,
      abortVerb      = abortVerb,
      autoFocus      = autoFocus,
      commit         = commit,
      commitVerb     = commitVerb,
      extraControls  = extraControls,
    )

    val forTagEditor = (_: Any) => ForTagEditor(
      project        = project,
      abort          = abort,
      abortVerb      = abortVerb,
      autoFocus      = autoFocus,
      commit         = commit,
      commitVerb     = commitVerb,
      extraControls  = extraControls,
    )

    val forTextEditor = (_: Any) => ForTextEditor(
      previewRW      = previewRW,
      project        = project,
      textSearch     = textSearch,
      projectWidgets = projectWidgets,
      abort          = abort,
      abortVerb      = abortVerb,
      autoFocus      = autoFocus,
      commit         = commit,
      commitVerb     = commitVerb,
      extraControls  = extraControls,
    )

    type Args[A, V] = A

    val fold = FieldKey.Fold[Args](
      allTags         = forTagEditor,
      code            = forReqCodeEditor,
      codes           = forReqCodeEditor,
      customFieldTags = forTagEditor,
      customTextField = forTextEditor,
      implications    = forImplicationEditor,
      manualIssue     = forTextEditor,
      otherTags       = forTagEditor,
      titleCG         = forTextEditor,
      titleGR         = forTextEditor,
      titleUC         = forTextEditor,
    )

    f.fold(fold)
  }

  final case class ForReqCodeEditor(trie          : ReqCode.Trie,
                                    abort         : Option[Callback],
                                    abortVerb     : String,
                                    autoFocus     : Boolean,
                                    commit        : Option[Callback],
                                    commitVerb    : String,
                                    extraControls : EditControlsFeature.ExtraControls) {

    def commitFn: Option[Any => Callback] =
      commit.map(c => _ => c)
  }

  final case class ForImplicationEditor(project       : Project,
                                        plainText     : PlainText.ForProject.AnyCtx,
                                        textSearch    : TextSearch,
                                        abort         : Option[Callback],
                                        abortVerb     : String,
                                        autoFocus     : Boolean,
                                        commit        : Option[Callback],
                                        commitVerb    : String,
                                        extraControls : EditControlsFeature.ExtraControls) {

    def commitFn: Option[Any => Callback] =
      commit.map(c => _ => c)
  }

  final case class ForTagEditor(project       : Project,
                                abort         : Option[Callback],
                                abortVerb     : String,
                                autoFocus     : Boolean,
                                commit        : Option[Callback],
                                commitVerb    : String,
                                extraControls : EditControlsFeature.ExtraControls) {

    def commitFn: Option[Any => Callback] =
      commit.map(c => _ => c)
  }

  final case class ForTextEditor(previewRW     : PreviewFeature.ReadWrite.Composite[PreviewId],
                                 project       : Project,
                                 textSearch    : TextSearch,
                                 projectWidgets: ProjectWidgets.NoCtx,
                                 abort         : Option[Callback],
                                 abortVerb     : String,
                                 autoFocus     : Boolean,
                                 commit        : Option[Callback],
                                 commitVerb    : String,
                                 extraControls : EditControlsFeature.ExtraControls) {

    def commitFn: Option[Any => Callback] =
      commit.map(c => _ => c)
  }

  object ForTextEditor {
    def basic(previewRW     : PreviewFeature.ReadWrite.Composite[PreviewId],
              project       : Project,
              textSearch    : TextSearch,
              projectWidgets: ProjectWidgets.NoCtx,
              abort         : Option[Callback],
              commit        : Option[Callback]): ForTextEditor =
      apply(
        previewRW      = previewRW,
        project        = project,
        textSearch     = textSearch,
        projectWidgets = projectWidgets,
        abort          = abort,
        abortVerb      = EditControlsFeature.defaultAbortVerb,
        autoFocus      = true,
        commit         = commit,
        commitVerb     = EditControlsFeature.defaultCommitVerb,
        extraControls  = EditControlsFeature.ExtraControls.empty)

    def empty(previewRW     : PreviewFeature.ReadWrite.Composite[PreviewId],
              project       : Project,
              textSearch    : TextSearch,
              projectWidgets: ProjectWidgets.NoCtx): ForTextEditor =
      basic(
        previewRW      = previewRW,
        project        = project,
        textSearch     = textSearch,
        projectWidgets = projectWidgets,
        abort          = None,
        commit         = None)

  }
}