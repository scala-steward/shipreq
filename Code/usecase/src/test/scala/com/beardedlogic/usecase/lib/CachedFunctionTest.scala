package com.beardedlogic.usecase.lib

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class CachedFunctionTest extends FunSpec with ShouldMatchers {

  class Inc {
    var i = 0
    def next = { i += 1; i }
    def add(a: Int) = { i += a; i }
  }

  // TODO reenable CachedFunction after refactoring
  /*
  describe("CachedFunction") {

    it("should have a consistent hashcode") {
      val i = new Inc
      val cf = new CachedFunction(666, i.next)
      val h = cf.hashCode
      cf.hashCode should be(h)
      cf.refresh
      cf.hashCode should be(h)
    }
    it("basics (via constructor)") {
      val i = new Inc
      val cf = new CachedFunction(666, i.next)
      cf.get should be(666)
      cf.get should be(666)
      cf.refresh should be(1)
      cf.get should be(1)
      cf.refresh should be(2)
      cf.get should be(2)
    }
    it("basics (via apply)") {
      val i = new Inc
      val cf = CachedFunction(i.next)
      cf.get should be(666)
      cf.get should be(666)
      cf.refresh should be(1)
      cf.get should be(1)
      cf.refresh should be(2)
      cf.get should be(2)
    }
    it("dependent copy") {
      val i = new Inc
      val r = new CachedFunction(666, i.next)
      val cc = r.dependentCopy
      cc.get should be(666)
      r.refresh should be(1)
      cc.get should be(666)
      cc.refresh should be(1)
      cc.get should be(1)
    }
  }

  describe("CachedFunction1") {
    it("basics") {
      val i = new Inc
      val cf = new CachedFunction1[Int, Int](666, i.add _)
      cf.get should be(666)
      cf.get should be(666)
      cf.refresh(3) should be(3)
      cf.get should be(3)
      cf.refresh(4) should be(7)
      cf.get should be(7)
    }
  }
  */
}
