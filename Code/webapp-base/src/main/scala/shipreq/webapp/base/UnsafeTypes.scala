package shipreq.webapp.base


/**
 * THIS SHOULD ONLY BE USED FOR TESTING.
 */
object UnsafeTypes {
  import shipreq.webapp.base.data._
  import shipreq.webapp.base.data.delta._

  implicit def autoMnemonic(s: String) = ReqType.Mnemonic(s)
  implicit def autoRefKey(s: String) = RefKey(s)

  implicit def autoCustomReqTypeId(i: Int) = CustomReqType.Id(i)
  implicit def autoCustomIssueTypeId(i: Int) = CustomIssueType.Id(i)
  implicit def autoTagId(i: Int) = Tag.Id(i)
  implicit def autoRev(i: Int) = Rev(i)

  implicit def autoSome[A](a: A) = Some(a)

  implicit def tagTreeTree(t: TagTree) = t.mapValues(_.children)
}
