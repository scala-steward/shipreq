package com.beardedlogic.usecase.lib

import com.beardedlogic.usecase.db.UseCaseSummary
import Types._

trait UseCaseRelations {
  def findUcTitle(num: UseCaseNumber): Option[String]
}

object UseCaseRelations {
  val Empty: UseCaseRelations = new UseCaseRelations {
    override def findUcTitle(num: UseCaseNumber) = None
  }
}

case class CachedUseCaseRelations(ucs: List[UseCaseSummary]) extends UseCaseRelations {
  override def findUcTitle(num: UseCaseNumber) = ucs.find(_.number == num).map(_.title)
}
