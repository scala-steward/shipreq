package shipreq.webapp.base.issue

final case class IssueId(value: Int)

final case class IssueWithId(id: IssueId, issue: Issue)

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