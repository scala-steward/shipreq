package com.beardedlogic.usecase.lib
package field

import scala.xml.NodeSeq
import net.liftweb.http.Templates
import net.liftweb.util.ClearClearable
import net.liftweb.util.Helpers._

object Fields {

  val TemplateSource = ClearClearable(Templates("uce" :: Nil) openOrThrowException "UC Editor template not found.")

  private[field] def Template(id: String): NodeSeq = {
    var t = s"#$id ^^" #> ""
    if (id.startsWith("template-")) t = t & s"#$id [id]" #> (None: Option[String])
    val r = t(TemplateSource)
    if (r.isEmpty) {
      val e = new Exception(s"Failed to load template: $id\nTemplateSource.length = ${TemplateSource.length}")
      // e.printStackTrace()
      throw e
    }
    r
  }
}
