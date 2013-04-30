package com.beardedlogic.usecase.test

import SeleniumTestSupport.SeleniumDriver
import org.openqa.selenium.By
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

    reload

    // Action
    def reload = { s.get(Jetty.URL); this }

    // Inspection
    private def steps = s.findElementsByCssSelector(".step")
    def useCaseId = s.findElementById("uc_id").getText
    def useCaseTitle = s.findElementByName("title").getText
    def stepCount = steps.size
    def stepText(row: Int) = steps(row).findElement(By.cssSelector("textarea")).getText
    def stepPosition(row: Int) = steps(row).findElement(By.cssSelector(".pos")).getText

    val lvlClassPrefix = "lvl-"
    def stepLevel(row: Int) = {
      val lvls = for (
        l <- steps(row).getAttribute("class").split("\\s+") if l.startsWith(lvlClassPrefix)
      ) yield l.replace(lvlClassPrefix, "")
      lvls should have size (1)
      val lvl = lvls(0)
      lvl should fullyMatch regex ("\\d+")
      lvl.toInt
    }
  }
}

