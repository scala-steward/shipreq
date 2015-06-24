package shipreq.webapp.base.data

import japgolly.nyaya.util.Multimap
import monocle.Lens
import monocle.macros.Lenses
import scalaz.{Equal, -\/, \/-}
import shipreq.base.util.ScalaExt._
import shipreq.base.util.{UnivEq, Monoidish, Must}
import shipreq.webapp.base.text.{Atom, Text}
import shipreq.webapp.base.util.{TransitiveClosure, ShowSize}
import shipreq.webapp.base.util.TypeclassDerivation._
import DataImplicits._
import ReqFieldData.{Implications, ImplicationsU}

object Project {
  implicit def equality: Equal[Project] = deriveEqual

  val customIssueTypes: Lens[Project, RevAnd[CustomIssueTypeIMap]] = config ^|-> ProjectConfig.customIssueTypes
  val customReqTypes  : Lens[Project, RevAnd[CustomReqTypeIMap]  ] = config ^|-> ProjectConfig.customReqTypes
  val fields          : Lens[Project, RevAnd[FieldSet]           ] = config ^|-> ProjectConfig.fields
  val tags            : Lens[Project, RevAnd[TagTree]            ] = config ^|-> ProjectConfig.tags
}

@Lenses
final case class Project(config      : ProjectConfig,
                         reqs        : RevAnd[Requirements],
                         reqCodes    : RevAnd[ReqCodes],
                         reqFieldData: RevAnd[ReqFieldData]) {

  def contentRev: Rev =
    reqs            .rev +
    reqCodes        .rev +
    reqFieldData    .rev

  val rev: Rev =
    config.rev + contentRev

  override def toString =
    s"Project(config: ${config.rev}, content: $contentRev)"
    //ShowSize(this).showTree

  def allRichText: Stream[(String, Stream[Text.AnyOptional])] =
    Stream(
      ("Generic Req descs", reqs.data.reqs.values.filterT[GenericReq].map(_.title)),
      ("Text fields", reqFieldData.data.text.values.toStream.flatMap(_.values.toStream).map(_.whole)))

  def countAtoms: ShowSize.Node = {
    val counted =
      allRichText.map {
        case (name, txts) =>
          ShowSize.Node.countChildren(name, txts.flatMap(_.toStream))(Atom.Type.of(_).toString)
      }
    ShowSize.Node.sum("Atoms", counted: _*)
  }

  /**
   * Transitive closure of implications going source → target.
   *
   * Note: Dead reqs are included (reflexively and when direct implications) but are not followed.
   */
  lazy val implicationSrcToTgtTC: TransitiveClosure[ReqId] =
    implicationTransitiveClosure(_.srcToTgt)

  /**
   * Transitive closure of implications going target → source.
   *
   * Note: Dead reqs are included (reflexively and when direct implications) but are not followed.
   */
  lazy val implicationTgtToSrcTC: TransitiveClosure[ReqId] =
    implicationTransitiveClosure(_.tgtToSrc)

  private def implicationTransitiveClosure(f: Implications => ImplicationsU): TransitiveClosure[ReqId] =
    ReqFieldData.implicationTransitiveClosure(
      reqs.data.reqs.keys,
      reqs.data.dead,
      f(reqFieldData.data.implications))

  lazy val tagsInTextR  : Multimap[ReqId,     Set,  ApplicableTagId] = Multimap(scanAllLiveTextR(Text.findTags(_),   Text.findTags))
  lazy val issuesInTextR: Multimap[ReqId,     Vector, Atom.AnyIssue] = Multimap(scanAllLiveTextR(Text.findIssues(_), Text.findIssues))
  lazy val issuesInTextG: Multimap[ReqCodeId, Vector, Atom.AnyIssue] = Multimap(scanAllLiveTextG(Text.findIssues(_)))

  private def scanAllLiveTextR[R](f1: Text.GenericReqTitle.OptionalText => R,
                                  f2: (Text.CustomTextField.OptionalText, R) => R): Map[ReqId, R] = {
    val textData   = reqFieldData.data.text
    val textFields = config.liveCustomTextFields.map(_.id)

    def searchCustomTextFields(id: ReqId, into: R): R =
      textFields.foldLeft(into)((q, f) =>
        textData.get(f).flatMap(_ get id).fold(q)(txt => f2(txt.whole, q)))

    reqs.data.reqs.mapValues {
      case r: GenericReq => searchCustomTextFields(r.id, f1(r.title))
    }
  }

  private def scanAllLiveTextG[R](f: Text.ReqCodeGroupTitle.OptionalText => R): Map[ReqCodeId, R] =
    reqCodes.data.activeGroups
      .foldLeft(Map.empty[ReqCodeId, R])((m, g) =>
        m.updated(g.id, f(g.group.title)))

  // Finally, ensure validity
  import japgolly.nyaya._
  this assertSatisfies DataProp.project.all
}
