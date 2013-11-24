package com.beardedlogic.shipreq.app

import org.scalatest.FunSuite
import com.beardedlogic.shipreq.test.TestHelpers
import AppSiteMap._
import Implicits._

class AppSiteMapTest extends FunSuite with TestHelpers {

   test("Home relativeUrl is /") {
     Home.relativeUrl should be("/")
   }
 }
