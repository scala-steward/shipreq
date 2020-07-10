package shipreq.webapp.client.project.feature.create

import japgolly.scalajs.react.Reusability
import shipreq.base.util.univeq._
import shipreq.webapp.base.data._
import shipreq.webapp.base.protocol.websocket.{CreateContentCmd, ManualIssueCmd}
import shipreq.webapp.client.project.feature.create.{FieldKey => AnyFieldKey}
import shipreq.webapp.client.project.lib.DataReusability._

sealed abstract class RowKey { self =>
  type FieldKey <: AnyFieldKey
  type Cmd
  type Id
  def foldC[F[_]](f: RowKey.FoldCmd[F]): F[Cmd]

  final type AndId = RowKey.AndId { val row: self.type }

  final def andId(id: Id): AndId = {
    val _id = id
    new RowKey.AndId {
      override val row: self.type = self
      override val id = _id
    }
  }
}

object RowKey {

  case object CodeGroup extends RowKey {
    override type FieldKey = FieldKey.ForCodeGroup
    override type Cmd      = CreateContentCmd
    override type Id       = Unit
    override def foldC[F[_]](f: FoldCmd[F]): F[Cmd] = f.codeGroup(this)
    val andId = super.andId(())
  }

  case object Req extends RowKey {
    override type FieldKey = FieldKey.ForSomeReq
    override type Cmd      = CreateContentCmd
    override type Id       = ReqTypeId
    override def foldC[F[_]](f: FoldCmd[F]): F[Cmd] = f.req(this)
  }

  case object ManualIssue extends RowKey {
    override type FieldKey = FieldKey.ForManualIssue
    override type Cmd      = ManualIssueCmd
    override type Id       = Unit
    override def foldC[F[_]](f: FoldCmd[F]): F[Cmd] = f.manualIssue(this)
    val andId = super.andId(())
  }

  @inline implicit def equality: UnivEq[RowKey] =
    UnivEq.derive

  implicit val reusability: Reusability[RowKey] =
    Reusability.byUnivEq

  /** This shit is required to workaround Scala failing to be check exhaustivity when pattern-matching on Aux */
  final case class FoldCmd[F[_]](codeGroup  : CodeGroup  .type => F[CodeGroup  .Cmd],
                                 req        : Req        .type => F[Req        .Cmd],
                                 manualIssue: ManualIssue.type => F[ManualIssue.Cmd],
                                ) {
    @inline def apply(r: RowKey): F[r.Cmd] = r.foldC(this)
  }

  sealed trait AndId {
    val row: RowKey
    val id: row.Id

    final override def hashCode = row.hashCode * 31 + id.hashCode

    final override def equals(obj: Any): Boolean = obj match {
      case x: AndId => (row == x.row) && (id == x.id)
      case _        => false
    }

    def fold[A](codeGroup  : CodeGroup  .AndId => A,
                req        : Req        .AndId => A,
                manualIssue: ManualIssue.AndId => A): A =
      // I just don't care anymore. Scala 3 will let me do this properly.
      row match {
        case r: CodeGroup  .type => codeGroup  (this.asInstanceOf[r.AndId])
        case r: Req        .type => req        (this.asInstanceOf[r.AndId])
        case r: ManualIssue.type => manualIssue(this.asInstanceOf[r.AndId])
      }

    def foldFK[F[_ <: FieldKey]](codeGroup  : CodeGroup  .AndId => F[CodeGroup  .FieldKey],
                                 req        : Req        .AndId => F[Req        .FieldKey],
                                 manualIssue: ManualIssue.AndId => F[ManualIssue.FieldKey]): F[row.FieldKey] =
      // I just don't care anymore. Scala 3 will let me do this properly.
      (row match {
        case r: CodeGroup  .type => codeGroup  (this.asInstanceOf[r.AndId])
        case r: Req        .type => req        (this.asInstanceOf[r.AndId])
        case r: ManualIssue.type => manualIssue(this.asInstanceOf[r.AndId])
      }).asInstanceOf[F[row.FieldKey]]

  }

  object AndId {
    UnivEq[Req.Id] // prove this holds
    implicit def univEq: UnivEq[AndId] = UnivEq.force
  }
}
