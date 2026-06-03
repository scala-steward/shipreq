package shipreq.webapp.member.project.storage

import boopickle.Pickler
import japgolly.scalajs.react.{AsyncCallback, CallbackTo}
import shipreq.webapp.base.data.ProjectCreator
import shipreq.webapp.base.protocol.Version
import shipreq.webapp.base.protocol.binary.SafePickler
import shipreq.webapp.base.protocol.webstorage._
import shipreq.webapp.member.project.event.EventOrd
import shipreq.webapp.member.project.library.{CacheJs, ProjectLibrary}
import shipreq.webapp.member.protocol.binary.{BinaryFormat, Compression, Encryption}
import shipreq.webapp.member.protocol.webstorage.WebStorageCodecs

/** Uses WebStorage as the client-side storage mechanism.
 *
 * LocalStorage is extremely limited. As such we only save the latest project.
 *
 * Format:
 *
 * {{{
 *   prefix:project = ord:<encrypted (latest) project>
 * }}}
 *
 */
final class WebStorage(ws        : AbstractWebStorage,
                       ctx       : ClientSideStorage.Context,
                       encryption: Encryption) extends ClientSideStorage.ReadWrite {

  @inline private implicit def _ws = ws

  override protected val creator =
    ctx.creator

  override def describe =
    "WebStorage"

  override val isAvailable: CallbackTo[Boolean] =
    CallbackTo.pure(true)

  private val protocols =
    new WebStorage.Protocols(ctx.creator, encryption)

  private def newKey(name: String): AbstractWebStorage.Key =
    AbstractWebStorage.Key(s"${ctx.namespace}:$name")

  private val key = newKey("project")

  private val projectLibrary: WebStorageKey.Async[ProjectLibrary] =
    protocols.projectLibrary.valueCodec.webStorageKey(key)

  override def clear: AsyncCallback[Unit] =
    ws.removeItem(key).asAsyncCallback

  override val getProjectLibrary: AsyncCallback[Option[ProjectLibrary]] =
    projectLibrary.get

  override val getProjectLibraryOrd: AsyncCallback[Option[EventOrd.Latest]] =
    ws.getItem(projectLibrary.key)
      .map(_.map(_.value.takeWhile(_ != ':').toInt |> EventOrd.Latest))
      .asAsyncCallback

  override def saveProjectLibrary(pl: ProjectLibrary): AsyncCallback[Unit] =
    for {
      saved <- getProjectLibraryOrd
      _     <- projectLibrary.set(pl).when_(pl > saved)
    } yield ()
}

object WebStorage {

  private[WebStorage] final class Protocols(creator: ProjectCreator, encryption: Encryption) {

    object projectLibrary {

      val ver = Version.fromInts(1, 1) // Bump this when any of following imports change
      import shipreq.webapp.member.project.protocol.binary.v2.Rev1.picklerProject

      val cache = CacheJs(creator)

      def picklerProjectLibrary(v: Version.Minor): Pickler[ProjectLibrary] =
        picklerProject(v)
          .xmap(ProjectLibrary.init(creator, _, cache))(_.latest)

      implicit val pickler: SafePickler[ProjectLibrary] =
        SafePickler.of(ver, picklerProjectLibrary).withMagicNumbers(0x7F19E183, 0x3365F95A)

      val format: BinaryFormat[ProjectLibrary] =
        BinaryFormat.versioned(
          BinaryFormat.pickleCompressEncrypt(Compression.maxNoHeaders, encryption))

      val valueCodec: ValueCodec.Async[ProjectLibrary] =
        WebStorageCodecs.binaryFormat(format)
          .xmapRaw( // Prefix binary string with "<ord>:"
            afterEncode = (p, v) => v.mod(p.latest.history.ordAsInt.toString + ":" + _),
            beforeDecode = _.mod(v => v.drop(v.indexOf(':') + 1))
          )
    }

  } // Protocols

}
