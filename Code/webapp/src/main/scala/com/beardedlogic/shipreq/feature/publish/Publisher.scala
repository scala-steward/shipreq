package com.beardedlogic.shipreq.feature.publish

import org.joda.time.DateTime
import com.beardedlogic.shipreq.db.UseCaseRev
import com.beardedlogic.shipreq.feature.uc.UseCase
import com.beardedlogic.shipreq.util.DateTimeOrdering.instance
import UseCase.ordering

case class DocHeader(
  title: String,
  preface: Option[String])

class Input(val header: Option[DocHeader], ucInput: List[(UseCase, UseCaseRev)]) {

  val sortedUseCases: List[UseCase] =
    ucInput.map(_._1).sorted

  val revMap: Map[UseCase, UseCaseRev] =
    ucInput.toMap

  val lastUpdated: Option[DateTime] =
    if (ucInput.isEmpty)
      None
    else
      Some(ucInput.maxBy(_._2.createdAt)._2.createdAt)
}

trait Publisher[Output] {
  def publish(input: Input): Output
}
