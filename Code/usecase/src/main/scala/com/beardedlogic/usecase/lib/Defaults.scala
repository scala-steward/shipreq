package com.beardedlogic.usecase
package lib

import field._

object Defaults {

  //  val DateCreated = TextFieldDef("Date Created")
  //  val DateLastUpdated = TextFieldDef("Date Last Updated")

  val Fields: List[FieldDef] =
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
}
