package shipreq.webapp.gen.transform

import shipreq.webapp.base.data.{ProjectMetaData, Username}
import shipreq.webapp.gen._
import scala.xml.Utility.escape

object ProjectSpaLoader extends Transformer(
    shipreq.webapp.gen.output.ProjectSpaLoader.templates,
    Data.projectSpaLoaderData) {

   def apply(i: (Username, ProjectMetaData)): Html =
    templates.main.map(_
      .replace(data.main._1.value, i._1.value)
      .replace(data.main._2.name, escape(i._2.name)))
}
