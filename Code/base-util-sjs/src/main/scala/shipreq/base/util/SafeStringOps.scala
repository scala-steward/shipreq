package shipreq.base.util

object SafeStringOps {

  @inline implicit class SafeStringOps_Str(val _a: String) extends AnyVal {
    @inline def ~(b: String): String = _a + b
    @inline def ~(b: Char)  : String = _a + b
    @inline def ~(b: Int)   : String = _a + b
  }

  @inline implicit class SafeStringOps_Chr(val _a: Char) extends AnyVal {
    @inline def ~(b: String): String = _a.toString + b
    @inline def ~(b: Char)  : String = _a.toString + b
  }
}
