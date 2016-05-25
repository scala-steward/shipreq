package shipreq.webapp.client.home

import scalajs.js
import scalajs.js.annotation.JSExport
import shipreq.webapp.base.protocol.{ClientFnDecl, InitDataForHomeSpa}
import shipreq.webapp.client.base.protocol.ClientFnImpl

@JSExport(ClientFnDecl.HomeSpaName)
object Main extends ClientFnImpl(ClientFnDecl.HomeSpa) {
  override def run(i: InitDataForHomeSpa): Unit = {
    println(i.username)
    i.projects.items.foreach(println)
  }
}
