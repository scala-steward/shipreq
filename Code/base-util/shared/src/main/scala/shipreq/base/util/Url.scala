package shipreq.base.util

object Url {

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  /** Always starts with a slash. */
  final case class Relative(relativeUrl: String) extends AnyVal {
    def thenParam[A](f: A => String): Relative.Param1[A] =
      Relative.Param1(this, f)
  }
  object Relative {
    def apply(value: String): Relative =
      new Relative(value.replaceFirst("^/*", "/"))

    /** Represents `/prefix/<A>`; the param is always last */
    final case class Param1[-A](prefix: Relative, suffix: A => String) {
      assert(prefix.relativeUrl != "/")

      private val pre = prefix.relativeUrl + "/"

      def apply(a: A): Relative =
        new Relative(pre + suffix(a))
    }
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  final case class Absolute(absoluteUrl: String) extends AnyVal
  object Absolute {

    /** Never ends with a slash. */
    final case class Base(value: String) extends AnyVal {
      def /(r: Relative): Absolute =
        Absolute(value + r.relativeUrl)
      def /[A](r: Relative.Param1[A]): Absolute.Param1[A] =
        Absolute.Param1(this / r.prefix, r.suffix)
    }
    object Base {
      def apply(value: String): Base =
        new Base(value.replaceFirst("/+$", ""))
    }

    /** Represents `https://blah.com/prefix/<A>`; the param is always last */
    final case class Param1[-A](prefix: Absolute, suffix: A => String) {

      private val pre = prefix.absoluteUrl + "/"

      def apply(a: A): Absolute =
        Absolute(pre + suffix(a))
    }
  }

}
