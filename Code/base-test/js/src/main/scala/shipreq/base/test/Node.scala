package shipreq.base.test

import japgolly.scalajs.react.AsyncCallback
import scala.concurrent.Future
import scala.scalajs.js
import shipreq.base.test.BaseTestUtil._

/** Node JS access provided by `project/AdvancedNodeJSEnv.scala`.
 */
object Node {

  @inline private def node = js.Dynamic.global.window.node

  private def require(path: String): Any =
    node.require(path)

  private def envVar(name: String): js.UndefOr[String] =
    node.process.env.selectDynamic(name).asInstanceOf[js.UndefOr[String]]

  private def envVarNeed(name: String): String =
    envVar(name).getOrElse(throw new RuntimeException("Missing env var: " + name))

  private val inCI             = envVar("CI").contains("1")
  private val sbtRootDir       = envVarNeed("SBT_ROOT")
  private val testNodeDir      = sbtRootDir + "/frontend/dist/test-node"
  private val asyncTestTimeout = if (inCI) 60000 else 3000

  val loadFakeIndexedDb: () => Unit =
    onceUnit {
      require(s"$testNodeDir/fake-indexeddb")
      js.Dynamic.global.window.indexedDB   = node.indexedDB
      js.Dynamic.global.window.IDBKeyRange = node.IDBKeyRange
    }

  def asyncTest(a: AsyncCallback[_]): Future[Unit] = {
    a.timeoutMs(asyncTestTimeout).map {
      case Some(_) => ()
      case None    => fail(s"Async test timed out after ${asyncTestTimeout / 1000} sec.")
    }.unsafeToFuture()
  }
}
