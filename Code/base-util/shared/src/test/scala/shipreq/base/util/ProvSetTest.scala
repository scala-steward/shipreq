package shipreq.base.util

import japgolly.microlibs.testutil.TestUtil._
import sourcecode.Line
import utest._
import nyaya.gen._
import nyaya.test.PropTest._
import utest.framework.TestPath
import shipreq.base.util.PartialOrder.Cmp

@nowarn
object ProvSetTest extends TestSuite {
  import PartialOrder.Cmp._
  import PartialOrder.ImplicitOps._
  import ProvSet.ProvEntry

  private object Internals {
    final case class K(id: String, rev: Int) {
      override val toString = s"$id$rev"
    }
    type V    = String
    type PE   = ProvEntry[K]
    type Prov = ArraySeq[ProvEntry[K]]

    implicit def univEqK: UnivEq[K] = UnivEq.derive
    implicit val keyOrder = keyedInt((_: K).id, (_: K).rev)
    val module = ProvSet.Module[K, V](_.into, _.key.toString < _.key.toString)
    import module.{ProvSet, empty}

    val parseK: String => K = {
      val regex = "^([A-Za-z]+)(\\d+)$".r
      s => {
        val regex(id, rev) = s
        K(id, rev.toInt)
      }
    }

    private val regexPE = "^(?:([A-Za-z0-9]+)(?:->|<))?([A-Za-z0-9]+)$".r

    private def parsePE(k: K, s: String): PE =
      s match {
        case regexPE(from, to) =>
          ProvEntry(
            from = Option(from).fold(k)(parseK),
            to   = parseK(to))
      }

    private val regexPS1 = "^\\{(.*?)}:\\{(.+)}$".r
    private val regexPS2 = "^\\{(.*?)}$".r

    implicit def strToProvSet(s: String): ProvSet = {

      def singleVal(id: String, prov: String): ProvSet = {
        val k = parseK(id)
        val provs = prov.split(',').iterator.filter(_ != "").map(parsePE(k, _)).toSet
        module(Map(k -> id), provs)
      }

      s match {
        case regexPS1(valuesStr, provStr) =>
          val values = valuesStr.split(',').iterator.filter(_ != "").map(s => parseK(s) -> s).toMap
          val provs = provStr.split(',').iterator.filter(_ != "").map(parsePE(null, _)).toSet
          module(values, provs)

        case regexPS2(valuesStr) =>
          val values = valuesStr.split(',').iterator.filter(_ != "").map(s => parseK(s) -> s).toMap
          module(values, Set.empty)

        case _ =>
          s.indexOf(':') match {
            case -1 => singleVal(s, "")
            case n  => singleVal(s.take(n), s.drop(n + 1))
          }
      }
    }

    def assertAdd(i: ProvSet, j: ProvSet)(expect: ProvSet)(implicit l: Line): Unit = {
      i.assertProps()
      j.assertProps()
      assertEq(s"$i + $j", i ++ j, expect)
      assertEq(s"$j + $i [reverse]", j ++ i, expect)
    }

    def assertCmp[A](x: A, y: A)(expect: Cmp)(implicit l: Line, p: PartialOrder[A]): Unit = {
      assertEq(s"$x cmp $y", p(x, y), expect)
      assertEq(s"$y cmp $x [reverse]", p(y, x), expect.flip)
    }

    private val cmpTestFmt = "^([A-Za-z0-9]+)([^A-Za-z0-9])([A-Za-z0-9]+)$".r

    def cmpTest()(implicit l: Line, tp: TestPath, po: PartialOrder[String]): Unit = {
      val cmpTestFmt(lhs, rel, rhs) = tp.value.last
      val expect: Cmp =
        rel match {
          case ">" => Greater
          case "<" => Lesser
          case "=" => Equal
          case "|" => Separate
        }
      assertCmp(lhs, rhs)(expect)
    }
  }

  // ===================================================================================================================
  import Internals._
  import module.ProvSet

  override def tests = Tests {

    "props" - {
      val Laws = new ProvSet.Laws(module)
      import Laws._

      val range = 6
      val size = 8

      val genId  = Gen.choose_!(('A' to 'Z').take(range).map(_.toString))
      val genRev = Gen.chooseInt(range)
      val genK   = Gen.lift2(genId, genRev)(K.apply)

      val getPE: Gen[ProvEntry[K]] =
        for {
          k <- genK
          j <- genK.map(j => Option.when(k isSeparateTo j)(j)).optionGet
        } yield ProvEntry(k, j)

      val genPS: Gen[ProvSet] =
        for {
          prov       <- getPE.set(0 to size)
          provSize    = prov.size
          valueKeys <- genK.arraySeq(0 to provSize)
        } yield {
          val values = valueKeys.iterator.map(k => k -> s"${k.id}${k.rev}").toMap
          module(values, prov).pruneValues
        }

      val gen: Gen[Input] =
        Gen.lift3(genPS, genPS, genPS)(ProvSet.Laws.Input(_, _, _))

//      import japgolly.microlibs.stdlib_ext.StdlibExt._
//      gen.withSeed(0).samples().take(100).drain()
//      laws.mustBeSatisfiedBy(gen.withSeed(6))
    }

    "manual" - {
      "basic" - {
        "gt"  - assertAdd("A2", "A3")("A3")
        "lt"  - assertAdd("A2", "A1")("A2")
        "eq"  - assertAdd("A2", "A2")("A2")
        "sep" - assertAdd("A2", "B3")("{A2,B3}")
      }

      "prov" - {
        "eq"     - assertAdd("A2:B3<A1", "B3")("A2:B3<A1")
        "gt"     - assertAdd("A2:B3<A1", "B2")("A2:B3<A1")
        "lt"     - assertAdd("A2:B3<A1", "B4")("{A2,B4}:{B3<A1}")
        "sep"    - assertAdd("A2:B3<A1", "C2:B2<C1")("{A2,C2}:{B3<A1,B2<C1}")
        "merge"  - assertAdd("A2:B3<A1", "B2:C6<B1")("{A2}:{B3<A1,C6<B1}")
        "cycle1" - assertAdd("A1:B1<A1", "B1:A1<B1")("{B1}:{A1<B1,B1<A1}")
        "cycle2" - assertAdd("B2:A1<B1,C0<A0", "C1:B2<C0")("C1:A1<B1,C0<A0,B2<C0")
        "cycle3" - {
          val expect: ProvSet = "{B1}:{A1<C0,A2<B1,C0<A1}"
          implicit val po = expect.partialOrder.contramap(parseK)
          "A1<A2" - cmpTest()
          "A1<B1" - cmpTest()
          "A1<C0" - cmpTest()
          "A2<B1" - cmpTest()
          "A2>C0" - cmpTest()
          "B1>C0" - cmpTest()
          "add"   - assertAdd("{B1}:{A1<C0,A2<B1}", "{A2}:{C0<A1}")(expect)
        }
        "cycle4" - {
          val expect: ProvSet = "{D2}:{B1<C0,A2<B1,C0<B1,C1<D1}"
          implicit val po = expect.partialOrder.contramap(parseK)
          "A1<A2" - cmpTest()
          "A1<B1" - cmpTest()
          "A1<C0" - cmpTest()
          "A1<C1" - cmpTest()
          "A1<D1" - cmpTest()
          "A2<B1" - cmpTest()
          "A2<C0" - cmpTest()
          "A2<C1" - cmpTest()
          "A2<D1" - cmpTest()
          "B1<C0" - cmpTest()
          "B1<C1" - cmpTest()
          "B1<D1" - cmpTest()
          "C0<C1" - cmpTest()
          "C0<D1" - cmpTest()
          "C1<D1" - cmpTest()
          "add"   - assertAdd("{B1}:{B1<C0,A2<B1,C0<B1}", "{D2}:{C1<D1}")(expect)
        }
        "cycle5" - {
          val expect: ProvSet = "{}:{B1<E3,E5<B1}"
          implicit val po = expect.partialOrder.contramap(parseK)
          "B1<E3" - cmpTest()
          "B1<E4" - cmpTest()
          "B1<E5" - cmpTest()
          "B2>B1" - cmpTest()
          "B2>E3" - cmpTest()
          "B2>E4" - cmpTest()
          "B2>E5" - cmpTest()
          "E6>B1" - cmpTest()
          "E6>E3" - cmpTest()
          "E6>E4" - cmpTest()
          "E6>E5" - cmpTest()
          "E2<B1" - cmpTest()
          "E2<E4" - cmpTest()
          "E3<E4" - cmpTest()
          "E3<E5" - cmpTest()
        }
      }

      /*
      I'M SCREWED! AGAIN!
      Potential next steps:
      1. model real usage, check if these problems can actually occur (eg. always partial with usage via proper ops)
      2. rather than pruning on ++, what about pruning as part of the ops? (eg. .merge(a,b) deletes a, not ++)
      2a. would the above require us to keep a (deleted: Set[K]) and use that to prune on (++)?
       */

      "components" - {
//        "1" - {
//          val c = "{}:{A5<B3,B5<A3}".components
//          assertSet(c.map(_.map(_.toString)), Set(
//            NonEmptySet("A3", "A5", "B3", "B5"),
//          ))
//        }
        "1" - {
          val c = "{}:{A5<B3,B5<A3,A7<B7,B1<A1}".components
          assertSet(c.map(_.map(_.toString)), Set(
            NonEmptySet("A3", "A5", "B3", "B5"),
            NonEmptySet("A1"),
            NonEmptySet("B1"),
            NonEmptySet("A7"),
            NonEmptySet("B7"),
          ))

//          val cs = "{}:{A4<B3,A5<C4,A5<F1,B1<E3,B2<F1,B3<F4,B4<D1,B5<D1,B5<D3,D2<E0,D3<C3,D4<A5,E4<D2,E5<B1,F2<B2,F3<B3}".components.filter(_.tail.nonEmpty)
          val cs = "{}:{A4<B3,A5<C4,B1<E3,B2<F1,B3<F4,B4<D1,B5<D1,B5<D3,D4<A5,E5<B1,F3<B3}".components.filter(_.tail.nonEmpty)
          cs.map(_.whole.toArray.map(_.toString).sortInPlace().mkString("[", ",", "]")).mkString("\n")
        }
      }

      "fromLaws" - {
        "1" - {
          val allProv = "{A4<B3,A5<C4,A5<F1,B1<E3,B2<F1,B3<F4,B4<D1,B5<D1,B5<D3,D2<E0,D3<C3,D4<A5,E4<D2,E5<B1,F2<B2,F3<B3}"
          val a  : ProvSet = "{F1}:{A5<C4,B3<F4,B5<D3,D4<A5}"
          val b  : ProvSet = "{A5,D4}:{A4<B3,B1<E3,B2<F1,B4<D1,B5<D1,E5<B1,F3<B3}"
          val c  : ProvSet = "{A2,B0}:{A5<F1,D2<E0,D3<C3,E4<D2,F2<B2}"
          val ab : ProvSet = "{A5}:{A4<B3,A5<C4,B1<E3,B2<F1,B3<F4,B4<D1,B5<D1,B5<D3,D4<A5,E5<B1,F3<B3}"
          val bc : ProvSet = "{D4}:{A4<B3,A5<F1,B1<E3,B2<F1,B4<D1,B5<D1,D2<E0,D3<C3,E4<D2,E5<B1,F2<B2,F3<B3}"
          val abc: ProvSet = "{F1}:" + allProv

          "ab"   - assertAdd(a, b)(ab)
          "bc"   - assertAdd(b, c)(bc)
          "ab_c" - assertAdd(ab, c)(abc)
          "a_bc" - assertAdd(a, bc)(abc)
          "abc"  - assertEq(s"{F1,A5,D4,A2,B0}:$allProv".pruneValues, abc)
        }
      }
    }

  }
}
