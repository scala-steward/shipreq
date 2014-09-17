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

class WIP {
  def render = {
    val manualFnName1 = S.fmapFunc(jsonPipeline(square))(n => n)
    val manualFnName2 = S.fmapFunc(jsonPipeline(half))(n => n)
//    val manualEg = s"""liftAjax.lift_ajaxHandler('$manualFnName=' + encodeURIComponent(JSON.stringify( {id:"1"} )), function(a,b,c,d){console.log("Success",a,b,c,d)}, function(a,b,c,d){console.log("Error",a,b,c,d)}, "json")"""
//    val manualEg = s"""hahaa.ReactExamples().invokeSquare('$manualFnName')"""

//SHtml.ajaxCall()
//SHtml.jsonCall()
//SHtml.ajaxInvoke()
    "*" #> (
      <script type="text/javascript">
        hahaa.ReactExamples().main()
      </script>
        <div><h4>jsonCall</h4><pre><code>
          {SHtml.jsonCall(JsRaw(""" {id : "1"}  """), clientCalled _)}
        </code></pre></div>
        <div><h4>jsonCall</h4><pre><code>
          {SHtml.jsonCall(JsRaw(""" {id : "1"}  """), new JsonContext(Empty, Empty), clientCalledJ _)}
        </code></pre></div>
        <div><h4>ajaxInvoke</h4><pre><code>
          {SHtml.ajaxInvoke(noArgCallback _)}
        </code></pre></div>
        <div><h4>manual</h4><pre><code>
          {manualFnName1}
          {manualFnName2}
          var r = hahaa.ReactExamples(); var w = r.wire('{manualFnName1}','{manualFnName2}')
          r.invokeSquare(w, 7)
          r.invokeHalf(w, 7)
        </code></pre></div>
      )
  }

  def noArgCallback: JsCmd =
    Alert("yes?")

  import net.liftweb.json._
  implicit val formats = DefaultFormats

  def clientCalled(x: JValue): JsCmd =
    Alert(s"Got: ${compactRender(x)}")

  def clientCalledJ(x: JValue): JValue =
    JObject(JField("hehe", x) :: Nil)

  def square(x: JValue): JValue = { val JInt(n) = x; JString(s"$n² = ${n*n}") }
  def half(x: JValue): JValue = { val JInt(n) = x; JString(s"$n/2 = ${n/2}") }

  // parseOpt swallows parsing exceptions. Use parse instead
  def jsonPipeline(f: JValue => JValue) =
    S.SFuncHolder(s => parseOpt(s).map(f) getOrElse JNothing)


  // <script type="text/javascript">hahaa.ReactExamples().main()</script>
}
