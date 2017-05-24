package shipreq.webapp.client.project.feature.editor2

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.MonocleReact._
import shipreq.base.util._
import shipreq.base.util.univeq._
import shipreq.webapp.base.data._
import shipreq.webapp.base.validation.Simple.Invalidity
import shipreq.webapp.client.base.feature._
import shipreq.webapp.client.base.ui.EditTheme
import shipreq.webapp.client.project.lib.DataReusability._

object Feature {

  type AsyncError = String
  type AsyncState = AsyncFeature.Read.D0[AsyncError]

  /** This is not safe for reusability because the implementation calls `CallbackTo#runNow()`. */
  trait Editor[+Change] {
    def render(p: Permission, a: AsyncState): Option[VdomElement]
    def change(): PotentialChange[Invalidity, Change]
  }

  object Editor {
    implicit def reusability[C]: Reusability[Editor[C]] =
      Reusability.never // ∵ Editor is not safe for reusability
  }

  /** Id used for [[shipreq.webapp.client.project.feature.PreviewFeature]] */
  final case class PreviewId(row: RowKey, cell: FieldKey)
  object PreviewId {
    implicit def equality: UnivEq[PreviewId] = UnivEq.derive
  }

  // ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████

  object State {
    type ForEditor[+C] = Option[Editor[C]]
    type ForRow        = Map[FieldKey, Editor[Any]]
    type ForProject    = Map[RowKey, ForRow]

    def initForProject: ForProject =
      UnivEq.emptyMap

    final case class ForSpecificRow[FK <: FieldKey](state: ForRow) extends AnyVal {
      @inline def get(f: FK): ForEditor[f.Change] =
        f.cast2(state.get(f))
    }
  }

           val reusabilityStateForEditorAny: Reusability[State.ForEditor[Any]] = Reusability.option
  implicit def reusabilityStateForEditor[A]: Reusability[State.ForEditor[A]  ] = reusabilityStateForEditorAny.narrow
  implicit val reusabilityStateForRow      : Reusability[State.ForRow        ] = Reusability.mapSameOrEmpty
  implicit val reusabilityStateForProject  : Reusability[State.ForProject    ] = Reusability.mapSameOrEmpty

  // ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████

  object Read {

    final case class ForEditor[+C](editor: Option[Editor[C]], editability: Permission, async: AsyncState) {
      def render(): Option[VdomElement] =
        editor.flatMap(_.render(editability, async))

      def renderOr[A](a: => A)(implicit ev: VdomElement => A): A =
        render().fold(a)(ev)
    }

    final case class ForRow[R <: RowKey](_editor    : State.ForRow,
                                         editability: Reusable[Editability.ForRow[R]],
                                         async      : AsyncFeature.Read.D1[FieldKey, AsyncError]) {

      def editor: State.ForSpecificRow[R#FieldKey] =
        State.ForSpecificRow(_editor)

      def apply(f: R#FieldKey): ForEditor[f.Change] =
        ForEditor(editor.get(f), editability(f), async(f))
    }

    type ForCodeGroup    = ForRow[RowKey.CodeGroup]
    type ForGenericReq   = ForRow[RowKey.GenericReq]
    type ForUseCase      = ForRow[RowKey.UseCase]
    type ForUseCaseSteps = ForRow[RowKey.UseCaseSteps.type]

    final case class ForProject(state      : State.ForProject,
                                editability: Editability.ForProject,
                                async      : AsyncFeature.Read.D2[RowKey, FieldKey, AsyncError]) {

       private def forRow[R <: RowKey](r: R)(e: Reusable[Editability.ForRow[R]]): ForRow[R] =
         ForRow(state.getOrElse(r, UnivEq.emptyMap), e, async(r))

      def forCodeGroup(id: ReqCodeId): ForCodeGroup =
        forRow(RowKey.CodeGroup(id))(Reusable implicitly editability.forCodeGroups(id))

      def forGenericReq(id: GenericReqId): ForGenericReq =
         forRow(RowKey.GenericReq(id))(Reusable implicitly editability.forReqs(id))

      def forUseCase(id: UseCaseId): ForUseCase =
        forRow(RowKey.UseCase(id))(Reusable implicitly editability.forReqs(id))

       lazy val forUseCaseSteps: ForUseCaseSteps =
         forRow(RowKey.UseCaseSteps)(Reusable implicitly editability.forUseCaseSteps)
    }

             val reusabilityForEditorAny   : Reusability[ForEditor[Any] ] = Reusability.caseClass
    implicit def reusabilityForEditor[A]   : Reusability[ForEditor[A]   ] = reusabilityForEditorAny.narrow
    implicit val reusabilityForCodeGroup   : Reusability[ForCodeGroup   ] = Reusability.caseClass
    implicit val reusabilityForGenericReq  : Reusability[ForGenericReq  ] = Reusability.caseClass
    implicit val reusabilityForUseCase     : Reusability[ForUseCase     ] = Reusability.caseClass
    implicit val reusabilityForUseCaseSteps: Reusability[ForUseCaseSteps] = Reusability.caseClass
    implicit val reusabilityForProject     : Reusability[ForProject     ] = Reusability.caseClass
  }

  // ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████

  object Write {

    final case class ForEditor(newEditor: Reusable[NewEditor], async: AsyncFeature.Write.D0[AsyncError]) {
      def startEdit(state: Read.ForEditor[Any], cb: Callback): Option[Callback] =
        if (state.editability.is(Deny) || state.editor.isDefined)
          None
        else
          Some(newEditor.create(cb))
    }

    sealed trait ForRowInterface[FK <: FieldKey] {
      val async: AsyncFeature.Write.D1[FieldKey, AsyncError]
      def apply(field: FK): ForEditor
    }

    type ForRow[FK <: FieldKey] = Reusable[ForRowInterface[FK]]

    type ForCodeGroup    = ForRow[RowKey.CodeGroup   #FieldKey]
    type ForGenericReq   = ForRow[RowKey.GenericReq  #FieldKey]
    type ForUseCase      = ForRow[RowKey.UseCase     #FieldKey]
    type ForUseCaseSteps = ForRow[RowKey.UseCaseSteps.FieldKey]

    /** Create only one instance; reusability is byRef */
    final case class ForProject(static      : NewEditor.Static,
                                stateAccess : StateAccessPure[State.ForProject],
                                async       : AsyncFeature.Write.D2[RowKey, FieldKey, AsyncError]) {

      private val reuseFromRow  : Reusability[(ForProject, RowKey)          ] = implicitly
      private val reuseFromField: Reusability[(ForProject, RowKey, FieldKey)] = implicitly

      private def rowInterface(row: RowKey): ForRowInterface[row.FieldKey] =
        new ForRowInterface[row.FieldKey] {
          val rowAccess = stateAccess zoomStateL Optics.innerMap(row)
          val rowEditors = NewEditor.forRow(static, row)

          override val async =
            ForProject.this.async(row)

          override def apply(field: row.FieldKey): ForEditor = {
            val asyncCell = async(field)

            def newEditor: NewEditor = {
              val stateAccess: StateAccessPure[State.ForEditor[Any]] = rowAccess zoomStateL Optics.mapValue(field)
              val ctx = NewEditor.Ctx[field.Change](field.cast2(stateAccess), asyncCell)
              rowEditors(field)(ctx)
            }

            val reuseKey = Reusable.explicitly((ForProject.this, row: RowKey, field: FieldKey))(reuseFromField)
            ForEditor(reuseKey.map(_ => newEditor), asyncCell)
          }
        }

      private def forRow(row: RowKey): ForRow[row.FieldKey] = {
        val reuseKey = Reusable.explicitly((this, row: RowKey))(reuseFromRow)
        reuseKey.map(_ => rowInterface(row))
      }

      def forCodeGroup(id: ReqCodeId): ForCodeGroup =
        forRow(RowKey.CodeGroup(id))

      def forGenericReq(id: GenericReqId): ForGenericReq =
        forRow(RowKey.GenericReq(id))

      def forUseCase(id: UseCaseId): ForUseCase =
        forRow(RowKey.UseCase(id))

      lazy val forUseCaseSteps: ForUseCaseSteps =
        forRow(RowKey.UseCaseSteps)

      @inline def toReadWrite(r: Read.ForProject): ReadWrite.ForProject =
        ReadWrite.ForProject(r, this)
    }

    implicit val reusabilityForEditor : Reusability[ForEditor ] = Reusability.caseClass
    implicit val reusabilityForProject: Reusability[ForProject] = Reusability.byRef
  }

  // ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████

  object ReadWrite {

    final case class ForEditor[+C](read: Read.ForEditor[C], write: Write.ForEditor) {
      @inline def render(): Option[VdomElement] =
        read.render()

      @inline def renderOr[A](a: => A)(implicit ev: VdomElement => A): A =
        read.renderOr(a)(ev)

      def themedRenderOr(view: => TagMod): TagMod =
        renderOr(TagMod(EditTheme.editableInline(startEdit), view))

      /** Enable an editor so that the user can edit a portion of data.
        *
        * @return `None` if the underlying data isn't allowed to be edited.
        *         `None` if the editor is already active.
        *         `Some[Callback]` otherwise that, when invoked, will add an editor to state and UI.
        */
      def startEdit: Option[Callback] =
        startEdit(Callback.empty)

      def startEdit(cb: Callback): Option[Callback] =
        write.startEdit(read, cb)

      def asyncFeature = write.async
      def asyncState = read.async
    }

    final case class ForRow[R <: RowKey](read: Read.ForRow[R], write: Write.ForRow[R#FieldKey]) {
      def asyncFeature = write.async
      def asyncState = read.async

      def apply(field: R#FieldKey): ForEditor[field.Change] =
        ForEditor(read(field), write.apply(field))
    }

    type ForCodeGroup    = ForRow[RowKey.CodeGroup]
    type ForGenericReq   = ForRow[RowKey.GenericReq]
    type ForUseCase      = ForRow[RowKey.UseCase]
    type ForUseCaseSteps = ForRow[RowKey.UseCaseSteps.type]

    final case class ForProject(read: Read.ForProject, write: Write.ForProject) {

      def forCodeGroup(id: ReqCodeId): ForCodeGroup =
        ForRow[RowKey.CodeGroup](read.forCodeGroup(id), write.forCodeGroup(id))

      def forGenericReq(id: GenericReqId): ForGenericReq =
        ForRow[RowKey.GenericReq](read.forGenericReq(id), write.forGenericReq(id))

      def forUseCase(id: UseCaseId): ForUseCase =
        ForRow[RowKey.UseCase](read.forUseCase(id), write.forUseCase(id))

      lazy val forUseCaseSteps: ForUseCaseSteps =
        ForRow(read.forUseCaseSteps, write.forUseCaseSteps)

      def asyncFeature = write.async
      def asyncState = read.async
    }

             val reusabilityForEditorAny   : Reusability[ForEditor[Any] ] = Reusability.caseClass
    implicit def reusabilityForEditor[A]   : Reusability[ForEditor[A]   ] = reusabilityForEditorAny.narrow
    implicit val reusabilityForCodeGroup   : Reusability[ForCodeGroup   ] = Reusability.caseClass
    implicit val reusabilityForGenericReq  : Reusability[ForGenericReq  ] = Reusability.caseClass
    implicit val reusabilityForUseCase     : Reusability[ForUseCase     ] = Reusability.caseClass
    implicit val reusabilityForUseCaseSteps: Reusability[ForUseCaseSteps] = Reusability.caseClass
    implicit val reusabilityForProject     : Reusability[ForProject     ] = Reusability.caseClass
  }
}
