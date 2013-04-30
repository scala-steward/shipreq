package com.beardedlogic.usecase.test

import SeleniumTestSupport.SeleniumDriver
import org.scalatest.Suite
import org.scalatest.matchers.ShouldMatchers
import scala.collection.JavaConversions._

/**
 * Provides tests with Selenium-based DSLs.
 * 
 * @since 30/04/2013
 */
trait SeleniumDSL extends SeleniumTestSupport { this: Suite =>
  import SeleniumDSL._

  def uce = new UCEditorDSL(s)
}

/**
 * @since 30/04/2013
 */
object SeleniumDSL {
  import SeleniumTestSupport.SeleniumDriver

  /**
   * @since 30/04/2013
   */
  class UCEditorDSL(val s: SeleniumDriver) extends ShouldMatchers with TestHelpers {

    // Action
    def load = { s.get(Jetty.URL); this }
    def assertStepCount(expected: Int) = { expectSoon { stepCount should be(expected.toString) }; this }
    def clickAdd(row: Int) = { addButton(row).click; this }

    // Inspection
    def stepCount = s.findElementById("total_steps").getText
    def addButton(row: Int) = s.findElementsByCssSelector("input[value=Add]")(row)
  }
}

