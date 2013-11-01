package com.beardedlogic.usecase.feature.publish

import com.beardedlogic.usecase.db.UseCaseRev
import com.beardedlogic.usecase.feature.uc.UseCase
import com.beardedlogic.usecase.feature.uc.persist.UseCaseSaveCheckpoint
import UseCase.ordering

case class DocHeader(
  title: String,
  preface: Option[String])

class Input(val header: Option[DocHeader], ucInput: List[UseCaseSaveCheckpoint]) {
  val sortedUseCases: List[UseCase] = ucInput.map(_.uc).sorted
  val revMap: Map[UseCase, UseCaseRev] = ucInput.map(cp => (cp.uc, cp.rec)).toMap
}

trait Publisher[Output] {
  def publish(input: Input): Output
}
