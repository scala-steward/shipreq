package shipreq.webapp.base.issue

import japgolly.univeq.UnivEq

final case class IssueId(value: Int)
object IssueId {
  implicit def univEq: UnivEq[IssueId] = UnivEq.derive
}

final case class IssueWithId(id: IssueId, issue: Issue)
object IssueWithId {
  implicit def univEq: UnivEq[IssueWithId] = UnivEq.derive
}

final case class Issues(vector: Vector[IssueWithId]) {
  def count = vector.length
}

object Issues {

  def fromDetectorMap[V](m: Map[IssueDetector, V])(f: V => TraversableOnce[IssueWithId]): Issues = {
    val b = Vector.newBuilder[IssueWithId]
    m.valuesIterator.foreach(b ++= f(_))
    apply(b.result())
  }
}