package shipreq.webapp.base.hash2

import japgolly.univeq.UnivEq
import shipreq.webapp.base.data.Project

final case class HashSchemeId(value: Char) extends AnyVal {
  def plus(n: Int): HashSchemeId =
    HashSchemeId((value.toInt + n).toChar)
}

object HashSchemeId {

  implicit def univEq: UnivEq[HashSchemeId] =
    UnivEq.derive

  val zero: HashSchemeId =
    apply('a')
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

final case class HashScheme(hashFns: Map[HashScope, HashFn[Project]]) {

  // dataBefore: Option[A] should be "'hashes with this Scheme' before" for cache-ability
//    def check(dataBefore: Option[A], dataNow: A, recs: Map[S, Option[Int]]): Map[S, HashFailure] =
//      // warn about irrelavent scopes with values
//      // compare hashes for each relevant scope
//      // Hash=None == force pass
//      // missing entry for scope means no change to scope from before
//      ???
}

