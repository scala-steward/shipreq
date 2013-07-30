package com.beardedlogic.usecase
package lib

import net.liftweb.common.Logger
import field._
import model._
import util.CachedFunction

/**
 * Data IDs below 100 are reserved and can be safely allocated here.
 */
object ReservedIds {

  val DefaultFieldList = 1
}

object Defaults extends Logger {

  /** Default title of new use cases. */
  val Title = "Untitled"

  val FieldListDefns: List[FieldDefinition] =
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

  val FieldList = CachedFunction.eager1WithInitial[DAO, FieldListRec](dao => {
    val fl = dao.syncFieldList(ReservedIds.DefaultFieldList, FieldListDefns)
    debug(s"Default field list: ${fl.dataId}:${fl.valueId}")
    fl
  })(null)

  def init() {
    DI.DaoProvider.withTransaction { dao =>
      FieldList.refresh(dao)
    }
    debug("Defaults initialised successfully.")
  }
}