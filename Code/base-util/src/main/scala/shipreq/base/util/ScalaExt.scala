package shipreq.base.util

object ScalaExt {

  implicit class AnyExt[A](val a: A) extends AnyVal {
    @inline def |>[B](f: A => B): B = f(a)
  }

  implicit class Tuple2Ext[A, B](val t: (A, B)) extends AnyVal {
    def map1[X](f: A => X): (X, B) = (f(t._1), t._2)
    def map2[X](f: B => X): (A, X) = (t._1, f(t._2))
  }

  implicit class Tuple3Ext[A, B, C](val t: (A, B, C)) extends AnyVal {
    def map1[X](f: A => X): (X, B, C) = (f(t._1), t._2, t._3)
    def map2[X](f: B => X): (A, X, C) = (t._1, f(t._2), t._3)
    def map3[X](f: C => X): (A, B, X) = (t._1, t._2, f(t._3))
  }

}
