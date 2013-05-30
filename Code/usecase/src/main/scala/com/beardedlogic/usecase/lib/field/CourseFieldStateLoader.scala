package com.beardedlogic.usecase
package lib
package field

import StepTree._
import model._
import CourseFields._
import TypeTags._
import CourseFieldState._
import scala.annotation.tailrec

class CourseFieldStateLoader(val fieldKey: FieldKey, val li: StartingLabelIndices) extends FieldStateLoader[CourseFieldState] {

  override def load(ctx: FieldLoadCtx) = ???
}

object CourseFieldState {
}

case class CourseFieldState(courses: List[StepState]) {

  val stepMap = {
    val map = Map.newBuilder[String @@ LocalStepId, StepState]
    courses.foreach(_.deepForeach(ss => map += (ss.id -> ss)))
    map.result
  }
}

case class StepState(
  id: String @@ LocalStepId,
  text: String @@ NormalisedRefs,
  children: List[StepState]
  ) {

  def deepForeach(fn: StepState => Any) {
    fn(this)
    children foreach (_ deepForeach fn)
  }
}
