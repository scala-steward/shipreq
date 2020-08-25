/*
rule = FixShipReq
*/
import shipreq.webapp.base.data.CustomField

object RuleTest1 {

  val tag = CustomField.Tag.Id(1)
  Option(tag)
  Some(tag)
  Option.apply(tag)
  Some.apply(tag)

  def tagFn(i: Int) = CustomField.Tag.Id(i)
  Option(tagFn(1))
  Some(tagFn(2))
  Option.apply(tagFn(3))
  Some.apply(tagFn(4))

  val text = CustomField.Text.Id(1)
  Option(text)
  Some(text)
  Option.apply(text)
  Some.apply(text)

}
