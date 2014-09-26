package shipreq.webapp.snippet

import net.liftweb.util.Helpers._
import shipreq.webapp.lib.InterfaceServer
import shipreq.webapp.shared.rpc.{ExampleData, Interfaces}

class WIP {
  def render = {
    val wired1 = InterfaceServer.impl(Interfaces.Square)(n => s"$n² = ${n*n}")
    val wired2 = InterfaceServer.impl(Interfaces.Half)(n => s"$n/2 = ${n/2}")
    val wired3 = InterfaceServer.impl(Interfaces.Grrr)(e => ExampleData(e.i + 9000))
    val pg = Interfaces.WIP(wired1,wired2,wired3)
    val pgJson = upickle.write(pg) // TODO move into InterfaceServer

    "*" #> (
      <script type="text/javascript">
        X = {pgJson}
        R = hahaa.ReactExamples()
        W = R.wired(X)
        R.main()
      </script>
        <div><h4>manual</h4><pre><code>
          R.invokeSquare(W, 7)
          R.invokeGrrr(W, 7)
        </code></pre></div>
      )
  }
}
