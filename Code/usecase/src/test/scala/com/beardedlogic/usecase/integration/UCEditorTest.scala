package com.beardedlogic.usecase.integration

import com.beardedlogic.usecase.test.SeleniumDSL
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

/**
 * Tests the use case editor.
 *
 * @since 29/04/2013
 */
class UCEditorTest extends WordSpec with ShouldMatchers with SeleniumDSL {

  "The Use Case Editor" should {

    "start blank" in {
      uce.load.assertStepCount(2)
    }

    "add new steps" in {
      uce.load.clickAdd(0).assertStepCount(3)
    }

  }
}
