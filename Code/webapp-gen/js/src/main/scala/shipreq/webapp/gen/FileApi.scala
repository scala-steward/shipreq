package shipreq.webapp.gen

import scala.scalajs.js
import phantomjs.FS

/*
None of this works because of:
http://stackoverflow.com/questions/30414033/cannot-call-requirefs-with-phantomjs-runner/30460169#30460169
http://stackoverflow.com/questions/27487724/writing-to-filesystem-from-within-phantomjs-sandboxed-environment
https://github.com/scala-js/scala-js/blob/v0.6.9/js-envs/src/main/scala/org/scalajs/jsenv/phantomjs/PhantomJSEnv.scala#L445-L460
 */
object FileApi {

//  lazy val FS: phantomjs.FS =
//    js.Dynamic.global.FS.asInstanceOf[phantomjs.FS]

  def write(filename: String, content: String): Unit = {
    val p = js.Object().asInstanceOf[phantomjs.WriteParams]
    p.mode = "w"
    p.charset = "utf8"
    FS.write(filename, content, p)
  }
}
