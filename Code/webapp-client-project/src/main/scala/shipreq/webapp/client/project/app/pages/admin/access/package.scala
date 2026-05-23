package shipreq.webapp.client.project.app.pages.admin

import shipreq.webapp.base.data.UserId

package object access {

  type AsyncKey = Option[UserId]

  object AsyncKey {
    @inline def newUser: AsyncKey =
      None

    @inline def apply(id: UserId): AsyncKey =
      Some(id)
  }

}
