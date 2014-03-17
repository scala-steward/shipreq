package shipreq.taskman.api

import shipreq.base.util.TypeTags

// TODO Merge with webapp's types

trait Types extends TypeTags {
  import Types._
  type UserId = JLong @@ IsUserId
  type EmailAddr = String @@ IsEmailAddr
}

object Types extends Types {
  sealed trait IsUserId extends TypeTag[JLong]
  sealed trait IsEmailAddr extends TypeTag[String]
}
