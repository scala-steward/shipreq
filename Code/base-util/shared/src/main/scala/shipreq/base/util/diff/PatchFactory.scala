package shipreq.base.util.diff

trait PatchFactory[+A] {
  def newBuilder(): PatchFactory.Builder[A]
}

object PatchFactory {

  trait Builder[+A] {
    def delete(srcIdx: Int, length: Int): Unit
    def insert(srcIdx: Int, tgtIdx: Int, length: Int): Unit
    def result(): A
  }

  // ===================================================================================================================

  sealed trait Op {
    val srcIdx: Int
    def isInsert: Boolean
    final def isDelete = !isInsert
  }

  object Op {
    final case class Insert(srcIdx: Int, tgtIdx: Int, length: Int) extends Op {
      override def isInsert = true
    }
    final case class Delete(srcIdx: Int, length: Int) extends Op {
      override def isInsert = false
    }

    implicit def univEq: UnivEq[Op] = UnivEq.derive

//    implicit lazy val ordering: Ordering[Op] =
//      (x, y) => {
//        val n = x.srcIdx - y.srcIdx
//        if (n != 0)
//          n
//        else if (x.isDelete)
//          -1
//        else
//          1
//      }
  }

  type Ops = ArraySeq[Op]

  object Ops extends PatchFactory[Ops] {

    override def newBuilder(): PatchFactory.Builder[Ops] =
      new Builder

    private final class Builder extends PatchFactory.Builder[Ops] {
      private val ops = Array.newBuilder[Op]

      override def delete(srcIdx: Int, length: Int): Unit =
        ops += Op.Delete(srcIdx, length)

      override def insert(srcIdx: Int, tgtIdx: Int, length: Int): Unit =
        ops += Op.Insert(srcIdx, tgtIdx, length)

      override def result() = {
        val a = ops.result()
//        a.sortInPlace()
        ArraySeq.unsafeWrapArray(a)
      }
    }
  }

}