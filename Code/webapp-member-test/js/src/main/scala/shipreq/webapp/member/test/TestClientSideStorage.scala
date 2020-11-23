package shipreq.webapp.member.test

import japgolly.scalajs.react.{AsyncCallback, CallbackTo}
import shipreq.webapp.member.project.event.EventOrd
import shipreq.webapp.member.project.library.ProjectLibrary
import shipreq.webapp.member.project.storage.ClientSideStorage

final class TestClientSideStorage extends ClientSideStorage.ReadWrite {
  var available = true

  private var projectStore = Option.empty[ProjectLibrary]

  override val isAvailable: CallbackTo[Boolean] =
    CallbackTo(available)

  override def saveProjectLibrary(pl: ProjectLibrary): AsyncCallback[Unit] =
    AsyncCallback.delay {
      if (pl > projectStore.flatMap(_.ord)) {
        projectStore = Some(pl.withoutFutureEvents)
      }
    }

  override def getProjectLibrary: AsyncCallback[Option[ProjectLibrary]] =
    AsyncCallback.delay(projectStore)

  override def getProjectLibraryOrd: AsyncCallback[Option[EventOrd.Latest]] =
    getProjectLibrary.map(_.flatMap(_.ord))

  def projectLibrary(): Option[ProjectLibrary] =
    projectStore

  def ordAsInt(): Int =
    projectStore.flatMap(_.ord).fold(0)(_.value)
}

object TestClientSideStorage {

  def apply(): TestClientSideStorage =
    new TestClientSideStorage
}