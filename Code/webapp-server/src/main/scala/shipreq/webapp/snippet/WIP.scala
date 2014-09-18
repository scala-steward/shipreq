package shipreq.webapp.snippet

import net.liftweb.http.S
import net.liftweb.util.Helpers._
import shipreq.webapp.lib.RawJsonResponse
import shipreq.webapp.shared.{ExampleData, Interface}

class WIP {
  def render = {
    val wired1 = interfaceImpl(Interface.Defn.Square)(n => s"$n² = ${n*n}")
    val wired2 = interfaceImpl(Interface.Defn.Half)(n => s"$n/2 = ${n/2}")
    val wired3 = interfaceImpl(Interface.Defn.Grrr)(e => ExampleData(e.i + 9000))
    val pg = Interface.Page.WIP(wired1,wired2,wired3)
    val pgJson = upickle.write(pg)

    "*" #> (
      <script type="text/javascript">
        hahaa.ReactExamples().main()
      </script>
        <div><h4>manual</h4><pre><code>
          var r = hahaa.ReactExamples()
          var w = r.wired('{pgJson}')
          r.invokeSquare(w, 7)
          r.invokeGrrr(w, 7)
        </code></pre></div>
      )
  }

  def interfaceImpl[D <: Interface.Defn](d: D)(f: d.I => d.O)(implicit I: upickle.Reader[d.I], O: upickle.Writer[d.O]) = {
    // TODO test all failure scenarios imaginable
    val proc = S.SFuncHolder(req => {
        val i = upickle.read[d.I](req)
        val o = f(i)
        val r = upickle.write[d.O](o)
        RawJsonResponse(r)
      })
    val fnName = S.fmapFunc(proc)(n => n)
    Interface.Wired[D](fnName, d)
  }
}
