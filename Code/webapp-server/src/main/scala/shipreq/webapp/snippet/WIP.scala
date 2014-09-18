package shipreq.webapp.snippet

import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.Alert
import net.liftweb.json.JsonAST.JValue
import shipreq.webapp.app.AppConfig
import shipreq.webapp.util.{HtmlTransformExt, CacheFn, NonEmptyTemplate}
import HtmlTransformExt._
import net.liftweb.common.{Empty, Logger}
import net.liftweb.util.Helpers._
import net.liftweb.http.{JsonContext, S, SHtml}
import scala.util.Random
import scala.xml.{NodeSeq, Elem, Node}
import shipreq.webapp.shared.Interface

class WIP {
  def render = {
//    val manualFnName1 = S.fmapFunc(jsonPipeline(square))(n => n)
//    val manualFnName2 = S.fmapFunc(jsonPipeline(half))(n => n)

    val wired1 = interfaceImpl(Interface.Defn.Square)(n => s"$n² = ${n*n}")
    val wired2 = interfaceImpl(Interface.Defn.Half)(n => s"$n/2 = ${n/2}")
    val pg = Interface.Page.WIP(wired1,wired2)
    val pgJson = upickle.write(pg)

//    val manualEg = s"""liftAjax.lift_ajaxHandler('$manualFnName=' + encodeURIComponent(JSON.stringify( {id:"1"} )), function(a,b,c,d){console.log("Success",a,b,c,d)}, function(a,b,c,d){console.log("Error",a,b,c,d)}, "json")"""
//    val manualEg = s"""hahaa.ReactExamples().invokeSquare('$manualFnName')"""

//SHtml.ajaxCall()
//SHtml.jsonCall()
//SHtml.ajaxInvoke()

    /*
        <div><h4>jsonCall</h4><pre><code>
          {SHtml.jsonCall(JsRaw(""" {id : "1"}  """), clientCalled _)}
        </code></pre></div>
        <div><h4>jsonCall</h4><pre><code>
          {SHtml.jsonCall(JsRaw(""" {id : "1"}  """), new JsonContext(Empty, Empty), clientCalledJ _)}
        </code></pre></div>
        <div><h4>ajaxInvoke</h4><pre><code>
          {SHtml.ajaxInvoke(noArgCallback _)}
        </code></pre></div>
    {manualFnName1}
    {manualFnName2}
    {pgJson}
    var r = hahaa.ReactExamples(); var w = r.wire('{manualFnName1}','{manualFnName2}')
    */

    "*" #> (
      <script type="text/javascript">
        hahaa.ReactExamples().main()
      </script>
        <div><h4>manual</h4><pre><code>
          var r = hahaa.ReactExamples()
          var w = r.wired('{pgJson}')
          r.invokeSquare(w, 7)
          r.invokeHalf(w, 7)
        </code></pre></div>
      )
  }

  def interfaceImpl[D <: Interface.Defn](d: D)(f: d.I => d.O)(implicit I: upickle.Reader[d.I], O: upickle.Writer[d.O]) = {
    val proc = S.SFuncHolder(req => {
        val i = upickle.read[d.I](req)
        val o = f(i)
        val r = upickle.write[d.O](o)
        RawJsonResponse(r)
      })
    val fnName = S.fmapFunc(proc)(n => n)
    Interface.Wired[D](fnName, d)
  }

//  def interfaceImpl[D <: Interface.Defn[I, O], I, O](d: D)(f: I => O)(implicit I: upickle.Reader[I], O: upickle.Writer[O]) = {
//    val proc = S.SFuncHolder(req => {
//        val i = upickle.read[I](req)
//        val o = f(i)
//        upickle.write[O](o)
//      })
//    val fnName = S.fmapFunc(proc)(n => n)
//    Interface.Wired[D,I,O](fnName, d)
//  }

//  def noArgCallback: JsCmd =
//    Alert("yes?")
//
//  import net.liftweb.json._
//  implicit val formats = DefaultFormats
//
//  def clientCalled(x: JValue): JsCmd =
//    Alert(s"Got: ${compactRender(x)}")
//
//  def clientCalledJ(x: JValue): JValue =
//    JObject(JField("hehe", x) :: Nil)
//
//  def square(x: JValue): JValue = { val JInt(n) = x; JString(s"$n² = ${n*n}") }
//  def half(x: JValue): JValue = { val JInt(n) = x; JString(s"$n/2 = ${n/2}") }
//
//  // parseOpt swallows parsing exceptions. Use parse instead
//  def jsonPipeline(f: JValue => JValue) =
//    S.SFuncHolder(s => parseOpt(s).map(f) getOrElse JNothing)

  // <script type="text/javascript">hahaa.ReactExamples().main()</script>
}

import net.liftweb.http._
import provider._

object RawJsonResponse {
  def headers: List[(String, String)] = S.getResponseHeaders(Nil)
  def cookies: List[HTTPCookie] = S.responseCookies

  def apply(json: String): LiftResponse =
    new RawJsonResponse(json, headers, cookies, 200)

//  def apply(json: JsonAST.JValue): LiftResponse =
//    apply(json, headers, cookies, 200)
//
//  def apply(json: JsonAST.JValue, code: Int): LiftResponse =
//    apply(json, headers, cookies, code)
//
//
//  def apply(_json: JsonAST.JValue, _headers: List[(String, String)], _cookies: List[HTTPCookie], code: Int): LiftResponse = {
//    new RawJsonResponse(new JsExp {
//      lazy val toJsCmd = jsonPrinter(JsonAST.render((_json)))
//    }, _headers, _cookies, code)
//  }
//
//  lazy val jsonPrinter: scala.text.Document => String =
//    LiftRules.jsonOutputConverter.vend
}

case class RawJsonResponse(json: String, headers: List[(String, String)], cookies: List[HTTPCookie], code: Int) extends LiftResponse {
  def toResponse = {
    val bytes = json.getBytes("UTF-8")
    InMemoryResponse(bytes, ("Content-Length", bytes.length.toString) :: ("Content-Type", "application/json; charset=utf-8") :: headers, cookies, code)
  }
}
