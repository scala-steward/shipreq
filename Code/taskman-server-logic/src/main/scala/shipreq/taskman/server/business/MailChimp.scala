package shipreq.taskman.server.business

import shipreq.taskman.api.Types._
import scalaz.NonEmptyList

object MailChimp {

  // ===================================================================================================================
  // Data

  case class ListId(value: String)

  sealed abstract class AccountStatus(val remoteValue: String)
  object AccountStatus {
    
    /** User has never had a ShipReq account. */
    case object Never extends AccountStatus("Never")

    /** User has a ShipReq account. */
    case object Active extends AccountStatus("Active")
  }

  case class Subscription(addr: EmailAddr, name: String, newsletter: Boolean, status: AccountStatus)
  
  // ===================================================================================================================
  // API

  sealed trait API[R]
  object API {

    /** Looks up the ID of a mailing list by name. */
    case class GetListId(name: String) extends API[Option[ListId]]

    case class BatchSubscribe(list: ListId, data: NonEmptyList[Subscription]) extends API[Unit]
  }
}
