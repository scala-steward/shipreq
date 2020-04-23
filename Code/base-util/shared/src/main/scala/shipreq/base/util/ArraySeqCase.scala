package shipreq.base.util

import scala.collection.immutable.ArraySeq

/**
 * Pattern matching on ArraySeqs.
 */
object ArraySeqCase {

  object Empty {
    def unapply[A](v: ArraySeq[A]) = v.isEmpty
  }

  object Sole {
    def unapply[A](v: ArraySeq[A]) = new Unapply(v)
    final class Unapply[A](val v: ArraySeq[A]) extends AnyVal {
      def isEmpty = v.length != 1
      def get     = v.head
    }
  }

  object NonEmpty {
    def unapply[A](v: ArraySeq[A]) = new Unapply(v)
    final class Unapply[A](val v: ArraySeq[A]) extends AnyVal {
      def isEmpty = v.isEmpty
      def get     = NonEmptyArraySeq(v.head, v.tail)
    }
  }
}
