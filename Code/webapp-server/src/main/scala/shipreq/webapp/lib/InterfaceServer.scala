package shipreq.webapp.lib

import net.liftweb.http.S
import upickle._
import shipreq.webapp.shared.rpc.Interface

/**
 * Server-side RPC support.
 */
object InterfaceServer {

  def impl[D <: Interface.Def](d: D)(f: d.I => d.O)(implicit I: Reader[d.I], O: Writer[d.O]) = {
    // TODO test all failure scenarios imaginable
    val proc = S.SFuncHolder(req => {
      val i = read[d.I](req)
      val o = f(i)
      val r = write[d.O](o)
      RawJsonResponse(r)
    })
    val fnName = S.fmapFunc(proc)(n => n)
    Interface.Remote[D](fnName, d)
  }

}
