package shipreq.webapp.client.project.widgets

import japgolly.scalajs.react.vdom.html_<^._
import shipreq.webapp.base.text.Grammar.texTag
import shipreq.webapp.base.ui.semantic.Modal

object RichTextEditorHelp {
  import HelpModal._

  private val references =
    Group("References")(
      Example(
        "Requirements can be referenced by putting the ID inside square brackets.")(
        "[FR-3]"),

      Example(
        "Use case steps can be referenced by putting the use case ID and step number, inside square brackets.")(
        "[UC-2.0.3.a]"),

      Example(
        "Codes can be referenced by putting the code inside square brackets.")(
        "[backend.backup.times]"))

  private val issuesAndTags =
    Group("Issues & Tags")(
      Example(
        "Issues can be declared in text by typing a hash (", code("#"), ") followed by the issue.")(
        "#TBD"),

      Example(
        "Tags can be declared in text by typing a hash (", code("#"), ") followed by the tag.")(
        "#uat"))

  private val multiline =
    Group("Multiline")(
      Example(
        "A list of bullet points can be created by starting new lines with an asterisk (", code("*"), ") followed by a space."
      )(
        "* item 1",
        "* item 2"))

  private val useCaseFlow =
    Group("Use Case flow")(
      Example(
        "Flow between use case steps can be specified by adding arrows ", code("-->"), " and/or ", code("<--"),
        " to the end of a step, followed by the target use case steps.",
        <.br, <.br,
        "When specifying use case steps, you can omit the initial number representing the use case itself.",
        " Eg. if you're in UC-4, you can specify 4.1.2 or just .1.2 for short.",
        <.br, <.br,
        "Currently use case steps can only flow to other steps within the same use case, and not steps in other use cases.",
        " This may change in future."
      )(
        "--> .0.1",
        "--> 3.0.1 <-- 3.3, 3.4"))

  private val other =
    Group("Other")(
      Example(
        "URLs are detected automatically, and presented as links.")(
        "https://google.com"),

      Example(
        "Emails are detected automatically, and presented as links.")(
        "bob.loblaw@ad-law-firm.com"),

      Example(
        "Mathematical expressions can be entered in TeX format, by surrounding in ", <.br, code(s"<$texTag>…</$texTag>"), ".",
        <.br, <.br,
        "For more detail, see ",
        <.a.toNewWindow("https://khan.github.io/KaTeX/")("KaTeX"), " or ",
        <.a.toNewWindow("http://utensil-site.github.io/available-in-katex/")("Symbols and Functions in KaTeX"),
        "."
      )(
        s"<$texTag>{1 \\over n} + x^2</$texTag>"))

  val modal: Modal = {
    val groups = Groups(
      references,
      issuesAndTags,
      multiline,
      useCaseFlow,
      other)
    HelpModal("Rich Text Editor Help", groups)
  }
}
