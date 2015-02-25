package shipreq.webapp.base.data

import japgolly.nyaya._
import japgolly.nyaya.test._
import japgolly.nyaya.test.PropTest._
import scalaz.std.AllFunctions._
import scalaz.std.AllInstances._
import utest._
import shipreq.base.util.ScalaExt._
import shipreq.webapp.base.RandomData

object ReqCodesTest extends TestSuite {
  import ReqCode._

  case class TrieProps(trie: Trie, target: Target, code: ReqCode) {
    val E          = EvalOver(this)
    val flat       = Trie.flatten(trie)
    val flatStream = Trie.flatStream(trie)

    def put = {
      val a = flat.updated(code, target)
      val n = Trie.put(trie, code)(target) |> Trie.flatten
      E.equal("put", a, n)
    }

    def createFromFlatten = {
      val n = flat.foldLeft(Trie.empty) { case (q, (c, t)) => Trie.put(q, c)(t) }
      E.equal("createFromFlatten", trie, n)
    }

    def flattenEqualsFlatStream =
      E.equal("flatten = flatStream.toMap", flat, flatStream.toMap)

    def all = "Trie props" rename_: (
      flattenEqualsFlatStream ∧ (put ==> createFromFlatten))
  }

  def gen: Gen[TrieProps] =
    for {
      targets ← RandomData.reqId.set.sup
      trie    ← RandomData.reqCodeTrie(targets).lim(10)
      target  ← Gen.newOrOld(RandomData.reqId)(targets)
      code    ← Gen.newOrOld(RandomData.reqCode)(Trie.flatStream(trie).map(_._1))
    } yield TrieProps(trie, target, code)

  override def tests = TestSuite {
    gen.mustSatisfyE(_.all)
  }
}