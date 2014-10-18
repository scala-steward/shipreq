package shipreq.webapp.feature.validation

import scala.xml.{NodeSeq, Text}
import scalaz.NonEmptyList
import shipreq.webapp.lib.ScalazSubset._
import shipreq.webapp.base.validation.GenericVFailureRenderer
import shipreq.webapp.base.validation.VFailure.ErrorMsg

object VFailureHtmlRenderer extends GenericVFailureRenderer {
  override type I = NodeSeq
  override type O = NodeSeq
  override def iMonoid = scalaz.std.nodeseq.nodeSeqInstance
  override protected def finalise(i: I) = i

  override protected def renderTopLevelN(is: List[I]) =
    <ul>{is foldMap renderTopLevelItem}</ul>

  private def renderTopLevelItem(n: I): I =
    <li>{n}</li>

  override protected def renderFieldError1(name: String, e: ErrorMsg) =
    Text(s"$name $e")

  override protected def renderFieldErrorN(name: String, es: NonEmptyList[ErrorMsg]) =
    Text(name) ++ <ul>{es foldMap renderSubFailItem}</ul>

  private def renderSubFailItem(e: ErrorMsg): I =
    <li>{render(e)}</li>

  override protected def render(e: ErrorMsg) =
    Text(e)
}