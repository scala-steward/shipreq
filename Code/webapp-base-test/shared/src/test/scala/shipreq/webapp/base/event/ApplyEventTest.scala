package shipreq.webapp.base.event

import scalaz.{\/-, -\/}
import utest._
import shipreq.base.util.NonEmpty
import shipreq.base.util.ScalaExt._
import shipreq.base.util.UnivEq._
import shipreq.webapp.base.AppConsts
import shipreq.webapp.base.data._
import shipreq.webapp.base.test.BaseTestUtil._
import shipreq.webapp.base.test.UnsafeTypes._
import DeletionAction._
import ApplyEventTestFns._

object ApplyEventTestFns {

  val tooLongStr = "a" * (AppConsts.largeTextMaxLength + 1)

  // TODO Move to Project once Rev is removed
  val emptyProject = {
    import DataImplicits._
    val rev = Rev(0)
    implicit def autoRevAnd[D](d: D): RevAnd[D] = RevAnd(rev, d)

    val cit = emptyDataMap(CustomIssueType)
    val crt = emptyDataMap(CustomReqType)
    val fs  = FieldSet(emptyDataMap(CustomField), StaticField.values.whole)
    val tt  = TagTree.empty
    val cfg = ProjectConfig(cit, crt, fs, tt)

    val reqs     = Requirements.empty
    val reqCodes = ReqCodes.empty
    val reqText  = ReqData.emptyText
    val reqTags  = ReqData.emptyTags
    val reqImps  = Implications.empty

    Project(cfg, reqs, reqCodes, reqText, reqTags, reqImps)
  }

  val apply = new ApplyEvent()(Untrusted)

  def fmtEvents(es: Seq[Event]): String = {
    val t = es.length
    es.zipWithIndex.map { case (e, i) => s"[${i + 1}/$t] $e" } mkString "\n"
  }

  def assertPass(es: Event*): Unit =
    _assertPass(es: _*)

  def _assertPass(es: Event*): Project = {
    val r = apply(es) run emptyProject
    val p =
      r match {
        case \/-(v) => v
        case -\/(e) => fail(s"\nPass expected but failed with '$e'.\nEvents were:\n${fmtEvents(es)}")
      }
    assertQty(p, es: _*)
    p
  }

  def assertFail(errFrag: String)(es: Event*): Unit = {
    val r = apply(es) run emptyProject
    r match {
      case -\/(e) => assert(e contains errFrag)
      case \/-(_) => fail(s"\nFailure expected but didn't occur.\nEvents were:\n${fmtEvents(es)}")
    }
  }

  def assertQty(p: Project, es: Event*): Unit = {
    var customReqTypes = 0
    var atags          = 0
    var tagGroups      = 0
    def ifHard(d: DeletionAction, f: => Unit): Unit =
      if (d == HardDel) f
    es foreach {
      case _: CreateCustomReqType => customReqTypes += 1
      case _: CreateTagGroup      => tagGroups += 1
      case _: CreateApplicableTag => atags += 1
      case DeleteCustomReqType(_, d) => ifHard(d, customReqTypes -= 1)
      case DeleteApplicableTag(_, d) => ifHard(d, atags -= 1)
      case DeleteTagGroup     (_, d) => ifHard(d, tagGroups -= 1)
      case _: UpdateCustomReqType
         | _: UpdateApplicableTag
         | _: UpdateTagGroup => ()
    }
    val actualAtags = p.config.atags.size
    assertEq("CustomReqTypes", p.config.customReqTypes.data.size, customReqTypes)
    assertEq("ApplicableTags", actualAtags, atags)
    assertEq("TagGroups",      p.config.tags.data.size - actualAtags, tagGroups)
  }
}

// =====================================================================================================================
object CustomReqTypeEventTest extends TestSuite {
  import CustomReqTypeGD._

  implicit class CreateCustomReqTypeExt(private val a: CreateCustomReqType) extends AnyVal {
    def mod(f: Values => Values) =
      a.copy(vs = NonEmpty.force(f(a.vs.value)))
  }

  val mfName = "Major Feature"
  val c1  = CreateCustomReqType(1, nev(Mnemonic("MF"), Name(mfName), Imp(ImplicationRequired)))
  val c2  = CreateCustomReqType(2, nev(Mnemonic("FR"), Name("Functional Req"), Imp(ImplicationRequired.Not)))
  val u1  = UpdateCustomReqType(1, nev(Mnemonic("M")))
  val sd1 = DeleteCustomReqType(1, SoftDel)
  val hd1 = DeleteCustomReqType(1, HardDel)
  val r1  = DeleteCustomReqType(1, Restore)

  override def tests = TestSuite {
    'create {
      'one      - assertPass(c1)
      'two      - assertPass(c1, c2)
      'needName - assertFail("Name")          (c1.mod(_ - Name))
      'needMne  - assertFail("Mnemonic")      (c1.mod(_ - Mnemonic))
      'needImp  - assertFail("Imp")           (c1.mod(_ - Imp))
      'badId    - assertFail(" id ")          (c1.copy(id = -1))
      'badName  - assertFail("blank")         (c1.mod(_ + Name("")))
      'badMne   - assertFail("Mnemonic")      (c1.mod(_ + Mnemonic("?")))
      'dupName  - assertFail("unique")        (c1, c2.mod(_ + Name(mfName)))
      'dupMne   - assertFail("unique")        (c1, c2.mod(_ + Mnemonic("MF")))
      'dupId    - assertFail("already exists")(c1, c2.copy(id = c1.id))
    }

    'update {
      'ok - {
        var es = Vector(c1, u1)
        def r = _assertPass(es: _*).config.customReqTypes.data.get(1).get
        assertEq(r, CustomReqType(1, "M", Set("MF"), mfName, ImplicationRequired, Live))

        es :+= UpdateCustomReqType(1, nev(Mnemonic("X"), Name("xxx")))
        assertEq(r, CustomReqType(1, "X", Set("MF", "M"), "xxx", ImplicationRequired, Live))

        es :+= UpdateCustomReqType(1, nev(Mnemonic("MF"), Imp(ImplicationRequired.Not)))
        assertEq(r ,CustomReqType(1, "MF", Set("M", "X"), "xxx", ImplicationRequired.Not, Live))
      }
      'notFound - assertFail("not found")(u1)
      'dead     - assertFail("dead")     (c1, sd1, u1)
      'badName  - assertFail("blank")    (c1, UpdateCustomReqType(1, nev(Name(""))))
      'badMne   - assertFail("Mnemonic") (c1, UpdateCustomReqType(1, nev(Mnemonic("?"))))
      'dupName  - assertFail("unique")   (c1, c2, UpdateCustomReqType(2, nev(Name(mfName))))
      'dupMne   - assertFail("unique")   (c1, c2, UpdateCustomReqType(2, nev(Mnemonic("MF"))))
    }

    'delete {
      'okHard    - assertPass(c1, hd1)
      'okSoft    - assertPass(c1, sd1)
      'okRest    - assertPass(c1, sd1, r1)
      'okMulti   - assertPass(c1, sd1, r1, sd1, r1, hd1)
      'notFound  - List(hd1, sd1, r1).foreach(d => assertFail("not found")(d))
      'hardTwice - assertFail("not found")(c1, hd1, hd1)
      'hardRest  - assertFail("not found")(c1, hd1, r1)

      // Disabling for now. These are NOP issues, not integrity issues.
      // 'softTwice - assertFail("x")(c1, sd1, sd1)
      // 'restTwice - assertFail("x")(c1, sd1, r1, r1)
      // 'restLive  - assertFail("x")(c1, r1)

      // TODO Add tests of HardDeletion failing when subject in use
    }
  }
}

// =====================================================================================================================
object TagGroupEventTest extends TestSuite {
  import TagGroupGD._

  implicit class CreateTagGroupExt(private val a: CreateTagGroup) extends AnyVal {
    def mod(f: Values => Values) =
      a.copy(vs = NonEmpty.force(f(a.vs.value)))
  }

  def child(id: TagGroupId) = Children(Vector(id))
  def parent(id: TagGroupId) = Parents(Map((id: TagId) -> none))
  def ttget(tt: TagTree, ids: TagGroupId*): List[TagInTree] = ids.toList.map(i => tt.get(i).get)

  val c1Name = "Version"
  val c1 = CreateTagGroup(1, nev(Name(c1Name), Desc(None), MutexChildren(false)))
  val c2 = CreateTagGroup(2, nev(Name("Released"), Desc(Some("r")), MutexChildren(true), parent(1)))
  val c3 = CreateTagGroup(3, nev(Name("All"), Desc(None), MutexChildren(false), child(1)))
  val u1 = UpdateTagGroup(1, nev(Desc(Some("versionness"))))
  val List(hd1,hd2,hd3,hd4) = List(1,2,3,4).map(DeleteTagGroup(_, HardDel))
  val List(sd1,sd2,sd3,sd4) = List(1,2,3,4).map(DeleteTagGroup(_, SoftDel))
  val List( r1, r2, r3, r4) = List(1,2,3,4).map(DeleteTagGroup(_, Restore))

  override def tests = TestSuite {
    'create {
      'one   - assertPass(c1)
      'two   - assertPass(c1, c2)
      'three - {
        val a = _assertPass(c1, c2, c3)
        val b = _assertPass(c1, c3, c2)
        assertEq(a, b)
      }
      'needName          - assertFail("Name")          (c1.mod(_ - Name))
      'needMC            - assertFail("Mutex")         (c1.mod(_ - MutexChildren))
      'badId             - assertFail(" id ")          (c1.copy(id = -1))
      'badName           - assertFail("blank")         (c1.mod(_ + Name("")))
      'badDesc           - assertFail("Desc")          (c1.mod(_ + Desc(Some(tooLongStr))))
      'badChildNotFound  - assertFail("")              (c1.mod(_ + child(2)))
      'badParentNotFound - assertFail("")              (c1.mod(_ + parent(2)))
      'badChildSelf      - assertFail("")              (c1.mod(_ + child(1)))
      'badParentSelf     - assertFail("")              (c1.mod(_ + parent(1)))
      'badCycle          - assertFail("Cycle")         (c1, c2.mod(_ + child(1)))
      'dupId             - assertFail("already exists")(c1, c2.copy(id = c1.id))
      'dupName           - assertFail("unique")        (c1, c2.mod(_ + Name(c1Name)))
      // c/p to dead subject = bad?
    }

    'update {
      'ok - {
        var es = Vector(c1, u1)
        def r1 = _assertPass(es: _*).config.tags.data.get(1.TG).get
        def r2 = _assertPass(es: _*).config.tags.data.get(2.TG).get
        assertEq(r1, TagInTree(TagGroup(1, c1Name, Some("versionness"), false, Live), Vector.empty))

        es :+= c2
        es :+= UpdateTagGroup(1, nev(Name("Ver"), MutexChildren(true)))
        assertEq(r1, TagInTree(TagGroup(1, "Ver", Some("versionness"), true, Live), Vector(2.TG)))
        assertEq(r2, TagInTree(TagGroup(2, "Released", Some("r"), true, Live), Vector.empty))

        // TODO confirm parent order
      }

      'notFound          - assertFail("not found")(u1)
      'dead              - assertFail("dead")     (c1, sd1, u1)
      'badName           - assertFail("blank")    (c1, UpdateTagGroup(1, nev(Name(""))))
      'badDesc           - assertFail("Desc")     (c1, UpdateTagGroup(1, nev(Desc(Some(tooLongStr)))))
      'badChildNotFound  - assertFail("")         (c1, UpdateTagGroup(1, nev(child(2))))
      'badParentNotFound - assertFail("")         (c1, UpdateTagGroup(1, nev(parent(2))))
      'badChildSelf      - assertFail("")         (c1, UpdateTagGroup(1, nev(child(1))))
      'badParentSelf     - assertFail("")         (c1, UpdateTagGroup(1, nev(parent(1))))
      'badCycle          - assertFail("Cycle")    (c1, c2, UpdateTagGroup(1, nev(parent(2))))
      'dupName           - assertFail("unique")   (c1, c2, UpdateTagGroup(2, nev(Name(c1Name))))
      // c/p to dead subject = bad?
    }

    'delete {
      'okHard    - assertPass(c1, hd1)
      'okSoft    - assertPass(c1, sd1)
      'okRest    - assertPass(c1, sd1, r1)
      'okMulti   - assertPass(c1, sd1, r1, sd1, r1, hd1)
      'notFound  - List(hd1, sd1, r1).foreach(d => assertFail("not found")(d))
      'hardTwice - assertFail("not found")(c1, hd1, hd1)
      'hardRest  - assertFail("not found")(c1, hd1, r1)

      'delRest1 {
        var es = Vector[Event](c1)
        def test(e: Event, ab: String): Unit = {
          es :+= e
          val t = _assertPass(es: _*).config.tags.data
          val List(a, b) = ttget(t, 1, 2)
          assertEq((a, b).mapEach(_.children), (Vector(2.TG), Vector.empty))
          def f(d: TagInTree, s: String) = if (d.tag.live :: Live) s else "-"
          assertEq(f(a, "A") + f(b, "B"), ab)
        }

        test(c2,  "AB")
        test(sd2, "A-") // softdel with live parents
        test(r2,  "AB") // restore with live parents
        test(sd2, "A-") // softdel with live parents
        test(sd1, "--") // softdel with dead children
        test(r2,  "-B") // restore with dead parents
        test(sd2, "--") // softdel with dead parents
        test(sd1, "--") // softdel with live children (sole parent)
        test(r1,  "AB") // restore with live children (sole parent)
      }

      'delRest2 {
        val c3 = CreateTagGroup(3, nev(Name("C"), Desc(None), MutexChildren(false), child(2)))
        var es = Vector[Event](c1, c2)
        def test(e: Event, acb: String): Unit = {
          es :+= e
          val t = _assertPass(es: _*).config.tags.data
          val List(a, b, c) = ttget(t, 1, 2, 3)
          assertEq((a, c, b).mapEach(_.children), (Vector(2.TG), Vector(2.TG), Vector.empty))
          def f(d: TagInTree, s: String) = if (d.tag.live :: Live) s else "-"
          assertEq("[" + f(a, "A") + f(c, "C") + "]" + f(b, "B"), acb)
        }

        test(c3,  "[AC]B")
        test(sd1, "[-C]B") // softdel with live children (other live parents)
        test(sd3, "[--]-") // softdel with live children (last live parents)
        test(r1,  "[A-]-") // restore with dead children (other dead parents)
        test(r3,  "[AC]B") // restore with dead children (last dead parent)
        test(sd3, "[A-]B") // softdel with live children (other live parents)
        test(r3,  "[AC]B") // restore with live children
      }

      'delRest3 {
        val cC = CreateTagGroup(3, nev(Name("C"), Desc(None), MutexChildren(false), parent(2)))
        val cD = CreateTagGroup(4, nev(Name("D"), Desc(None), MutexChildren(false), child(2)))
        var es = Vector[Event](c1, c2, cC)
        def test(e: Event, state: String): Unit = {
          es :+= e
          val t = _assertPass(es: _*).config.tags.data
          val List(a, b, c, d) = ttget(t, 1, 2, 3, 4)
          assertEq((a, d, b, c).mapEach(_.children), (Vector(2.TG), Vector(2.TG), Vector(3.TG), Vector.empty))
          def f(d: TagInTree, s: String) = if (d.tag.live :: Live) s else "-"
          assertEq("[" + f(a, "A") + f(d, "D") + "]" + f(b, "B") + f(b, "C"), state)
        }
        test(cD,  "[AD]BC")
        test(sd1, "[-D]BC")
        test(sd4, "[--]--")
        test(r4,  "[-D]--")
        test(r1,  "[AD]BC")
      }

      'delRest4 {
        val cC = CreateTagGroup(3, nev(Name("C"), Desc(None), MutexChildren(false), parent(2)))
        val cD = CreateTagGroup(4, nev(Name("D"), Desc(None), MutexChildren(false), child(3)))
        var es = Vector[Event](c1, c2, cC)
        def test(e: Event, state: String): Unit = {
          es :+= e
          val t = _assertPass(es: _*).config.tags.data
          val List(a, b, c, d) = ttget(t, 1, 2, 3, 4)
          assertEq((a, b, c, d).mapEach(_.children), (Vector(2.TG), Vector(3.TG), Vector.empty, Vector(3.TG)))
          def f(d: TagInTree, s: String) = if (d.tag.live :: Live) s else "-"
          assertEq("{" + f(a, "A") + f(b, "B") + "," + f(d, "D") + "}" + f(c, "C"), state)
        }
        test(cD,  "{AB,D}C")
        test(sd1, "{--,D}C")
        test(sd4, "{--,-}-")
        test(r4,  "{--,D}-")
        test(r1,  "{AB,D}C")
      }

      // TODO HardDeletion: If tag in use in [project content]     , prevent hard delete
      // TODO HardDeletion: If tag in use in [tag tree]            , it should be allowed
      // TODO HardDeletion: If tag in use in [other project config], should it should be allowed?
//      'hardTree {
//        def test(es: Event*) = assertFail("??")((c1 :: c2 :: es.toList): _*)
//        test(hd1) // live child
//        test(hd2) // live parent
//        test(hd2) // dead child
//      }

      // Disabling for now. These are NOP issues, not integrity issues.
      // 'softTwice - assertFail("x")(c1, sd1, sd1)
      // 'restTwice - assertFail("x")(c1, sd1, r1, r1)
      // 'restLive  - assertFail("x")(c1, r1)
    }
  }
}

// =====================================================================================================================
object ApplicableTagEventTest extends TestSuite {
  import ApplicableTagGD._

  implicit class CreateApplicableTagExt(private val a: CreateApplicableTag) extends AnyVal {
    def mod(f: Values => Values) =
      a.copy(vs = NonEmpty.force(f(a.vs.value)))
  }

  def child(id: ApplicableTagId) = Children(Vector(id))
  def parent(id: ApplicableTagId) = Parents(Map((id: TagId) -> none))
  def ttget(tt: TagTree, ids: ApplicableTagId*): List[TagInTree] = ids.toList.map(i => tt.get(i).get)

  val c1Name = "Version"
  val c1 = CreateApplicableTag(1, nev(Name(c1Name), Desc(None), Key("c1")))
  val c2 = CreateApplicableTag(2, nev(Name("Released"), Desc(Some("r")), Key("c2"), parent(1)))
  val c3 = CreateApplicableTag(3, nev(Name("All"), Desc(None), Key("c3"), child(1)))
  val u1 = UpdateApplicableTag(1, nev(Desc(Some("versionness"))))
  val List(hd1,hd2,hd3,hd4) = List(1,2,3,4).map(DeleteApplicableTag(_, HardDel))
  val List(sd1,sd2,sd3,sd4) = List(1,2,3,4).map(DeleteApplicableTag(_, SoftDel))
  val List( r1, r2, r3, r4) = List(1,2,3,4).map(DeleteApplicableTag(_, Restore))

  override def tests = TestSuite {
    'create {
      'one   - assertPass(c1)
      'two   - assertPass(c1, c2)
      'three - {
        val a = _assertPass(c1, c2, c3)
        val b = _assertPass(c1, c3, c2)
        assertEq(a, b)
      }
      'needName          - assertFail("Name")          (c1.mod(_ - Name))
      'needKey           - assertFail("Key")           (c1.mod(_ - Key))
      'badId             - assertFail(" id ")          (c1.copy(id = -1))
      'badName           - assertFail("blank")         (c1.mod(_ + Name("")))
      'badDesc           - assertFail("Desc")          (c1.mod(_ + Desc(Some(tooLongStr))))
      'badChildNotFound  - assertFail("")              (c1.mod(_ + child(2)))
      'badParentNotFound - assertFail("")              (c1.mod(_ + parent(2)))
      'badChildSelf      - assertFail("")              (c1.mod(_ + child(1)))
      'badParentSelf     - assertFail("")              (c1.mod(_ + parent(1)))
      'badCycle          - assertFail("Cycle")         (c1, c2.mod(_ + child(1)))
      'dupId             - assertFail("already exists")(c1, c2.copy(id = c1.id))
      'dupName           - assertFail("unique")        (c1, c2.mod(_ + Name(c1Name)))
      'dupKey            - assertFail("unique")        (c1, c2.mod(_ + Key("c1")))
      // c/p to dead subject = bad?
    }

    'update {
      'ok - {
        var es = Vector(c1, u1)
        def r1 = _assertPass(es: _*).config.tags.data.get(1.AT).get
        def r2 = _assertPass(es: _*).config.tags.data.get(2.AT).get
        assertEq(r1, TagInTree(ApplicableTag(1, c1Name, Some("versionness"), "c1", Live), Vector.empty))

        es :+= c2
        es :+= UpdateApplicableTag(1, nev(Name("Ver"), Key("c=one")))
        assertEq(r1, TagInTree(ApplicableTag(1, "Ver", Some("versionness"), "c=one", Live), Vector(2.AT)))
        assertEq(r2, TagInTree(ApplicableTag(2, "Released", Some("r"), "c2", Live), Vector.empty))

        // TODO confirm parent order
      }

      'notFound          - assertFail("not found")(u1)
      'dead              - assertFail("dead")     (c1, sd1, u1)
      'badName           - assertFail("blank")    (c1, UpdateApplicableTag(1, nev(Name(""))))
      'badDesc           - assertFail("Desc")     (c1, UpdateApplicableTag(1, nev(Desc(Some(tooLongStr)))))
      'badChildNotFound  - assertFail("")         (c1, UpdateApplicableTag(1, nev(child(2))))
      'badParentNotFound - assertFail("")         (c1, UpdateApplicableTag(1, nev(parent(2))))
      'badChildSelf      - assertFail("")         (c1, UpdateApplicableTag(1, nev(child(1))))
      'badParentSelf     - assertFail("")         (c1, UpdateApplicableTag(1, nev(parent(1))))
      'badCycle          - assertFail("Cycle")    (c1, c2, UpdateApplicableTag(1, nev(parent(2))))
      'dupName           - assertFail("unique")   (c1, c2, UpdateApplicableTag(2, nev(Name(c1Name))))
      'dupKey            - assertFail("unique")   (c1, c2, UpdateApplicableTag(2, nev(Key("c1"))))
      // c/p to dead subject = bad?
    }

    'delete {
      'okHard    - assertPass(c1, hd1)
      'okSoft    - assertPass(c1, sd1)
      'okRest    - assertPass(c1, sd1, r1)
      'okMulti   - assertPass(c1, sd1, r1, sd1, r1, hd1)
      'notFound  - List(hd1, sd1, r1).foreach(d => assertFail("not found")(d))
      'hardTwice - assertFail("not found")(c1, hd1, hd1)
      'hardRest  - assertFail("not found")(c1, hd1, r1)

      'delRest1 {
        var es = Vector[Event](c1)
        def test(e: Event, ab: String): Unit = {
          es :+= e
          val t = _assertPass(es: _*).config.tags.data
          val List(a, b) = ttget(t, 1, 2)
          assertEq((a, b).mapEach(_.children), (Vector(2.AT), Vector.empty))
          def f(d: TagInTree, s: String) = if (d.tag.live :: Live) s else "-"
          assertEq(f(a, "A") + f(b, "B"), ab)
        }

        test(c2,  "AB")
        test(sd2, "A-") // softdel with live parents
        test(r2,  "AB") // restore with live parents
        test(sd2, "A-") // softdel with live parents
        test(sd1, "--") // softdel with dead children
        test(r2,  "-B") // restore with dead parents
        test(sd2, "--") // softdel with dead parents
        test(sd1, "--") // softdel with live children (sole parent)
        test(r1,  "AB") // restore with live children (sole parent)
      }

      'delRest2 {
        val c3 = CreateApplicableTag(3, nev(Name("C"), Desc(None), Key("c3"), child(2)))
        var es = Vector[Event](c1, c2)
        def test(e: Event, acb: String): Unit = {
          es :+= e
          val t = _assertPass(es: _*).config.tags.data
          val List(a, b, c) = ttget(t, 1, 2, 3)
          assertEq((a, c, b).mapEach(_.children), (Vector(2.AT), Vector(2.AT), Vector.empty))
          def f(d: TagInTree, s: String) = if (d.tag.live :: Live) s else "-"
          assertEq("[" + f(a, "A") + f(c, "C") + "]" + f(b, "B"), acb)
        }

        test(c3,  "[AC]B")
        test(sd1, "[-C]B") // softdel with live children (other live parents)
        test(sd3, "[--]-") // softdel with live children (last live parents)
        test(r1,  "[A-]-") // restore with dead children (other dead parents)
        test(r3,  "[AC]B") // restore with dead children (last dead parent)
        test(sd3, "[A-]B") // softdel with live children (other live parents)
        test(r3,  "[AC]B") // restore with live children
      }

      'delRest3 {
        val cC = CreateApplicableTag(3, nev(Name("C"), Desc(None), Key("c"), parent(2)))
        val cD = CreateApplicableTag(4, nev(Name("D"), Desc(None), Key("d"), child(2)))
        var es = Vector[Event](c1, c2, cC)
        def test(e: Event, state: String): Unit = {
          es :+= e
          val t = _assertPass(es: _*).config.tags.data
          val List(a, b, c, d) = ttget(t, 1, 2, 3, 4)
          assertEq((a, d, b, c).mapEach(_.children), (Vector(2.AT), Vector(2.AT), Vector(3.AT), Vector.empty))
          def f(d: TagInTree, s: String) = if (d.tag.live :: Live) s else "-"
          assertEq("[" + f(a, "A") + f(d, "D") + "]" + f(b, "B") + f(b, "C"), state)
        }
        test(cD,  "[AD]BC")
        test(sd1, "[-D]BC")
        test(sd4, "[--]--")
        test(r4,  "[-D]--")
        test(r1,  "[AD]BC")
      }

      'delRest4 {
        val cC = CreateApplicableTag(3, nev(Name("C"), Desc(None), Key("c"), parent(2)))
        val cD = CreateApplicableTag(4, nev(Name("D"), Desc(None), Key("d"), child(3)))
        var es = Vector[Event](c1, c2, cC)
        def test(e: Event, state: String): Unit = {
          es :+= e
          val t = _assertPass(es: _*).config.tags.data
          val List(a, b, c, d) = ttget(t, 1, 2, 3, 4)
          assertEq((a, b, c, d).mapEach(_.children), (Vector(2.AT), Vector(3.AT), Vector.empty, Vector(3.AT)))
          def f(d: TagInTree, s: String) = if (d.tag.live :: Live) s else "-"
          assertEq("{" + f(a, "A") + f(b, "B") + "," + f(d, "D") + "}" + f(c, "C"), state)
        }
        test(cD,  "{AB,D}C")
        test(sd1, "{--,D}C")
        test(sd4, "{--,-}-")
        test(r4,  "{--,D}-")
        test(r1,  "{AB,D}C")
      }

      // TODO HardDeletion: If tag in use in [project content]     , prevent hard delete
      // TODO HardDeletion: If tag in use in [tag tree]            , it should be allowed
      // TODO HardDeletion: If tag in use in [other project config], should it should be allowed?
//      'hardTree {
//        def test(es: Event*) = assertFail("??")((c1 :: c2 :: es.toList): _*)
//        test(hd1) // live child
//        test(hd2) // live parent
//        test(hd2) // dead child
//      }

      // Disabling for now. These are NOP issues, not integrity issues.
      // 'softTwice - assertFail("x")(c1, sd1, sd1)
      // 'restTwice - assertFail("x")(c1, sd1, r1, r1)
      // 'restLive  - assertFail("x")(c1, r1)
    }
  }
}
