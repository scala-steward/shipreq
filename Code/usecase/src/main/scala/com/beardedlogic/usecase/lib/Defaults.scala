package com.beardedlogic.usecase
package lib

import field._
import model._
import db.DB
import net.liftweb.common.Logger

/**
 * Data IDs below 100 are reserved and can be safely allocated here.
 */
object ReservedIds {

  val DefaultFieldList = 1
}

object Defaults extends Logger {
  private[this] implicit var s = DB.Slick.createSession()

  val FieldList = {
    //  val DateCreated = TextFieldDef("Date Created")
    //  val DateLastUpdated = TextFieldDef("Date Last Updated")
    val fields: List[FieldDef] =
      TextFieldDef("Actors") ::
        TextFieldDef("Pre-Conditions") ::
        TextFieldDef("Post-Conditions") ::
        NormalAndAlternateCourseFields ::
        ExceptionCourseFields ::
        TextFieldDef("Use Case Relationships") ::
        TextFieldDef("Constraints and Business Rules") ::
        TextFieldDef("Frequency of Use") ::
        TextFieldDef("Special Requirements") ::
        TextFieldDef("Assumptions") ::
        TextFieldDef("Notes and Issues") ::
        Nil
    model.FieldList.ensureSavedAndLatest(ReservedIds.DefaultFieldList, fields)
  }
  debug(s"Default field list: ${FieldList.dataId}/${FieldList.valueId}")

  s.close()
  s= null

  def init() {
    debug("Defaults initialised successfully.")
  }
}