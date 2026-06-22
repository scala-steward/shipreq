package shipreq.webapp.member.project.data

import shipreq.base.util.Impossible
import shipreq.webapp.member.project.data.FieldReqTypeRules.Resolution

final class FieldSetRules(val imp   : CustomField.Implication.Id => Resolution.ForImpField,
                          val num   : CustomField.Number     .Id => Resolution.ForNumField,
                          val tag   : CustomField.Tag        .Id => Resolution.ForTagField,
                          val text  : CustomField.Text       .Id => Resolution.ForTextField,
                          val static: StaticField                => Resolution[Impossible],
                         ) {

  def apply(id: FieldId): Resolution[Any] =
    id match {
      case i: CustomField.Implication.Id => imp(i)
      case i: CustomField.Number.Id      => num(i)
      case i: CustomField.Tag.Id         => tag(i)
      case i: CustomField.Text.Id        => text(i)
      case f: StaticField                => static(f)
    }

  override def hashCode =
    (((imp.hashCode + tag.hashCode * 31) * 31 + text.hashCode) * 31 + static.hashCode) * 31 + num.hashCode

  override def equals(obj: Any): Boolean =
    obj match {
      case f: FieldSetRules => (this eq f) || ((imp eq f.imp) && (num eq f.num) && (tag eq f.tag) && (text eq f.text) && (static eq f.static))
      case _                => false
    }
}

object FieldSetRules {

  def apply(imp   : CustomField.Implication.Id => Resolution.ForImpField,
            num   : CustomField.Number     .Id => Resolution.ForNumField,
            tag   : CustomField.Tag        .Id => Resolution.ForTagField,
            text  : CustomField.Text       .Id => Resolution.ForTextField,
            static: StaticField                => Resolution[Impossible],
           ): FieldSetRules =
    new FieldSetRules(imp, num, tag, text, static)

  def const(r: Resolution[Nothing]): FieldSetRules = {
    val f = (_: Any) => r
    apply(f, f, f, f, f)
  }

  val optional: FieldSetRules =
    const(Resolution.Optional)

  implicit def univEq: UnivEq[FieldSetRules] = UnivEq.force
}
