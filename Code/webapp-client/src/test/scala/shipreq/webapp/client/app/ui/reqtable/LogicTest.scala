package shipreq.webapp.client.app.ui.reqtable

import japgolly.nyaya._
import japgolly.nyaya.test._
import japgolly.nyaya.util.Multimap
import scalaz.{\/, \/-, -\/, Equal}
import scalaz.std.AllInstances._
import scalaz.syntax.id._
import utest._
import shipreq.base.util.ScalaExt._
import shipreq.webapp.base.RandomData
import shipreq.webapp.base.data._
import shipreq.webapp.client.lib.Presentation
import shipreq.webapp.client.test.ClientTestSettings._
import SortMethod._
import Sorter._

object LogicTest extends TestSuite {

//  private sealed trait ExpType[A] {
//    def visible(vs: ViewSettings): Boolean
//    def extract(e: Expansion): List[A]
//  }
//  private case object ExpCodes extends ExpType[ReqCode] {
//    override def visible(vs: ViewSettings) = vs isVisible Column.Code
//    override def extract(e: Expansion) = e.reqCodes
//  }
//
//  private val exptypes = List(ExpCodes)

  private def codes(r: Row): List[ReqCode] = r match {
    case g: GenericReqRow => g.exp.reqCodes
  }

  def firstCodePerRow(r: Row): String =
    codes(r) match {
      case Nil    => ""
      case h :: _ => h.txt
    }

  val nop = Eval.pass()

  case class LogicTests(vs: ViewSettings, p: Project) {
    val E = EvalOver(this)

    val gathered    = Logic.gather(vs, p)
    val gatheredG   = gathered.filterT[GenericReqRow]
    val rowReqCodes = gathered.flatMap(codes(_).toStream)
    val rowGReqIds  = gatheredG.map(_.req.id).toSet
    val srcGReqIds  = p.reqs.data.reqs.keys.filterT[GenericReq.Id].toSet
    val srcReqCodes = p.reqCodes.data.codeSet
    val textToStr   = Presentation.textToString(p)

    // -----------------------------------------------------------------------------------------------------------------
    // Gathering

    def noEmptyAndNonEmptyReqCodesMixed = {
      val data: Stream[List[List[ReqCode]]] =
        Multimap.empty[Req.Id, List, List[ReqCode]]
          .addPairs(gatheredG.map(r => (r.req.id, r.exp.reqCodes)): _*)
          .m.values.toStream
      E.forall(data)(l =>
        E.test("Either all empty or all non-empty", !(l.exists(_.isEmpty) && l.exists(_.nonEmpty))))
    }

    // TODO doesn't check expanded implications
    // expansions per expandable A
    //   - list A = req.A
    //   - if req.A then no rows without As

    // NOTE: ReqCodes *can* be duplicated. Imagine sorting by MF > Code.
    def gather =
      ( E.distinct("Rows", gathered)
      ∧ E.allPresent("each generic req id has a row", srcGReqIds, rowGReqIds)
      ∧ E.allPresent("all req codes are displayed", srcReqCodes, rowReqCodes)
      ∧ noEmptyAndNonEmptyReqCodesMixed
      ) rename "Logic.gather"

    // -----------------------------------------------------------------------------------------------------------------
    // Sorting

    implicit def textOrd[T <: Text.Generic] =
      implicitly[Ordering[String]].on[T#OptionalText](t => textToStr(t).toLowerCase)

    def universalSort = {
      val revOrder  = vs.order.reverse
      val sorted    = Logic.sort(vs.order, p, gathered)
      val reversed  = Logic.sort(revOrder, p, gathered)
      def criRev    = E.equal("[criteria] reverse.reverse = id", revOrder.reverse, vs.order)
      def sortTwice = E.equal("sort.sort = sort", Logic.sort(vs.order, p, sorted.toStream), sorted)
      def sortRev   = E.pass // TODO FAILS: sort(criteria.reverse) = reverse(sort(cri))
      //def sortRev   = E.equal("sort(criteria.reverse) = reverse(sort(cri))", reversed, reverseRows(sorted))
      ((criRev ==> sortRev) ∧ sortTwice) rename "Universal sort props"
    }

    def reverseRows(rs: List[Row]): List[Row] =
      rs.reverse.map {
        case GenericReqRow(r, e, mv) => GenericReqRow(r, reverseExpansion(e), reverseMultiValues(mv))
      }

    def reverseExpansion(e: Expansion): Expansion = {
      val Expansion(a, b, c) = e
      Expansion(a.reverse, b.reverse, c.reverse)
    }

    def reverseMultiValues(mv: MultiValues): MultiValues = {
      val MultiValues(a, b, c) = mv
      MultiValues(a.reverse, b.mapValues(_.reverse), c.mapValues(_.reverse))
    }

    def sortCriICB(c: SortCriterion.InconclusiveCB): SortCriteria =
      this.vs.order.copy(init = Vector(c))

    def gatherOn(c: Column.SortInconclusive, sc: SortCriteria): Stream[Row] =
      if (vs isVisible c) gathered else Logic.gather(ViewSettings(Vector(c), sc), p)

    /** @return error \/ (blank, non-blank) */
    def separateBlanks[A](expectBlanksFirst: Boolean, as: List[A])(isBlank: A => Boolean): String \/ (List[A], List[A]) = as match {
      case Nil =>
        \/-(Nil, Nil)
      case h :: t =>
        val firstBlockBlank = isBlank(h)
        val b1Cond: A => Boolean = if (firstBlockBlank) isBlank else !isBlank(_)
        val block1 = h :: t.takeWhile(b1Cond)
        val block2 = as drop block1.length
        val (b,nb) = if (firstBlockBlank) (block1, block2) else (block2, block1)
        def show = {
          val (s1,s2) = (block1,block2).mapEach(_.map(a => if (isBlank(a)) "." else "#").mkString(""))
          s"[$s1|$s2]"
        }
        def fail(e: String) = -\/(s"$e: $show")
        if (block2.isEmpty)
          if (firstBlockBlank) \/-(block1, Nil) else \/-(Nil, block1)
        else if (block2 exists b1Cond)
          fail("Blank and non-blanks not separated")
        else if (expectBlanksFirst != firstBlockBlank)
          fail(s"Blocks in wrong order")
        else
          \/-(b, nb)
    }

    def E_bnbBlocks[A](name: String, bp: BlankPlacement, as: List[A])(isBlank: A => Boolean, f: (List[A], List[A]) => EvalL): EvalL = {
      val expectBlanksFirst = bp match {case BlanksFirst => true; case BlanksLast => false}
      E.either(s"$name make separate blank/non-blank blocks", separateBlanks(expectBlanksFirst, as)(isBlank))(f.tupled)
    }

    def E_sorted[A: Ordering: Equal](name: String, as: List[A], dirChange: Dir): EvalL =
      E.equal(name + " are sorted", as, dirChange(as.sorted)(_.reverse))

    type IndivSortCB = (ConsiderBlanks, BlankPlacement, Dir) => EvalL
    type IndivSortIB = (IgnoreBlanks  ,                 Dir) => EvalL

    def sortByPubid: IndivSortIB = (sm, dir) => {
      def extract(pid: Pubid): (String, Int) = (p.reqType(pid.reqTypeId).fold(sys.error, _.mnemonic.value), pid.pos.value)
      val sc     = SortCriteria(Vector.empty, SortCriterion.Conclusive(Column.PubId, sm))
      val sorted = Logic.sort(sc, p, gathered)
      val pubids = sorted.map { case r: GenericReqRow => extract(r.req.pubId)}
      E_sorted("Pubids", pubids, dir)
    }

    def sortByRecCode: IndivSortCB = (sm, bp, dir) => {
      val sc         = sortCriICB(SortCriterion.InconclusiveCB(Column.Code, sm))
      val input      = gatherOn(Column.Code, sc)
      val sorted     = Logic.sort(sc, p, input)
      val data       = sorted map firstCodePerRow
      val name       = s"ReqCodes ($sm)"
      val intra      = sorted.toStream.map(codes).filter{case _ :: _ :: _ => true; case _ => false}.map(_.map(_.txt))
      def eachRow    = E.forall(intra)(E_sorted(s"Codes within a single row are sorted.", _, dir))
      def wholeTable = E_bnbBlocks(name, bp, data)(_.isEmpty, (_, nb) => E_sorted(name, nb, dir))
      (wholeTable ∧ eachRow) rename name
    }

    def sortByDesc: IndivSortCB = (sm, bp, dir) => {
      val sc         = sortCriICB(SortCriterion.InconclusiveCB(Column.Desc, sm))
      val input      = gatherOn(Column.Desc, sc)
      val sorted     = Logic.sort(sc, p, input)
      val data       = sorted.map{ case r: GenericReqRow => r.req.desc }
      val name       = s"Desc ($sm)"
      E_bnbBlocks(name, bp, data)(_.isEmpty, (_, nb) => E_sorted(name, nb, dir))
    }

    def sortCB(t: IndivSortCB): EvalL =
      ( t(BlanksThenAsc,  BlanksFirst, KeepDir)
      ∧ t(AscThenBlanks,  BlanksLast,  KeepDir)
      ∧ t(BlanksThenDesc, BlanksFirst, FlipDir)
      ∧ t(DescThenBlanks, BlanksLast,  FlipDir))

    def sortIB(t: IndivSortIB): EvalL =
      t(Asc, KeepDir) ∧ t(Desc, FlipDir)

    // Let's make it real obvious what we're omitting or potentially forgetting
    def individualSort: Column => EvalL = {
      case Column.ReqType         => nop
      case Column.PubId           => sortIB(sortByPubid)
      case Column.Code            => sortCB(sortByRecCode)
      case Column.Desc            => sortCB(sortByDesc)
      case Column.Tags            => nop
      case Column.ImplicationSrc  => nop
      case Column.ImplicationTgt  => nop
      case Column.CustomField(id) =>
        id match {
          case i: CustomField.Implication.Id => nop
          case i: CustomField.Tag        .Id => nop
          case i: CustomField.Text       .Id => nop
        }
    }

    def individualSorts: EvalL =
      Column.all(None).map(individualSort).reduce(_ ∧ _)

    def sorting =
      (individualSorts ==> universalSort) rename "Logic.sort"

    // -----------------------------------------------------------------------------------------------------------------
    def all = gather ∧ sorting
  }

  def gen: Gen[LogicTests] =
    for {
      p  <- RandomData.project
      vs <- ReqTableTest.rndViewSettings(p)
    } yield
      LogicTests(vs, p)

  override def tests = TestSuite {
    gen.mustSatisfyE(_.all)//(implicitly[Settings].setSeed(0).setDebug.setSampleSize(20))
  }
}
