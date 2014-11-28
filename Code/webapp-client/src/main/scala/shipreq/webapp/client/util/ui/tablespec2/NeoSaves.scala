package shipreq.webapp.client.util.ui.tablespec2

import japgolly.scalajs.react.ScalazReact._
import shipreq.webapp.base.protocol.DeletionAction
import shipreq.webapp.base.validation2._
import shipreq.webapp.client.protocol.FailureIO
import shipreq.webapp.client.util.ui.table.SuccessIO
import scalaz.{Need, Name}
import scalaz.effect.IO

object NeoSaves {

  type SetRowStatus[S] = RowStatus => ReactST[IO, S, Unit]

  type Retry[S] = Name[ReactST[IO, S, Unit]]

  sealed trait SaveNeed {
    def asOption[A](a: A): Option[A]
  }
  case object SaveNeeded extends SaveNeed {
    override def asOption[A](a: A) = Some(a)
  }
  case object SaveNotNeeded extends SaveNeed {
    override def asOption[A](a: A) = None
  }


  def retryably[A](f: Name[A] => A): A = {
    lazy val a: A = f(Need(a))
    a
  }

  def validateAndSaveAsync[S, T, P, U, I](validator: Validator[T, I, _, U],
                                          st: S => T,
                                          si: S => I,
                                          sp: S => P,
                                          needSave: (U, P) => SaveNeed,
                                          asyncSaveIO: (P, U, SuccessIO, FailureIO) => IO[Unit],
                                          realise: ReactST[IO, S, Unit] => IO[Unit],
                                          setStatus: SetRowStatus[S])
  : ReactST[IO, S, Unit] = {
    val Fix = ReactS.FixT[IO, S]
    type R = Fix.T[Unit]
    retryably[R](retry => {
      def abortSave: R = setStatus(RowStatus.Sync)
      def valid(u: U): R = Fix.liftR { s =>
        val p = sp(s)
        needSave(u, p) match {
          case SaveNotNeeded => abortSave
          case SaveNeeded    => save(p, u) >> setStatus(RowStatus.Locked)
        }
      }
      def save(p: P, u: U): R = {
        val s: SuccessIO = SuccessIO.nop
        val f = failureIO(retry, realise, setStatus)
        Fix.ret(asyncSaveIO(p, u, s, f))
      }
      Fix.liftR(s =>
        validator.correctAndValidate(st(s), si(s))
          .fold(_ => abortSave, valid))
    })
  }

  def validateAndCreateAsync[S, T, U, I](validator: Validator[T, I, _, U],
                                         st: S => T,
                                         si: S => I,
                                         removeNew: ReactS[S, Unit],
                                         asyncCreate: (U, SuccessIO, FailureIO) => IO[Unit],
                                         realise: ReactST[IO, S, Unit] => IO[Unit],
                                         setStatus: SetRowStatus[S])
  : ReactST[IO, S, Unit] = {
    val Fix = ReactS.FixT[IO, S]
    type R = Fix.T[Unit]
    retryably[R](retry => {
      def abortSave: R = setStatus(RowStatus.Sync)
      def valid(u: U): R = Fix.liftR { s =>
        save(u) >> setStatus(RowStatus.Locked)
      }
      def save(u: U): R = {
        val s = SuccessIO(realise(removeNew.liftIO))
        val f = failureIO(retry, realise, setStatus)
        Fix.ret(asyncCreate(u, s, f))
      }
      Fix.liftR(s =>
        validator.correctAndValidate(st(s), si(s))
          .fold(_ => abortSave, valid))
    })
  }

  def deleteAsync[S, D](id: D, da: DeletionAction,
                        asyncDelete: (D, DeletionAction, SuccessIO, FailureIO) => IO[Unit],
                        realise: ReactST[IO, S, Unit] => IO[Unit],
                        setStatus: SetRowStatus[S])
  : ReactST[IO, S, Unit] = {
    val Fix = ReactS.FixT[IO, S]
    type R = Fix.T[Unit]
    retryably[R](retry => {
      val s = SuccessIO.nop
      val f = failureIO(retry, realise, setStatus)
      Fix.ret(asyncDelete(id, da, s, f)) >> setStatus(RowStatus.Locked)
    })
  }

  def failureIO[S](retry: Retry[S],
                   realise: ReactST[IO, S, Unit] => IO[Unit],
                   setStatus: SetRowStatus[S]): FailureIO = {
    def failedStatus = RowStatus.Failed(realise(retry.value))
    FailureIO(realise(setStatus(failedStatus)))
  }
}