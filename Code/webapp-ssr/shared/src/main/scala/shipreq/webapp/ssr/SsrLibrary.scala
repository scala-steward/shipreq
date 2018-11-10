package shipreq.webapp.ssr

import shipreq.base.util.Permission
import shipreq.webapp.base.user.Username

trait SsrLibrary[I, O] {
  def apply(i: I): O
}

object SsrLibrary {
}

sealed trait SsrComponent
object SsrComponent {
  final case class Public(publicRegistration: Permission, loggedInUser: Option[Username]) extends SsrComponent
}
