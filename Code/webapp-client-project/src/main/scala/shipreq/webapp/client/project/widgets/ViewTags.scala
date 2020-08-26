package shipreq.webapp.client.project.widgets

import japgolly.microlibs.utils.Memo
import japgolly.scalajs.react.Reusable
import japgolly.scalajs.react.vdom.html_<^._
import scala.collection.mutable
import scalacss.ScalaCssReact._
import shipreq.base.util._
import shipreq.webapp.base.data._
import shipreq.webapp.base.data.derivation.VirtualProjectTags
import shipreq.webapp.base.data.derivation.VirtualProjectTags.TagProvenance
import shipreq.webapp.base.lib.ClientUtil
import shipreq.webapp.base.text.Grammar
import shipreq.webapp.base.ui.semantic.Icon
import shipreq.webapp.client.project.app.Style.{widgets => *}

final class ViewTags(project: Project) {
  import ViewTags._

  private[this] val vtags     = project.virtualTags
  private[this] val tagConfig = project.config.tags

  type Out = VdomTag

  def apply(t: ApplicableTag.OrId): Dsl =
    new Dsl(t)

  final class Dsl(t: ApplicableTag.OrId) {

    private var ts: TagSettings = TagSettings.default

    def withCustomName(name: String): Dsl = {
      ts = ts.copy(customName = Some(name))
      this
    }

    def withValidity(validity: Validity): Dsl = {
      ts = ts.copy(validity = validity)
      this
    }

    def render(implicit ds: DisplaySettings): Out =
      ViewTags.this.render(t, ts)
  }

  def render(t: ApplicableTag.OrId, ts: TagSettings = TagSettings.default)(implicit ds: DisplaySettings): Out = {
    val id = t.id

    val useCache = (
      id.value >= 0 // TagConfig creates a fake ApplicableTag with id of -1 to render new tags before they're saved
      && ts.customName.isEmpty // Custom names are not cached
    )

    if (useCache)
      cache(ds)(ts.validity)(t)
    else
      _render(t.tag(tagConfig), ds, ts)
  }

  private[this] val cache: DisplaySettings => Validity => ApplicableTag.OrId => Out =
    Util.memoWithMapVar { ds =>
      Validity.memo { validity =>
        val ts = TagSettings(None, validity)
        val cache = new mutable.HashMap[ApplicableTagId, Out]()
        t =>
          if (t.isId) {
            val id = t.id
            cache.getOrElseUpdate(id, {
              val tag = tagConfig.needApplicableTag(id)
              _render(tag, ds, ts)
            })
          } else {
            val tag = t.unsafeAsTag
            cache.getOrElseUpdate(tag.id, _render(tag, ds, ts))
          }
      }
    }

  private def _render(tag: ApplicableTag, ds: DisplaySettings, ts: TagSettings): Out = {
    val name      = ts.customName.getOrElse(tag.name)
    val live      = tag.live
    val tagInText = ds.contextualise ==* Contextualise
    val styleArgs = (live, ts.validity)


    val hoverText: String =
      if (ds.hoverText == HoverText.Omit)
        ""
      else {
        var txt =
          if (name.compareToIgnoreCase(tag.key.value) == 0)
            ""
          else
            Grammar.hashRefKey.prefix + tag.key.value
        for (d <- tag.desc) {
          if (txt.nonEmpty)
            txt += "\n\n"
          txt += d
        }
        txt
      }

    val hoverTextVdom =
      TagMod.when(hoverText.nonEmpty)(^.title := hoverText)

    if (tagInText) {
      // ---------------------------------------------------------------------------------------------------------------
      // Render as inline text

      <.span(
        *.tagInText(styleArgs),
        hoverTextVdom,
        Grammar.hashRefKey.prefix, name)

    } else {
      // ---------------------------------------------------------------------------------------------------------------
      // Render a little pill

      val colour: TagMod =
        // Do nothing if Dead because of the *.tag style and tagLabelColour which results in .ui.label.grey for dead tags
        TagMod.when(live is Live) {
          val c = tag.colour.getOrElse(Colour.tagDefault)
          TagMod(
            ^.backgroundColor := c.value,
            ^.borderColor     := c.value,
            ^.color           := c.foreground.value,
          )
        }

      <.span(
        *.tag(styleArgs),
        colour,
        hoverTextVdom,
        name)
    }
  }

  /** Basic here means the tag has context-specific decorations. So...
    * - no provenance icon
    * - no derivation explanation
    */
  def basicVectorById(ids: Vector[ApplicableTagId], validity: ApplicableTagId => Validity = Valid.always)
                     (implicit ds: DisplaySettings): VdomTag = {
    val c = cache(ds)
    ClientUtil.renderVector(ids, ClientUtil.sepSpace) { id =>
      val v = validity(id)
      c(v)(id)
    }
  }

  val forReq: FilterDead => ReqId => ForReq[Out] =
    FilterDead.memo { fd =>
      Memo { reqId =>
        _forReq(reqId, fd)
      }
    }

  private val neverDeadFn: (VirtualProjectTags.ResultsLiveDead => Set[ApplicableTagId]) => ApplicableTagId => Boolean = {
    val f = (_: Any) => false
    _ => f
  }

  private def _forReq(reqId: ReqId, fd: FilterDead): ForReq[Out] = {
    type Fn         = ApplicableTagId => VdomTag
    implicit def ds = DisplaySettings.tag
    val invalidTags = project.invalidTagsPerReq(reqId)
    val basicCache  = cache(ds)
    val basicTag    = (id: ApplicableTagId) => basicCache(Invalid when invalidTags.contains(id))(id)
    val provenance  = this.vtags(reqId).provenance
    val vtags       = this.vtags(reqId, fd)

    val makeIsDeadFn: (VirtualProjectTags.ResultsLiveDead => Set[ApplicableTagId]) => ApplicableTagId => Boolean =
      if (fd is HideDead)
        neverDeadFn
      else
        f => {
          val allIds = f(vtags)
          val liveIds = f(this.vtags(reqId, HideDead))
          val deadIds = allIds -- liveIds
          deadIds.contains
        }

    def isForegroundBlack(t: ApplicableTagId, isDead: Boolean): Boolean =
      if (isDead)
        false // foreground is always white for dead tags
      else {
        val tag = tagConfig.needApplicableTag(t)
        val colour = tag.colour.getOrElse(Colour.tagDefault)
        colour.foreground eq Colour.black
      }

    val renderInAll: Fn = {
      val isDeadFn = makeIsDeadFn(_.allSet)
      Util.memoWithMapVar { t =>
        val isDead         = isDeadFn(t)
        lazy val fgIsBlack = isForegroundBlack(t, isDead)
        var default        = false
        var derivedIn      = Set.empty[CustomField.Tag.Id]

        for {
          of <- vtags.tagSources(t)
          f  <- of
        } provenance(f)(t) match {
          case TagProvenance.Derived => derivedIn += f
          case TagProvenance.Default => default = true
          case TagProvenance.Manual  =>
        }

        basicTag(t)(
          TagMod.when(default)(provenanceIcon(TagProvenance.Default, fgIsBlack)),
          TagMod.when(derivedIn.nonEmpty)(provenanceIcon(TagProvenance.Derived, fgIsBlack)),
          TagMod.when(isDead)(tagIconDead))
      }
    }

    val renderInOther: Fn = {
      val isDeadFn = makeIsDeadFn(_.otherSet)
      t => {
        val isDead = isDeadFn(t)
        basicTag(t)(
          TagMod.when(isDead)(tagIconDead))
      }
    }

    val renderInField: CustomField.Tag.Id => Fn =
      f => {
        val fp = provenance(f)
        val isDeadFn = makeIsDeadFn(_.fieldSet(f))
        t => {
          val p = fp(t)
          val isDead = isDeadFn(t)
          basicTag(t)(
            provenanceIcon(p, isForegroundBlack(t, isDead)),
            TagMod.when(isDead)(tagIconDead))
        }
      }

    new ForReq[Out] {
      override val all     = renderInAll
      override val other   = renderInOther
      override val inField = renderInField

      override def vector(ids: Vector[ApplicableTagId], render: ApplicableTagId => Out): Out =
        ClientUtil.renderVector(ids, ClientUtil.sepSpace)(render)
    }
  }

  val forPlainTextViewReqCache: Reusable[FilterDead => ReqId => ViewTags.ForReq[String]] =
    Reusable.byRef(this).withValue(_ => _ => plainTextForReq)

  val plainTextForReq: ForReq[String] = {
    val renderOne: ApplicableTagId => String =
      project.config.tags.needApplicableTag(_).name

    new ForReq[String] {
      override def all     = renderOne
      override def other   = renderOne
      override val inField = _ => renderOne

      override def vector(ids: Vector[ApplicableTagId], render: ApplicableTagId => String) =
        ids.iterator.map(render).mkString(" ")
    }
  }
}

object ViewTags {

  def apply(project: Project): ViewTags =
    new ViewTags(project)

  final case class DisplaySettings private[DisplaySettings](hoverText    : HoverText,
                                                            contextualise: Contextualise) {
    override val hashCode =
      hoverText.hashCode * 31 + contextualise.hashCode

    override def equals(o: Any): Boolean =
      o.isInstanceOf[AnyRef] && {
        val a = o.asInstanceOf[AnyRef]
        (a eq this) || (a.hashCode == hashCode)
      }
  }

  object DisplaySettings {
    implicit def univEq: UnivEq[DisplaySettings] = UnivEq.derive

    val inText    = apply(HoverText.Show, Contextualise)
    val tag       = apply(HoverText.Show, Plain)
    val tagNoDesc = apply(HoverText.Omit, Plain)
  }

  sealed trait HoverText

  object HoverText {
    case object Omit extends HoverText
    case object Show extends HoverText

    implicit def univEq: UnivEq[HoverText] = UnivEq.derive
  }

  final case class TagSettings(customName: Option[String],
                               validity  : Validity)

  object TagSettings {
    val default = apply(None, Valid)
  }

  trait ForReq[A] {
    def inField: CustomField.Tag.Id => ApplicableTagId => A
    def other  : ApplicableTagId => A
    def all    : ApplicableTagId => A

    def vector(ids: Vector[ApplicableTagId], render: ApplicableTagId => A): A
  }

  private val tagIconDead    = Icon.Trash.tag(*.tagIconDead)
  private val tagIconDefault = Memo.bool(b => Icon.Sliders.tag(*.tagIconDefault(b)))
  private val tagIconDerived = Memo.bool(b => Icon.Sitemap.tag(*.tagIconDerived(b)))

  private[ViewTags] def provenanceIcon(p: TagProvenance, fgIsBlack: => Boolean): TagMod =
    p match {
      case TagProvenance.Manual  => TagMod.empty
      case TagProvenance.Default => tagIconDefault(fgIsBlack)
      case TagProvenance.Derived => tagIconDerived(fgIsBlack)
    }
}
