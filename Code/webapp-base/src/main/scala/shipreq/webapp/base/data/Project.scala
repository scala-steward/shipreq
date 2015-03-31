package shipreq.webapp.base.data

import monocle.Lens
import monocle.macros.Lenser
import scalaz.Memo
import shipreq.base.util.ScalaExt._
import shipreq.base.util.{UnivEq, Must}
import DataImplicits._

final case class RevAnd[D](rev: Rev, data: D)

object RevAnd {
  def _data[D] = Lens((_: RevAnd[D]).data)(b => _.copy(data = b))
}

object Project {
  private[this] def l = Lenser[Project]
  val _customIssueTypes = l(_.customIssueTypes)
  val _customReqTypes   = l(_.customReqTypes)
  val _fields           = l(_.fields)
  val _tags             = l(_.tags)
  val _reqs             = l(_.reqs)
  val _reqCodes         = l(_.reqCodes)
  val _reqFieldData     = l(_.reqFieldData)
}

final case class Project(customIssueTypes: RevAnd[CustomIssueTypeIMap],
                         customReqTypes  : RevAnd[CustomReqTypeIMap],
                         fields          : RevAnd[FieldSet],
                         tags            : RevAnd[TagTree],
                         reqs            : RevAnd[Requirements],
                         reqCodes        : RevAnd[ReqCodes],
                         reqFieldData    : RevAnd[ReqFieldData]) {

  import japgolly.nyaya._
  this assertSatisfies DataProp.project.all

  def rev =
    customIssueTypes.rev +
    customReqTypes  .rev +
    fields          .rev +
    tags            .rev +
    reqs            .rev +
    reqCodes        .rev +
    reqFieldData    .rev

  override def toString =
    Stream(customIssueTypes, customReqTypes, fields, tags, reqs, reqCodes, reqFieldData)
      .map("\n    " + _.toString.replace(" -> ", " → "))
      .mkString("Project(", "", "\n)")


  def atag(id: ApplicableTag.Id): Must[ApplicableTag] =
    Must.fromOption(tags.data.get(id), s"No tag found with $id")
      .flatMap(t => t.tag match {
      case a: ApplicableTag => Must.Exists(a)
      case _                => Must.Failed(s"$t is not an ApplicableTag")
    })

  def customField[I <: CustomField.Id, D <: CustomField](id: I)(implicit d: DataIdAux[D, I]): Must[D] =
    fields.data.customFields(id).flatMap(f =>
      Must.fromOption(d.unapplyData(f), s"$id associated with wrong type: $f"))

  def customIssueType(id: CustomIssueType.Id): Must[CustomIssueType] =
    Must.fromOption(customIssueTypes.data.get(id), s"No CustomIssueType found with $id")

  lazy val customTagFields =
    fields.data.customFields.keys.filterT[CustomField.Tag.Id]

  def reqType(i: ReqType.Id): Must[ReqType] =
    i.foldId[Must[ReqType]](Must.Exists(_), customReqTypes.data.apply)

  lazy val reqTypes: Stream[ReqType] =
    (customReqTypes.data.values.toStream: Stream[ReqType]) #:::
      (StaticReqType.valueStream        : Stream[ReqType])

  lazy val tagColumnDistribution = new TagColumnDistribution(this)
}

final class TagColumnDistribution(p: Project) {
  // Traversing the tag tree for used columns is better than calculating the full
  // transitive closure at O(V²) space and O(V²+VE) time.
  private[this] implicit val tagTree = p.tags.data

  type TagIds = Must[Set[Tag.Id]]

  final val tagsForColumn: CustomField.Tag.Id => TagIds =
    Memo.mutableHashMapMemo(fid =>
      p.customField(fid).flatMap(field => tagTree(field.tagId).flatMap(_.transitiveChildren)))

  final val tagsUsedInColumns: TagIds =
    p.customTagFields.foldLeft(Must.Exists(Set.empty): TagIds)((q,c) =>
      for {
        s <- q
        t <- tagsForColumn(c)
      } yield s ++ t
    )

  final val tagsNotUsedInColumns: TagIds =
    tagsUsedInColumns.map(s => tagTree.vstream(_.tag.id).filterNot(s.contains).toSet)
}
