package com.beardedlogic.usecase.snippet

import net.liftweb.util.Helpers._
import com.beardedlogic.usecase.app.RequestVars

object ProjectNavbar {
  def render = {
    val project = RequestVars.SoleProject.get
    ".navbar .active a *" #> project.name
  }
}
