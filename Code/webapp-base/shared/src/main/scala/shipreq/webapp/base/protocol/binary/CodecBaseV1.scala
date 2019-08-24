package shipreq.webapp.base.protocol.binary

import boopickle.DefaultBasic._
import java.time.Instant
import scalaz.Isomorphism.<=>
import scalaz.{-\/, \/, \/-}
import shipreq.base.util._
import shipreq.webapp.base.data._
import shipreq.webapp.base.user._

object CodecBaseV1 {

  def pickleBool[T](iso: Boolean <=> T): Pickler[T] =
    transformPickler(iso.to)(iso.from)

  def pickleDisj[L: Pickler, R: Pickler]: Pickler[L \/ R] =
    new Pickler[L \/ R] {
      private[this] final val KeyR = 0
      private[this] final val KeyL = 1
      override def pickle(a: \/[L, R])(implicit state: PickleState): Unit =
        a match {
          case \/-(r) => state.enc.writeByte(KeyR); state.pickle(r)
          case -\/(l) => state.enc.writeByte(KeyL); state.pickle(l)
        }
      override def unpickle(implicit state: UnpickleState): \/[L, R] =
        state.dec.readByte match {
          case KeyR => \/-(state.unpickle[R])
          case KeyL => -\/(state.unpickle[L])
        }
    }

  def pickleObfuscated[A]: Pickler[Obfuscated[A]] =
    transformPickler(Obfuscated.apply[A])(_.value)

  implicit lazy val picklerInstant: Pickler[Instant] =
    transformPickler(Instant.ofEpochMilli)(_.toEpochMilli)

  implicit lazy val picklerDirection: Pickler[Direction] =
    pickleBool(Forwards)

  implicit lazy val picklerPermission: Pickler[Permission] =
    pickleBool(Allow)

  implicit lazy val picklerValidity: Pickler[Validity] =
    pickleBool(Valid)

  implicit lazy val picklerSecurityToken: Pickler[SecurityToken] =
    transformPickler(SecurityToken.apply)(_.value)

  implicit lazy val picklerSecurityTokenStatus: Pickler[SecurityToken.Status] =
    new Pickler[SecurityToken.Status] {
      override def pickle(a: SecurityToken.Status)(implicit state: PickleState): Unit =
        a match {
          case SecurityToken.Status.Expired => state.enc.writeByte(0)
          case SecurityToken.Status.Invalid => state.enc.writeByte(1)
          case SecurityToken.Status.Valid   => state.enc.writeByte(2)
        }
      override def unpickle(implicit state: UnpickleState): SecurityToken.Status =
        state.dec.readByte match {
          case 0 => SecurityToken.Status.Expired
          case 1 => SecurityToken.Status.Invalid
          case 2 => SecurityToken.Status.Valid
        }
    }

  implicit lazy val picklerErrorMsg: Pickler[ErrorMsg] =
    transformPickler(ErrorMsg.apply)(_.value)

  implicit lazy val picklerEmailAddr: Pickler[EmailAddr] =
    transformPickler(EmailAddr.apply)(_.value)

  implicit lazy val picklerPersonName: Pickler[PersonName] =
    transformPickler(PersonName.apply)(_.value)

  implicit lazy val picklerPlainTextPassword: Pickler[PlainTextPassword] =
    transformPickler(PlainTextPassword.apply)(_.value)

  implicit lazy val picklerUsername: Pickler[Username] =
    transformPickler(Username.apply)(_.value)

  implicit lazy val picklerErrorMsgOrUnit: Pickler[ErrorMsg \/ Unit] =
    pickleDisj

  implicit lazy val picklerUsernameOrEmailAddr: Pickler[Username \/ EmailAddr] =
    pickleDisj

}
