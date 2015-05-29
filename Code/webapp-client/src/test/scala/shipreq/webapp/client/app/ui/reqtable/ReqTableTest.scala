package shipreq.webapp.client.app.ui.reqtable

import japgolly.nyaya._
import japgolly.nyaya.test._
import japgolly.nyaya.test.PropTestOps._
import japgolly.scalajs.react.test._
import org.scalajs.dom.html
import scalaz.Equal
import scalaz.syntax.equal._
import utest.TestSuite
import ReactTestUtils.Simulate

import shipreq.webapp.base.data._
import shipreq.webapp.client.app.ui.Checkbox
import shipreq.base.util.Debug._
import shipreq.base.util.ScalaExt._
import shipreq.base.util.UnivEq.{apply => _, force => _, _}
import shipreq.webapp.base.test.{ActionTester, SampleProject3}
import shipreq.webapp.base.test.BaseTestUtil._
import shipreq.webapp.client.lib._
import shipreq.webapp.client.test.{DomZipper, PrepareEnv}
import shipreq.webapp.client.util._

object ReqTableTest extends TestSuite {
  PrepareEnv()

  val project = SampleProject3.project

  lazy val c = ReactTestUtils renderIntoDocument ReqTable.WIP(project)

  // ===================================================================================================================
  class Screen {
    lazy val $ = new DomZipper(c.getDOMNode())

    object viewSettings {
      lazy val $ = Screen.this.$(">div")

      object columns {
        lazy val entirety: Vector[(On, String)] =
          $("tbody tr")(2, ">td", 0)(">ol").collectD("li", li =>
            (On <~ li("input").as[html.Input].checked, li("label span").innerHTML))

        lazy val allColumns: Vector[String] =
          entirety.map(_._2)

        lazy val onColumns: Vector[String] =
          entirety.filter(_._1 :: On).map(_._2)
      }

      object sorting {
        lazy val $ = viewSettings.this.$("tbody tr")(2, ">td", 1)

        private val all = (SortMethod.ignoreBlanks ++ SortMethod.considerBlanks).whole
        private val readSortMethod: String => Option[SortMethod] = {
          case "Unused" => None
          case s => all.find(_.optionLabel == s).fold(sys error s"Unknown sort method: $s")(Some(_))
        }

        private val readSortMethodIB: String => SortMethod.IgnoreBlanks =
          s => SortMethod.ignoreBlanks.whole.find(_.optionLabel == s).getOrElse(sys error s"Unknown sort method: $s")

        lazy val inconclusive: Vector[(Option[SortMethod], String)] =
          $("ol").collectD("li", li =>
            (li("select").selectedOptionText |> readSortMethod, li("select + span").innerHTML))

        lazy val conclusiveOrder: SortMethod.IgnoreBlanks =
          $(2, "ol+div select", 0).selectedOptionText |> readSortMethodIB

        lazy val conclusiveColumnSelected: String =
          $(2, "ol+div select", 1).selectedOptionText

        lazy val conclusiveColumns: Vector[String] =
          $(2, "ol+div select", 1) collectInnerHTML "option"

        lazy val visibleColumns: Vector[String] =
          inconclusive.map(_._2) ++ conclusiveColumns
      }

      object filterDead {
        lazy val $ = viewSettings.this.$(">label input")

        lazy val value: FilterDead =
          Checkbox.filterDeadChecked <~ $.as[html.Input].checked
      }
    }
    object table {
      lazy val $ = Screen.this.$(">table")

      lazy val  columns: Vector[String] =
        $(">thead") collectInnerHTML "th span"
    }

    def availCols = viewSettings.columns.allColumns
  }

  def * = new Screen

  // ===================================================================================================================
  // Properties

  case class PS(project: Project, screen: Screen) {
    lazy val cfname = CustomField.nameP(project)

    def customFieldNames(a: Alive): Set[String] = {
      val cfs   = project.fields.data.customFields.values.toStream
      val names = cfs.filter(_.alive ≟ a).map(cfname(_).unmust)
      names.toSet
    }
  }

  val builtInColumns = Column.builtInValues.map(Column.NameResolver.builtIn).toSet.whole

  val invariants: Prop[PS] = {
    type S = Screen
    implicit def autoContraS(p: Prop[S]): Prop[PS] = p.contramap[PS](_.screen)
    def equal(name: => String) = Prop.equal[S](name)


    val availableColumns = {
      val uniqueColumns =
        Prop.distinct("Unique columns", (_: S).availCols)

      val builtInColumnsAlwaysAvailable =
        Prop.allPresent[S]("Built-in columns always available")(_ => builtInColumns, _.availCols)

      val aliveCustomFieldColumnsAlwaysAvailable =
        Prop.allPresent[PS]("Alive custom field columns available")(_ customFieldNames Alive, _.screen.availCols)

      val deadColumnsAvailable =
        Prop.allPresent[PS]("Dead custom field columns available")(_ customFieldNames Dead, _.screen.availCols)

      val deadColumnsNotAvailable =
        Prop.blacklist[PS]("Dead custom field columns not available")(_ customFieldNames Dead, _.screen.availCols)

      val deadColumns =
        equal("")(_.viewSettings.filterDead.value, _ => ShowDead).ifelse(deadColumnsAvailable, deadColumnsNotAvailable)

      aliveCustomFieldColumnsAlwaysAvailable & builtInColumnsAlwaysAvailable & deadColumns & uniqueColumns
    }

    val sortableColumns = equal("Sortable columns = selected VS columns")(
      _.viewSettings.sorting.visibleColumns.sorted, _.viewSettings.columns.onColumns.sorted)

    val tableColumns = equal("Table columns = selected VS columns")(
      _.table.columns, _.viewSettings.columns.onColumns)

    availableColumns ==> (sortableColumns & tableColumns)
  }

  def assertInvariants(s: Screen = new Screen): Unit =
    invariants assert PS(project, s)

  // ===================================================================================================================
  // Actions

  object ScreenAction extends ActionTester {
    override protected type S          = Screen
    override protected def newState    = new Screen
    override protected def defaultLast = assertInvariants
  }
  import ScreenAction._

  val filterDeadToggle =
    Action(Simulate change _.viewSettings.filterDead.$.get)
      .focus(_.viewSettings.filterDead.value)
      .assertChange

  val filterDeadShowHide = (
    filterDeadToggle.unless(_.viewSettings.filterDead.value :: HideDead) >>
    filterDeadToggle.times(2).focus(_.viewSettings.columns.onColumns).assertNoChange
  )

  def applyViewSettings(vs: ViewSettings) =
    Action exec c.modState(_ updateVS vs)

  def actionProp[A](f: A => Action[_]): Prop[A] = {
    Prop.atom("action", a =>
      try {
        run(f(a))
        None
      } catch {
        case e: Throwable => Some(e.getMessage)
      }
    )
  }

  // ===================================================================================================================
  // Tests

  implicit val settings = DefaultSettings.propSettings.setSampleSize(8) //.setDebug

  import utest.TestableSymbol
  override def tests = TestSuite {
    c setState ReqTable.initialState(project)

    'initialState -
      assertInvariants()

    'filterDead {

      'addDeadCols - run(
        filterDeadToggle.assertAfter(ShowDead).focus(_.availCols.length).assertDelta(2) >>
        filterDeadToggle.focus(_.availCols.length).assertDelta(-2)
      )

      'restoresOnCols {
        RandomReqTableData.viewSettings(project) mustSatisfy
          actionProp(applyViewSettings(_) >> filterDeadShowHide)
      }
    }

    // randomise view settings > turn off = vs1.alive
    // randomise view settings > turn on  = vs1 + dead
  }
}
