package shipreq.fix

import scalafix.v1._
import scala.meta._
import Helpers._

object FixShipReq {

  val OptionOrSomeApply = SymbolMatcher.exact("scala/Option.apply().", "scala/Some.apply().").ignoreErrors

  object OptionOrSomeTermName {
    def unapply(t: Term.Name) = t.value == "Option" || t.value == "Some"
  }

  val CustomFieldTagId = SymbolMatcher.exact("shipreq/webapp/base/data/CustomField.Tag.Id#")
}

class FixShipReq extends SemanticRule("FixShipReq") {
  import FixShipReq._

  override def fix(implicit doc: SemanticDocument): Patch = {
//    println("Tree.syntax\n" + doc.tree.syntax)
//    println("Tree.structure\n" + doc.tree.structure)
//    println("Tree.structureLabeled\n" + doc.tree.structureLabeled)

    doc.tree.collect {

      // {Option,Some}(arg)
      case tree @ Term.Apply(option @ OptionOrSomeTermName(), arg :: Nil) =>
        fixOptionOrSomeApply(tree, option.synthetics, arg)

      // {Option,Some}.apply(arg)
      case tree @ OptionOrSomeApply(Term.Apply(option @ Term.Select(_, _), arg :: Nil)) =>
        fixOptionOrSomeApply(tree, option.synthetics, arg)

    }.asPatch
  }

  private def fixOptionOrSomeApply(tree: Tree, option: List[SemanticTree], arg: Term): Patch = {
//    println(option.headOption.structure)
    if (option.headOption.exists(_.arg1SymbolMatches(CustomFieldTagId)))
      Patch.replaceTree(tree, arg.toString + ".some")
    else
      Patch.empty
  }
}
