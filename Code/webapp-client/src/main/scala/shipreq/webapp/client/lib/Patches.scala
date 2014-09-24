package shipreq.webapp.client.lib

import scala.scalajs.js

object Patches {

  import upickle.Js
  def readJs(value: Any): Js.Value = value match{
    case s: js.String => Js.Str(s)
    case n: js.Number => Js.Num(n)
    case true => Js.True
    case false => Js.False
    case null => Js.Null
    case s: js.Array[_] => Js.Arr(s.map(readJs(_: Any)):_*)
    case s: js.Object => Js.Obj(s.asInstanceOf[js.Dictionary[_]].mapValues(readJs).toSeq:_*)
  }

}
