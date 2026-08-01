package shipreq.webapp.client.project.app.pages.content.issues

import shipreq.base.util.{Backwards, Direction}
import shipreq.webapp.client.project.feature.EditorFeature.{FieldKey => EditorFieldKey}
import shipreq.webapp.client.project.feature.RenderFeature.{FieldKey => RenderFieldKey}
import shipreq.webapp.member.project.data._

final case class IssueField[+RFK <: RenderFieldKey, +EFK <: EditorFieldKey](key: RFK \/ EFK, desc: Option[String])

object IssueField {
  import DataImplicits._

  implicit def univEq: UnivEq[IssueField[RenderFieldKey, EditorFieldKey]] = UnivEq.derive

  val CodeGroupTitle  = IssueField(\/-(EditorFieldKey.CodeGroupTitle) , Some(SpecialBuiltInField.Title.name))
  val GenericReqTitle = IssueField(\/-(EditorFieldKey.GenericReqTitle), Some(SpecialBuiltInField.Title.name))
  val UseCaseTitle    = IssueField(\/-(EditorFieldKey.UseCaseTitle)   , Some(SpecialBuiltInField.Title.name))
  val OtherTags       = IssueField(\/-(EditorFieldKey.OtherTags)      , Some(StaticField.OtherTags.name))

  def customField(id: CustomFieldId)(implicit cfg: ProjectConfig): IssueField[RenderFieldKey.ForAllReqs, EditorFieldKey.ForAllReqs] =
    customField(cfg.fields.customFields.need(id))

  def customField(cf: CustomField)(implicit cfg: ProjectConfig): IssueField[RenderFieldKey.ForAllReqs, EditorFieldKey.ForAllReqs] =
    cf match {
      case f: CustomField.Text        => customField(f)
      case f: CustomField.Tag         => customField(f)
      case f: CustomField.Implication => customField(f)
      case f: CustomField.Number      => customField(f)
      case f: CustomField.Formula     => customField(f)
    }

  def customField(f: CustomField.Formula): IssueField[RenderFieldKey.CustomFormulaField, Nothing] =
    IssueField(-\/(RenderFieldKey.CustomFormulaField(f.id)), Some(f.name))

  def customField(id: CustomField.Number.Id)(implicit cfg: ProjectConfig): IssueField[Nothing, EditorFieldKey.CustomNumberField] =
    customField(cfg.fields.custom(id))

  def customField(id: CustomField.Text.Id)(implicit cfg: ProjectConfig): IssueField[Nothing, EditorFieldKey.CustomTextField] =
    customField(cfg.fields.custom(id))

  def customField(id: CustomField.Tag.Id)(implicit cfg: ProjectConfig): IssueField[Nothing, EditorFieldKey.CustomFieldTags] =
    customField(cfg.fields.custom(id))

  def customField(id: CustomField.Implication.Id)(implicit cfg: ProjectConfig): IssueField[Nothing, EditorFieldKey.Implications] =
    customField(cfg.fields.custom(id))

  def customField(f: CustomField.Number): IssueField[Nothing, EditorFieldKey.CustomNumberField] =
    IssueField(\/-(EditorFieldKey.CustomNumberField(f.id)), Some(f.name))

  def customField(f: CustomField.Text): IssueField[Nothing, EditorFieldKey.CustomTextField] =
    IssueField(\/-(EditorFieldKey.CustomTextField(f.id)), Some(f.name))

  def customField(f: CustomField.Tag)(implicit cfg: ProjectConfig): IssueField[Nothing, EditorFieldKey.CustomFieldTags] =
    IssueField(\/-(EditorFieldKey.CustomFieldTags(f.id)), Some(f.name(cfg.tags.tree)))

  def customField(f: CustomField.Implication)(implicit cfg: ProjectConfig): IssueField[Nothing, EditorFieldKey.Implications] =
    IssueField(\/-(EditorFieldKey.Implications(-\/(f.id))), Some(f.name(cfg.reqTypes)))

  val implications: Direction => IssueField[Nothing, EditorFieldKey.Implications] =
    Direction.memo(d => IssueField(\/-(EditorFieldKey.Implications(\/-(d))), Some(SpecialBuiltInField.implication(d).name)))

  def impliedBy = implications(Backwards)

  def useCaseStep(id: UseCaseStepId, p: Project): IssueField[Nothing, EditorFieldKey.UseCaseStep] = {
    val focus = p.content.reqs.useCases.focusStep(id)
    useCaseStep(focus)
  }

  def useCaseStep(focus: UseCaseStep.Focus): IssueField[Nothing, EditorFieldKey.UseCaseStep] = {
    val label = focus.field.stepLabel(focus.uc.pubid.pos, focus.ploc, UseCaseStepLabelFmt.`N.m`)
    val desc  = "UC step " + label
    IssueField(\/-(EditorFieldKey.UseCaseStep(focus.id)), Some(desc))
  }

  def manual(issue: ManualIssue): IssueField[RenderFieldKey.ManualIssue, EditorFieldKey.ManualIssue] =
    IssueField(\/-(EditorFieldKey.ManualIssue(issue.id)), None)
}
