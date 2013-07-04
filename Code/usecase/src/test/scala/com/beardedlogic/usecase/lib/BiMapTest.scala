package com.beardedlogic.usecase.lib

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import com.beardedlogic.usecase.util.BiMapBuilder

class BiMapTest extends FunSuite with ShouldMatchers {

  test("Adding & retrieving") {
    val b = new BiMapBuilder[String,Int]
    b += ("Three" -> 3)
    b("Two") = 2
    val m = b.result
    m.ba(3) should be("Three")
    m.ab("Two") should be(2)
  }
}