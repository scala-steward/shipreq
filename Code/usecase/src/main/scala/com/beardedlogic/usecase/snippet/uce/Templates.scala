package com.beardedlogic.usecase.snippet.uce

import net.liftweb.util.Helpers._
import com.beardedlogic.usecase.util.TemplateCache._

object Templates {

  final val EntirePage = LoadTemplate(List("uce"))

  final val TextField = EntirePage.extract("template-text")

  final val StepTemplate = EntirePage.extract("template-step")
  // TODO This is clearly not a template
  final val StepLevelAttribute = "data-lvl"
  final val StepLevelAttributeCss = s".step [$StepLevelAttribute]"

  final val AddTailStepTemplate = EntirePage.extract("template-courses-addTailStep")
  final val AddTailStepClass = "addTailStep"
  final val AddStepTemplate = ".steps * " #> StepTemplate

  private def ExtractStepTemplate(name: String) = AddStepTemplate(EntirePage.extract(name))

  final val NormalCourseTemplate = ExtractStepTemplate("template-courses-n")
  final val AlternateCourseTemplate = ExtractStepTemplate("template-courses-a")
  final val AlternateCourseAddTailStepCss = s".courses-a .$AddTailStepClass"

  final val ExceptionCourseTemplate = ExtractStepTemplate("template-courses-e")
  final val ExceptionCourseAddTailStepCss = s".courses-e .$AddTailStepClass"

}
