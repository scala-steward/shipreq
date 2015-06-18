package shipreq.webapp.base.protocol

import shipreq.webapp.base.data._
import shipreq.webapp.base.delta.{Partition, PPI}

object CustomIssueTypeProtocol {
  type Values = (HashRefKey, Option[String])
  val ppi = PPI.imap(Partition.CustomIssueTypes)(Project.customIssueTypes)
}

object CustomReqTypeProtocol {
  type Values = (ReqType.Mnemonic, String, ImplicationRequired)
  val ppi = PPI.imap(Partition.CustomReqTypes)(Project.customReqTypes)
}
