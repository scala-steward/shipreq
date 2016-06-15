package shipreq.webapp.gen

import scala.scalajs.js.JSApp

object Main extends JSApp {
  def main(): Unit = {
    Manifest.All.foreach(_.gen.printFileContent())
    println()
  }
}
