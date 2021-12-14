package shipreq.webapp.base.data

import japgolly.microlibs.adt_macros.AdtMacros

sealed abstract class ProjectPerm(final val ord: Int)

object ProjectPerm {
  case object Admin        extends ProjectPerm(0)
  case object Collaborator extends ProjectPerm(1)

  val values = AdtMacros.adtValues[ProjectPerm]
}

