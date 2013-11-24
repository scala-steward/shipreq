package com.beardedlogic.shipreq.feature.uc

import com.beardedlogic.shipreq.db.UseCaseSummary
import com.beardedlogic.shipreq.lib.Types._

trait UseCaseRelations {
  def findUcTitle(num: UseCaseNumber): Option[String]
}

object UseCaseRelations {
  val Empty: UseCaseRelations = new UseCaseRelations {
    override def findUcTitle(num: UseCaseNumber) = None
    override def toString = "UseCaseRelations.Empty"
  }
}

case class CachedUseCaseRelations(ucs: List[UseCaseSummary]) extends UseCaseRelations {
  override def findUcTitle(num: UseCaseNumber) = ucs.find(_.number == num).map(_.title)
}
