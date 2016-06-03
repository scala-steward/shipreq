package shipreq.webapp.server.util

import java.time._
import org.scalatest.{Matchers, FunSpec}

class MicroCacheTest extends FunSpec with Matchers {

  def fn: () => Int = {
    var i = 0
    () => {i += 1; i}
  }

  describe("CacheVar") {
    def newCacheVar(policy: CachePolicy[Any]) = {
      val f = fn
      val c = CacheVar[Int](policy)
      new {def getOrSet(): Any = c.getOrSet(f())}
    }

    it("NeverExpire policy: should always return the same value") {
      val c = newCacheVar(NeverExpire)
      c.getOrSet shouldBe 1
      c.getOrSet shouldBe 1
      c.getOrSet shouldBe 1
    }

    it("DisableCache policy: should always return a new value") {
      val c = newCacheVar(DisableCache)
      c.getOrSet shouldBe 1
      c.getOrSet shouldBe 2
      c.getOrSet shouldBe 3
    }
  }

  describe("CacheFn") {
    def newCacheFn(policy: CachePolicy[Any]) = { val f = fn; CacheFn[Int](f())(policy) }

    it("NeverExpire policy: should always return the same value") {
      val c = newCacheFn(NeverExpire)
      c.value shouldBe 1
      c.value shouldBe 1
      c.value shouldBe 1
    }
    it("DisableCache policy: should always return a new value") {
      val c = newCacheFn(DisableCache)
      c.value shouldBe 1
      c.value shouldBe 2
      c.value shouldBe 3
    }
  }

  describe("ExpireAfter Policy") {
    it("should expire stuff after the given time limit") {
      val ttl = Duration.ofMinutes(5)
      var now = Instant.now()
      val p = ExpireAfter(ttl, () => now)
      val t = p.write(())
      p.expired(t) shouldBe false
      now = now plusSeconds 299
      p.expired(t) shouldBe false
      now = now plusSeconds 2
      p.expired(t) shouldBe true
    }
  }

}
