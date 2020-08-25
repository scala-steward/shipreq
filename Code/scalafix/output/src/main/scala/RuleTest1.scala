import shipreq.webapp.base.data.CustomField

object RuleTest1 {

  val tag = CustomField.Tag.Id(1)
  tag.some
  tag.some
  tag.some
  tag.some

  def tagFn(i: Int) = CustomField.Tag.Id(i)
  tagFn(1).some
  tagFn(2).some
  tagFn(3).some
  tagFn(4).some

  val text = CustomField.Text.Id(1)
  Option(text)
  Some(text)
  Option.apply(text)
  Some.apply(text)

}
