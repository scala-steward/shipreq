package com.beardedlogic.usecase.lib

object Misc {

  /**
   * Run running single tests from the IDE. the run-mode is still development. This changes it to test.
   */
  def ensureTestModeDuringTests() {
    if((new Exception).getStackTrace.toList.find(_.getClassName.contains("scalatest")).isDefined)
      System.setProperty("run.mode", "test")
  }
}