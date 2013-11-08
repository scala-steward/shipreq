package com.beardedlogic.usecase.feature.publish

import org.joda.time.DateTime
import com.beardedlogic.usecase.db.UseCaseRev
import com.beardedlogic.usecase.feature.uc.UseCase
import com.beardedlogic.usecase.feature.uc.persist.UseCaseSaveCheckpoint
import com.beardedlogic.usecase.util.DateTimeOrdering.instance
import UseCase.ordering

case class DocHeader(
  title: String,
  preface: Option[String])

class Input(val header: Option[DocHeader], ucInput: List[UseCaseSaveCheckpoint]) {

  val sortedUseCases: List[UseCase] =
    ucInput.map(_.uc).sorted

  val revMap: Map[UseCase, UseCaseRev] =
    ucInput.map(cp => (cp.uc, cp.rec)).toMap

  val lastUpdated: Option[DateTime] =
    if (ucInput.isEmpty)
      None
    else
      Some(ucInput.maxBy(_.rec.createdAt).rec.createdAt)
}

trait Publisher[Output] {
  def publish(input: Input): Output
}
