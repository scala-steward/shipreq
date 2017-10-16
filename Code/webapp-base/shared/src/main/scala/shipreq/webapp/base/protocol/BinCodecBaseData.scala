package shipreq.webapp.base.protocol

import boopickle._
import java.time.Instant
import java.util.regex.Pattern
import shipreq.base.util._
import shipreq.webapp.base.data._
import BinCodecGeneric._
import BoopickleMacros._

object BinCodecBaseData {

  implicit def pickleObfuscated[T]: Pickler[Obfuscated[T]] =
    pickleCaseClass

  implicit lazy val pickleInstant: Pickler[Instant] =
    xmap(Instant.ofEpochMilli)(_.toEpochMilli)

  implicit lazy val picklePattern: Pickler[Pattern] =
    new Pickler[Pattern] {
      override def pickle(p: Pattern)(implicit state: PickleState): Unit = {
        state.enc.writeString(p.pattern)
        state.enc.writeInt(p.flags)
      }
      override def unpickle(implicit state: UnpickleState) = {
        val p = state.dec.readString
        val f = state.dec.readInt
        Pattern.compile(p, f)
      }
    }

  implicit lazy val pickleVectorTreeLoc: Pickler[VectorTree.Location] =
    pickleNEV

  implicit lazy val pickleVectorTreeParentLoc: Pickler[VectorTree.ParentLocation] =
    implicitly[Pickler[Vector[Int]]].imap(VectorTree.ParentLocation.isoVector)

  implicit lazy val pickleDirection: Pickler[Direction] =
    pickleBool(Forwards)

  implicit lazy val picklePermission: Pickler[Permission] =
    pickleBool(Allow)

  implicit lazy val pickleValidity: Pickler[Validity] =
    pickleBool(Valid)

  implicit lazy val picklerSecurityToken: Pickler[SecurityToken] =
    pickleCaseClass[SecurityToken]

  implicit lazy val picklerSecurityTokenStatus: Pickler[SecurityToken.Status] =
    derivePickler[SecurityToken.Status]

  implicit val picklerErrorMsg: Pickler[ErrorMsg] =
    BoopickleMacros.pickleCaseClass
}
