package shipreq.base.util

abstract class GenTuple[A,B,C] {
  
  def append(a: A, b: B): C
  
  def init(c: C): (A,B)
  
  def map[X,Y,Z](c: C, f: A => X, g: B => Y, h: (X,Y) => Z): Z = {
    val (a,b) = init(c)
    h(f(a), g(b))
  }
}

object GenTupleImplicitTraits {
  trait P1 {
    implicit def genTupleFrom1[I,X]: GenTuple[I,X,(I,X)] =
      new GenTuple[I,X,(I,X)] {
        override def append(i: I, a: X): (I,X) = (i,a)
        override def init  (c: (I,X))  : (I,X) = c
      }
  }
  
  trait P extends P1 {
    implicit def genTupleFrom2[A,B,X]: GenTuple[(A,B),X,(A,B,X)] =
      new GenTuple[(A,B),X,(A,B,X)] {
        override def append(i: (A,B), x: X): (A,B,X)   = (i._1, i._2, x)
        override def init  (c: (A,B,X))    : ((A,B),X) = ((c._1, c._2), c._3)
      }

    implicit def genTupleFrom3[A,B,C,X]: GenTuple[(A,B,C),X,(A,B,C,X)] =
      new GenTuple[(A,B,C),X,(A,B,C,X)] {
        override def append(i: (A,B,C), x: X): (A,B,C,X)   = (i._1, i._2, i._3, x)
        override def init  (c: (A,B,C,X))    : ((A,B,C),X) = ((c._1, c._2, c._3), c._4)
      }

    implicit def genTupleFrom4[A,B,C,D,X]: GenTuple[(A,B,C,D),X,(A,B,C,D,X)] =
      new GenTuple[(A,B,C,D),X,(A,B,C,D,X)] {
        override def append(i: (A,B,C,D), x: X): (A,B,C,D,X)   = (i._1, i._2, i._3, i._4, x)
        override def init  (c: (A,B,C,D,X))    : ((A,B,C,D),X) = ((c._1, c._2, c._3, c._4), c._5)
      }
  }
}


object GenTuple extends GenTupleImplicitTraits.P {
  implicit final class GenTupleOps[A](val a: A) extends AnyVal {
    def ⊗[B, C](b: B)(implicit t: GenTuple[A, B, C]): C = t.append(a, b)
  }
}