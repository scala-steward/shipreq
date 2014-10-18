package shipreq.webapp.client.lib

import japgolly.scalajs.react.experiment.{Listenable, OnUnmount}
import japgolly.scalajs.react.ScalazReact._
import scalaz.effect.IO
import scalaz.syntax.bind._
import shipreq.webapp.base.data._
import shipreq.webapp.base.data.delta._
import shipreq.webapp.base.protocol._, Routine._
import shipreq.webapp.client.ClientData
import shipreq.webapp.client.delta.LocalDelta
import shipreq.webapp.client.protocol.{ClientProtocol, FailureIO}
import shipreq.webapp.client.util.ui.table._
import DataImplicits._

class TableIO[T <: DataAndId, C <: Crudable, RD <: CrudableCompanion[C]](implicit t_c_id: T#Id =:= C#Id, I: IdAccessor[T]) {
  final type P = T#Data
  final type D = T#Id
  final type U = C#V
  final type Arb = (Remote[RD], ClientData)

  private def crudIO(arb: Arb, s: SuccessIO, f: FailureIO, a: CrudAction[C]): IO[Unit] =
    ClientProtocol.call(arb._1)(a, arb._2.update(_) >> s.io, f)

  def saveIO(arb: Arb, op: Option[P], u: U, s: SuccessIO, f: FailureIO): IO[Unit] =
    crudIO(arb, s, f, op match {
      case None    => CrudAction.Create[C](u)
      case Some(p) => CrudAction.Update[C](p.id, u)
    })

  def deleteIO(arb: Arb, id: D, a: DeletionAction, f: FailureIO): IO[Unit] =
    crudIO(arb, SuccessIO.nop, f, CrudAction.Delete[C](id, a))

  private def recvExtUpdate[S, Q <: Partition](spec: TableSpec[_, S, D, U, P, _, _], partition: Q)
                                              (implicit ei: Q#Id =:= T#Id, ed: Q#Data =:= T#Data) =
    (d: LocalDelta) => ReactS.mod[S](s1 => {
      val ds = LocalDelta.filter(partition, d)
      val s2 = (s1 /: ds.del)((s, id)   => spec.savedRemoveF(id)(s))
      val s3 = (s2 /: ds.upd)((s, data) => spec.savedSetF(I id data, data)(s))
      s3
    })

  def recvExtUpdates[CP, CB <: OnUnmount, S, Q <: Partition]
      (spec: TableSpec[_, S, D, U, P, _, _], partition: Q, f: CP => Arb)
      (implicit ei: Q#Id =:= T#Id, ed: Q#Data =:= T#Data) =
    Listenable.installS[CP, S, CB, LocalDelta](f(_)._2, recvExtUpdate(spec, partition))
}
