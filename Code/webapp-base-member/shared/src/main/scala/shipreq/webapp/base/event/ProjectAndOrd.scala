package shipreq.webapp.base.event

import shipreq.webapp.base.data.Project

final case class ProjectAndOrd(project: Project, ord: Option[EventOrd.Latest]) {

  def >(x: ProjectAndOrd): Boolean =
    (ord, x.ord) match {
      case (Some(a), Some(b)) => a > b
      case (Some(_), None   ) => true
      case (None   , Some(_))
         | (None   , None   ) => false
    }

  @inline def < (x: ProjectAndOrd) = x > this
  @inline def <=(x: ProjectAndOrd) = !this.>(x)
  @inline def >=(x: ProjectAndOrd) = !this.<(x)
}

object ProjectAndOrd {
  val empty = apply(Project.empty, None)
}