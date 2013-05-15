package com.beardedlogic.usecase.lib

import scala.collection.immutable.TreeSet
import net.liftweb.actor.LiftActor
import net.liftweb.common.Logger
import net.liftweb.http.js.{JsCmds, JsCmd}
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._

import JsExt._
import field.CourseFields.StepChangeMsg
import msg.{MessageCentre, PushToClient}

object MutableTextWithStepRefs {

  /**
   * Regex that matches a reference to a step.
   *
   * Note: The braces are required for the match to complete but are not part of the matched text.
   */
  val StepRefRegex = """(?<=\[)\s*?[A-Za-z0-9][A-Za-z0-9\s.]+?(?=\])""".r

  /**
   * Whitespace are dots is removed. This regex matches a dot with optional whitespace on either side.
   */
  val DotWithWhitespaceRegex = """\s*\.\s*""".r

  @inline def MakeRef(ref: String) = "[" + ref + "]"

  val UnRefRegex = "^\\[(.+)\\]$".r

  @inline def UnRef(ref: String) = ref match {
    case UnRefRegex(label) => label
    case _                 => ref
  }

  @inline def IsInvalidStepLabel(label: String) = label.indexOf('.') == -1

  @inline def InvalidStepRef(label: String) = label + "?"

  @inline def NormaliseStepRef(label: String) = DotWithWhitespaceRegex.replaceAllIn(label.trim, ".")

  val DeletedRef = MakeRef("DELETED")

  val ArrowRegex = "-->|→".r
  val ArrowSplitRegex = s"^(.*)(?:$ArrowRegex)(.+)$$".r
  val ArrowBad = "->"
  val ArrowGood = "→"
}

/**
 * Encapsulates a String to provide the following functionality:
 * <ul>
 * <li>References to steps in the text are validated, invalid references are transformed to make the invalidity
 * obvious.</li>
 * <li>References to steps are updated when their labels change, and the updated text is pushed back to the client.</li>
 * </ul>
 *
 * Make sure you call <code>init()</code> before use.
 */
class MutableTextWithStepRefs(val msgCentre: MessageCentre,
                              refLookupProvider: () => Map[String, String],
                              val id: String = nextFuncName
                               ) extends LiftActor {

  import MutableTextWithStepRefs._

  private[lib] var curRefLookup = Map.empty[String, String]
  private[lib] var refsInText = Map.empty[String, String]
  private[lib] var refsInLinkNext = Map.empty[String, String]

  private[lib] var _text = ""

  def text = _text

  def text_=(newValueRaw: String) {
    val newValue = newValueRaw.trim
    if (text != newValue) {
      _text = parseText(newValue)
    }
  }

  def init() {
    msgCentre.register(this)
    _text = parseText(_text)
  }

  def renderTextarea = SHtml.ajaxTextarea(text, onTextChange _, "id" -> id)

  /**
   * Callback when the user changes the text.
   */
  def onTextChange(newValue: String): JsCmd = {
    text = newValue
    if (text != newValue)
      updateTextJs
    else
      JsCmds.Noop
  }

  /**
   * Scans text for step references.
   *
   * Creates a map-to-ids of valid references.
   * Removes whitespace from references.
   * Appends a ? to invalid references.
   */
  private def parseText(origText: String): String = {

    var (text,textSuffix) = parseTextLinkNext(origText)

    text = parsePlainText(text)

    List(text, textSuffix).filterNot(_.isEmpty).mkString(" ")
  }

  /**
   * Parses a plan text.
   * Step refs are normalised in text, and recorded in curRefLookup.
   */
  private def parsePlainText(text: String) : String = {
    val refLookup = refLookupProvider()
    refsInText = Map.empty

    val newText = StepRefRegex.replaceAllIn(text, m => {

      // Inspect ref
      val rawLabel = m.matched
      if (IsInvalidStepLabel(rawLabel))
        rawLabel // ignore refs without dots

      else {
        val label = NormaliseStepRef(m.matched)
        if (refLookup.contains(label)) {

          // Match found
          if (!refsInText.contains(label)) refsInText += (label -> refLookup(label))
          label
        } else
          InvalidStepRef(label)
      }
    })
    curRefLookup = refLookup
    newText
  }

  /**
   * Scans a text string for an optional "--> 1.0.2" suffix.
   * If found (and valid), the suffix is extracted and normalised.
   */
  private def parseTextLinkNext(text: String) : Tuple2[String,String] = {
    var (left,suffix) = (text,"")
    text match {
      case ArrowSplitRegex(l,r) =>
        val refLookup = refLookupProvider()
        var good = true
        var validLabels = TreeSet.empty[String]
        for (rawLabel <- r.replaceAll("]","],").split(",") if good) {
          val label = UnRef(NormaliseStepRef(rawLabel))
          if (!label.isEmpty) {
            if (IsInvalidStepLabel(label) || !refLookup.contains(label)) {
              good = false
            } else {
              validLabels += label
            }
          }
        }
        if (good && validLabels.nonEmpty) {
          val labels = validLabels.mkString(", ")
          left = l.trim
          suffix = s"$ArrowGood $labels"
        }
      case _ =>
    }
    left = ArrowRegex.replaceAllIn(left, ArrowBad)
    (left, suffix)
  }

  override def messageHandler = {
    case StepChangeMsg if refsInText.nonEmpty =>

      // Ignore changes if already processed
      val newRefLookup = refLookupProvider()
      if (newRefLookup != curRefLookup) {

        // Update step references
        var newRefsInText = Map.empty[String, String]
        var newText = text
        for ((oldLabel, id) <- refsInText) {

          // Lookup each existing reference
          newRefLookup.get(id).map { newLabel =>
            if (oldLabel != newLabel)
              newText = newText.replace(MakeRef(oldLabel), MakeRef(newLabel))
            if (!newRefsInText.contains(newLabel)) newRefsInText += (newLabel -> id)
          } orElse {
            newText = newText.replace(MakeRef(oldLabel), DeletedRef)
            None
          }
        }

        // Save and publish text changes
        if (newText != text) {
          _text = newText
          refsInText = newRefsInText
          msgCentre ! PushToClient(updateTextJs)
        }

        // Record ref lookup table so that we avoid re-processing when nothing upstream changes
        curRefLookup = newRefLookup
      }
  }

  private def updateTextJs(): JsCmd = JqId(id) ~> JqSetValue(text, false)
}