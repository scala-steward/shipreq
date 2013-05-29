package com.beardedlogic.usecase
package lib
package field

import StepTree._
import model._
import FieldValue.FieldValueData
import CourseFields._

class CourseFieldStateLoader(val fieldKey: FieldKey, val li: StartingLabelIndices) extends FieldStateLoader[CourseFieldState] {

  override def load(ctx: FieldLoadCtx) = ???
}
