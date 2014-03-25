package shipreq.base.util

object ScalaExt {

  implicit class Tuple2Ext[A, B](val t: (A, B)) extends AnyVal {
    def map1[C](f: A => C): (C, B) = (f(t._1), t._2)
    def map2[C](f: B => C): (A, C) = (t._1, f(t._2))
  }

}
