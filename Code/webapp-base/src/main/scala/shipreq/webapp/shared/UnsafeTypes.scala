package shipreq.webapp.shared


/**
 * THIS SHOULD ONLY BE USED FOR TESTING.
 */
object UnsafeTypes {
  import shipreq.webapp.shared.data._
  import shipreq.webapp.shared.data.delta._

  implicit def autoMnemonic(s: String) = ReqType.Mnemonic(s)
  implicit def autoRefKey(s: String) = RefKey(s)

  implicit def autoCustomReqTypeId(i: Int) = CustomReqType.Id(i)
  implicit def autoCustomIncmpTypeId(i: Int) = CustomIncmpType.Id(i)
  implicit def autoRev(i: Int) = Rev(i)

  implicit def autoSome[A](a: A) = Some(a)

}
