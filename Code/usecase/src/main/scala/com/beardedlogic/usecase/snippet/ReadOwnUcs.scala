package com.beardedlogic.usecase.snippet

import com.beardedlogic.usecase.app.{RequestVars, DI}
import com.beardedlogic.usecase.feature.uc.{UseCase, UseCasePersistence}
import com.beardedlogic.usecase.lib.Locks
import com.beardedlogic.usecase.lib.Types._
import com.beardedlogic.usecase.feature.publish.{Input, HtmlPublisher}
import xml.NodeSeq

object ReadOwnUcs {

  def render = {
    val project = RequestVars.SoleProject.get

    val ucs: List[UseCase] =
      DI.DaoProvider.withTransaction(dao =>
        Locks.UseCaseNumbers.read(project)(lock =>
          UseCasePersistence.loadAll(project, dao, lock)))

    val i = Input(None, ucs)
    val o: NodeSeq = HtmlPublisher.publish(i)
    o
  }
}
