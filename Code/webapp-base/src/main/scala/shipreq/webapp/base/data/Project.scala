package shipreq.webapp.base.data

import monocle.{SimpleLens, Lenser}

trait DataSetAccessor[D] {
  def getRev(p: Project): Rev
  def getData(p: Project): Stream[D]
  def set(p: Project, r: Rev, d: Stream[D]): Project
}

case class RevAnd[D](rev: Rev, data: D)

object RevAnd {
  def _data[D] = SimpleLens[RevAnd[D]](_.data)((a, b) => a.copy(data = b))
}

// TODO change List to Vector
// TODO this is just RevAnd[List[D]]
case class DataSet[D](rev: Rev, data: List[D])

object DataSet {
  def _data[D] = SimpleLens[DataSet[D]](_.data)((a, b) => a.copy(data = b))
}

object Project {
  private[this] def l = Lenser[Project]
  val _customIncmpTypes = l(_.customIncmpTypes)
  val _customReqTypes   = l(_.customReqTypes)

  private def dsa[D](ds: SimpleLens[Project, DataSet[D]]): DataSetAccessor[D] =
    new DataSetAccessor[D] {
      override def getRev(p: Project)                    = ds.get(p).rev
      override def getData(p: Project)                   = ds.get(p).data.toStream
      override def set(p: Project, r: Rev, d: Stream[D]) = ds.set(p, DataSet[D](r, d.toList))
    }

  trait Implicits {
    implicit val dsaCustomIncmpType = dsa(_customIncmpTypes)
    implicit val dsaCustomReqType   = dsa(_customReqTypes)
  }
}

final case class Project(customIncmpTypes: DataSet[CustomIncmpType],
                         customReqTypes:   DataSet[CustomReqType],
                         tags:             RevAnd[TagTree]) {
  import shipreq.prop._
  this assertSatisfies DataProp.project

  def rev = customIncmpTypes.rev + customReqTypes.rev + tags.rev

  override def toString =
    Stream(customIncmpTypes, customReqTypes, tags)
      .map("\n    " + _.toString.replace(" -> ", " → "))
      .mkString("Project(", "", "\n)")
}