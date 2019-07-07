package shipreq.webapp.client.project.app.issues

import japgolly.scalajs.react.Reusability
import japgolly.univeq.UnivEq
import shipreq.base.util.{Direction, Forwards}
import shipreq.webapp.base.data.CustomFieldId
import shipreq.webapp.base.lib.BaseReusability._

sealed abstract class Column(final val key: String)

object Column {

  case object IssueCategory                       extends Column("ic")
  case object IssueClass                          extends Column("cl")
  case object FieldName                           extends Column("fn")
  case object FieldEditor                         extends Column("fe")
  case object Actions                             extends Column("a")

  case object Pubid                               extends Column("p")
  case object Code                                extends Column("c")
  case object Title                               extends Column("t")
  case object ReqType                             extends Column("rt")
  case object Tags                                extends Column("t")
  final case class Implications(dir: Direction)   extends Column(if (dir is Forwards) "IF" else "IB")
  final case class CustomField(id: CustomFieldId) extends Column(id.foldId(_.name, _.value.toString))

  implicit def univEq: UnivEq[Column] = UnivEq.derive

  implicit def reusability: Reusability[Column] = Reusability.byRefOrUnivEq
}