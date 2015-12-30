package shipreq.webapp.server.lib

import scalaz.IdInstances
import scalaz.old.std._
import scalaz.std.{StringInstances, OptionInstances, TupleInstances, MapInstances, ListInstances, FunctionInstances}
import scalaz.syntax.{ToShowOps, ToMonadOps, ToMonadPlusOps, ToFunctorOps, ToFoldableOps, ToBifunctorOps, ToMonoidOps, ToSemigroupOps}

object ScalazSubset

  extends IdInstances

          with ToSemigroupOps
          with ToMonoidOps
          with ToMonadPlusOps
          with ToMonadOps
          with ToBifunctorOps
          with ToFoldableOps
          with ToFunctorOps
          with ToShowOps

          with FunctionInstances
          with ListInstances
          with OptionInstances
          with StringInstances
          with MapInstances
          with NodeSeqInstances
          with TupleInstances
