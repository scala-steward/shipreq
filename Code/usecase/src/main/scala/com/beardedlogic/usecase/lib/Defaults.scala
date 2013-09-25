package com.beardedlogic.usecase
package lib

import net.liftweb.common.Logger
import scalaz.{Name, Need}
import field._
import db._

object Defaults extends Logger {

  // TODO Change defaults to lowercase

  /** Default title of new use cases. */
  val Title = "Untitled"

  val useCaseHeader = UseCaseHeader(Title)

  val FieldListDefns: List[FieldDefinition] =
    TextFieldDefinition("Description") ::
      TextFieldDefinition("Actors") ::
      TextFieldDefinition("Pre-Conditions") ::
      TextFieldDefinition("Post-Conditions") ::
      NormalCourseFieldDefinition ::
      ExceptionCourseFieldDefinition ::
      TextFieldDefinition("Use Case Relationships") ::
      TextFieldDefinition("Constraints and Business Rules") ::
      TextFieldDefinition("Frequency of Use") ::
      TextFieldDefinition("Special Requirements") ::
      TextFieldDefinition("Assumptions") ::
      TextFieldDefinition("Notes and Issues") ::
      Nil

  val FieldList: Name[FieldListRec] = dbVal(_.syncFieldList(FieldListDefns))

  private def dbVal[V](fn: Dao => V): Name[V] = Need(DI.DaoProvider.withTransaction(fn))

  def init(): Unit = {
    FieldList.value
    debug("Defaults initialised successfully.")
  }
}