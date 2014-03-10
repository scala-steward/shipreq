package shipreq.webapp.db

import org.postgresql.util.PGobject

object SqlHelpers {

  @inline def pgObject(typ: String, value: String): PGobject = {
    val o = new PGobject()
    o.setType(typ)
    o.setValue(value)
    o
  }

  private[this] val LeadingWhitespace = """[\r\n]+\s*""".r

  implicit class SqlStringExt(val s: String) extends AnyVal {
    def sql = LeadingWhitespace.replaceAllIn(s, " ").trim
    def inTable(table: String) = {
      val p = table + "."
      """(^|,)\s*""".r.replaceAllIn(s, _.group(0)+p)
    }
  }
}