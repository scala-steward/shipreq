package shipreq.base.db

object SqlHelpers {

  private[this] val SqlComments = """\s+--[^\r\n]*""".r
  private[this] val LeadingWhitespace = """[\r\n]+\s*""".r

  implicit class SqlStringExt(private val s: String) extends AnyVal {
    def unNull(default: String): String =
      if (s eq null) default else s

    def sql: String = {
      var t = s
      t = SqlComments.replaceAllIn(t, "")
      t = LeadingWhitespace.replaceAllIn(t, " ")
      t.trim
    }
  }

}
