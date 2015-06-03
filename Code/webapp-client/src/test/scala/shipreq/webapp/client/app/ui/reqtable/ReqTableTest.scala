package shipreq.webapp.client.app.ui.reqtable

import japgolly.nyaya._
import japgolly.nyaya.test._
import japgolly.nyaya.test.PropTestOps._
import japgolly.scalajs.react._
import japgolly.scalajs.react.test._
import org.scalajs.dom, dom.html
import scalajs.js
import scalaz.Equal
import scalaz.std.option._
import scalaz.syntax.equal._
import utest.TestSuite
import ReactTestUtils.Simulate

import shipreq.webapp.base.data._
import shipreq.webapp.client.app.ui.{Style, Checkbox}
import shipreq.base.util.Debug._
import shipreq.base.util.ScalaExt._
import shipreq.base.util.UnivEq.{apply => _, force => _, _}
import shipreq.webapp.base.test.{ActionTester, SampleProject3}
import shipreq.webapp.base.test.BaseTestUtil._
import shipreq.webapp.client.lib._
import shipreq.webapp.client.test.{DomZipper, PrepareEnv}
import shipreq.webapp.client.test.ReactTmpExt._
import shipreq.webapp.client.test.TestUtil.fakeKeyboardEvent
import shipreq.webapp.client.util._

object ReqTableTest extends TestSuite {
  PrepareEnv()

  val project = SampleProject3.project

  lazy val c = ReactTestUtils renderIntoDocument ReqTable.WIP(project)

  lazy val cTable = Table.Component castM ReactTestUtils.findRenderedComponentWithType(c, Table.Component.jsCtor)

  def reset(): Unit =
    c setState ReqTable.initialState(project)

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
      lazy val tbody = $(">tbody")

      lazy val columns: Vector[String] =
        $(">thead") collectInnerHTML "th span"

      import ColumnRenderer.{Status, Normal, DeadRow}

      private def cell(s: Status, focus: Boolean): String =
        "td." + Style.reqtable.cell(s, focus).className.value

      private def row(inner: String): String =
        s">tr:has($inner)"

      private def byFocus(focus: Boolean, wrap: String => String): String =
        ColumnRenderer.statusDomain.toStream.map(s => wrap(cell(s, focus))).mkString(",")

      private def byStatus(s: Status, wrap: String => String): String =
        Vector(true, false).map(f => wrap(cell(s, f))).mkString(",")

      lazy val allRows   = tbody getAll ">tr"
      lazy val deadRows  = tbody getAll byStatus(DeadRow, row)
      lazy val aliveRows = tbody getAll byStatus(Normal, row)
      lazy val focusRow  = tbody option byFocus(true, row)
      lazy val focus     = tbody option byFocus(true, identity)

      lazy val inputsInFocusRow: Option[Int] =
        focusRow.map(_.getAll("input,select,textarea").length)

      def ensureHasFocus(): Unit =
        focus getOrElse fail("No focus.")
    }

    def availCols = viewSettings.columns.allColumns
  }

  def * = new Screen

  // ===================================================================================================================
  // Properties

  // TODO Rename and move into Nyaya
  @inline def existance[A](name: String) = new ExistanceB[A](name)
  final class ExistanceB[A](val name: String) extends AnyVal {
    def apply[B](expect: A => Boolean, expected: A => Set[B], testData: A => Traversable[B]): Prop[A] = {
      lazy val yes = Prop.allPresent[A](name + " available")(expected, testData)
      lazy val no = Prop.blacklist[A](name + " not available")(expected, testData)
      Prop.test[A](name, expect).ifelse(yes, no)
    }
  }

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

      val deadColumns =
        existance[PS]("Dead custom field columns")(_.screen.viewSettings.filterDead.value :: ShowDead,
          _ customFieldNames Dead, _.screen.availCols)

      aliveCustomFieldColumnsAlwaysAvailable & builtInColumnsAlwaysAvailable & deadColumns & uniqueColumns
    }

    val sortableColumns = equal("Sortable columns = selected VS columns")(
      _.viewSettings.sorting.visibleColumns.sorted, _.viewSettings.columns.onColumns.sorted)

    val tableColumns = equal("Table columns = selected VS columns")(
      _.table.columns, _.viewSettings.columns.onColumns)

    val tableContents: Prop[PS] = {
      val rowEitherDeadOrAlive = equal("Rows are either dead or alive")(
        _.table.allRows.length,
        t => t.table.aliveRows.length + t.table.deadRows.length)

      rowEitherDeadOrAlive
    }

    availableColumns & sortableColumns & tableColumns & tableContents
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

  val filterDeadToggle =
    Action(Simulate change _.viewSettings.filterDead.$.get)
      .focus(_.viewSettings.filterDead.value)
      .assertChange

  def setFilterDead(fd: FilterDead): Action[Unit] =
    filterDeadToggle.unless(_.viewSettings.filterDead.value == fd)

  val filterDeadShowHide =
    setFilterDead(HideDead) >>
    filterDeadToggle.times(2).focus(_.viewSettings.columns.onColumns).assertNoChange

  def applyViewSettings(vs: ViewSettings): Action[Unit] =
    Action exec c.modState(_ updateVS vs)

  def focusRow(alive: Alive, cellIndex: Int = 0) =
    Action { s =>
      val row = alive match {
        case Alive => DomZipper.first("Alive row", s.table.aliveRows)
        case Dead  => DomZipper.first("Dead row", s.table.deadRows)
      }
      val cell = row.getAll(">td")(cellIndex)
      Simulate.click(cell)
    }

  val F2 = fakeKeyboardEvent(keyCode = 113, target = dom.document.body)

  val editFocused = Action { s =>
    s.table.ensureHasFocus()
    cTable.backend._onKeyDown(F2)
    cTable.backend._onKeyUp(F2)
  }

  // ===================================================================================================================
  // Tests

  implicit val settings = DefaultSettings.propSettings.setSampleSize(8) //.setDebug

  import utest.TestableSymbol
  override def tests = TestSuite {
    reset()

    'initialState -
      assertInvariants()

    'dead {

      'addDeadCols - run(
        filterDeadToggle.assertAfter(ShowDead).focus(_.availCols.length).assertDelta(2) >>
        filterDeadToggle.focus(_.availCols.length).assertDelta(-2)
      )

      'restoresOnCols {
        RandomReqTableData.viewSettings(project) mustSatisfy
          actionProp(applyViewSettings(_) >> filterDeadShowHide)
      }

      'noEditing {
        val colCount = *.availCols.length

        val showAllColumns = applyViewSettings {
          val s  = c.state
          val vs = s.viewSettings
          val cn = Column.NameResolver.byProject(s.project)
          val cs = Column.all(cn.customFields.values)
          val o  = vs.order.copy(init = Vector.empty) // remove ReqCodeGroups
          vs.copy(columns = cs, order = o, filterDead = ShowDead)
        }

        def editAllColumns(rowType: Alive): Action[Int] = {
          val editEachCell =
            (0 until colCount).map { c =>
              focusRow(rowType, c).focus(_.table.focus).assertChange >> editFocused
            }.reduce(_ >> _)

          (showAllColumns >> editEachCell).focus(_.table.inputsInFocusRow getOrElse 0)
        }

        editAllColumns(Dead).assertAfter(0).run()

        // Ensure our test logic works
        reset()
        editAllColumns(Alive).testAfter(_ > 0, "[Alive Row] Cells should be in edit-mode").run()
      }
    }

  }
}
