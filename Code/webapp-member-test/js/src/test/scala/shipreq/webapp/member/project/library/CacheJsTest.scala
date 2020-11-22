package shipreq.webapp.member.project.library

import java.time.Instant
import scala.scalajs.js
import shipreq.webapp.base.util.LruMemo
import shipreq.webapp.member.project.data.Project
import shipreq.webapp.member.project.event._
import shipreq.webapp.member.test.WebappTestUtil.ImplicitProjectEqualityDeepExceptEventTime._
import shipreq.webapp.member.test.WebappTestUtil._
import sourcecode.Line
import utest._

object CacheJsTest extends TestSuite {

  override def tests = Tests {

    val now = Instant.now()

    def event(i: Int) =
      VerifiedEvent(
        EventOrd(i),
        Event.ProjectNameSet(i.toString),
        now.plusSeconds(i))

    def events(is: Range) =
      VerifiedEvent.NonEmptySeq.force(
        VerifiedEvent.Seq.empty ++ is.iterator.map(event))

    def projectAt(n: Int) =
      Project.empty.updateOrThrow(events(1 to n))

    val milestones: js.Array[Project] =
      new js.Array

    val milestones2: CacheJs.ArrayExt[Project] =
      milestones.asInstanceOf[CacheJs.ArrayExt[Project]]

    val retainEvery = 3

    val p9 = projectAt(9)

    var cache = new CacheJs.NonEmpty(
      latest         = p9,
      milestoneEvery = retainEvery,
      milestones     = milestones,
      lru            = LruMemo.ExternalFn.byUnivEq[Int, Project](1),
    ).update(p9).asInstanceOf[CacheJs.NonEmpty]

    def update(project: Project*): Unit = {
      cache = cache.update(project).asInstanceOf[CacheJs.NonEmpty]
    }

    def lru = cache.lru

    def inMilestoneCache(): Set[Int] =
      milestones
        .indices
        .iterator
        .filter(milestones2.get(_).isDefined)
        .map(_ * retainEvery + retainEvery)
        .toSet

    def inLruCache(): Set[Int] = {
      val b = Set.newBuilder[Int]
      lru.foreachKey(b += _)
      b.result()
    }

    def assertCaches(latest: Int)(inMilestones: Int*)(inLru: Int*)(implicit l: Line): Unit = {
      val actual = (cache.latest.ordAsInt, inMilestoneCache(), inLruCache())
      val expect = (latest, inMilestones.toSet, inLru.toSet)
      assertEq(actual, expect)
    }

    def get(ord: Int)(implicit l: Line): Project = {
      val latest = cache.latest.ordAsInt
      if (!(0 < ord && ord < latest))
        fail(s"Contract violation: 0 < ord ($ord) < latest ($latest)")
      val actual = cache(EventOrd(ord)).getOrThrow("No result for ord " + ord)
      val expect = projectAt(ord)
      assertEq(actual, expect)
      actual
    }

    assertCaches(9)(9)()
    get(5)
    assertCaches(9)(3, 9)(5)
    get(5)
    assertCaches(9)(3, 9)(5)
    get(4)
    assertCaches(9)(3, 9)(4)

    update(projectAt(15))
    assertCaches(15)(3, 9, 15)(4)
    get(4)
    assertCaches(15)(3, 9, 15)(4)

    milestones.clear()
    get(3)
    assertCaches(15)(3)(4)
    get(11)
    assertCaches(15)(3, 6, 9)(11)
    get(12)
    assertCaches(15)(3, 6, 9, 12)(11)
    get(12)
    assertCaches(15)(3, 6, 9, 12)(11)
    get(8)
    assertCaches(15)(3, 6, 9, 12)(8)
    get(9)
    assertCaches(15)(3, 6, 9, 12)(8)

    milestones.clear()
    get(13)
    assertCaches(15)(9, 12)(13)
    get(6)
    assertCaches(15)(3, 6, 9, 12)(13)
    get(4)
    assertCaches(15)(3, 6, 9, 12)(4)

    update(projectAt(15), projectAt(6), projectAt(22), projectAt(23), projectAt(21))
    assertCaches(23)(3, 6, 9, 12, 15, 21)(4)
    get(22)
    assertCaches(23)(3, 6, 9, 12, 15, 21)(22)

    update(projectAt(30), projectAt(33))
    assertCaches(33)(3, 6, 9, 12, 15, 21, 30, 33)(22)

    update(projectAt(23), projectAt(24))
    assertCaches(33)(3, 6, 9, 12, 15, 21, 24, 30, 33)(22)
  }
}
