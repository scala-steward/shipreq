package com.beardedlogic.usecase
package lib

import net.liftweb.http.CometActor
import field.{NormalAndAlternateCourseFields => NCAC, ExceptionCourseFields => EC, _}
import model._
import msg.MessageCentre
import TypeTags._

class UseCaseCtx(cometActor: CometActor) {

  val msgCentre = new MessageCentre(cometActor)

  var number = 1: Short
  var title = "Untitled"

  // TODO hardcoded fieldlist
  val fieldList = Defaults.FieldList
  val fields = fieldList.fieldKeys.map(k => k.fieldDef.newFieldInstance(this, k))

  val courseFields: List[CourseFields] = fields.collect { case f: CourseFields => f }
  def textFields = fields.collect { case f: TextField => f }
  def ncacField: Option[NCAC] = courseFields.collectFirst { case f: NCAC => f }
  def ecField: Option[EC] = courseFields.collectFirst { case f: EC => f }
  // TODO inefficient UCEditorState.stepLabelMap
  def stepLabelMap: Map[String, String] = courseFields.foldLeft(Map.empty[String, String]) { _ ++ _.stepLabelMap }
  def stepLabelMapProvider = () => stepLabelMap

  val normalCourseTitleId = ncacField.get.courses.head.stepTextId

  private[lib] var _savedSteps = Map.empty[Long_StepId, String]
  def savedSteps =  _savedSteps

  // -------------------------------------------------------------------------------------------------------------------
}

case class UseCaseSaveCheckpoint(
  data: Data[DataType.UseCase],
  uc: UseCase,
  saveCtx: FieldSaveCtx,
  fieldStates: Map[FieldKey, Any]
  )
