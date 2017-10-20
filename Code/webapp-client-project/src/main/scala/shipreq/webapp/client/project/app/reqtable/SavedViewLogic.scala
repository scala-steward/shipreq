package shipreq.webapp.client.project.app.reqtable

import japgolly.microlibs.nonempty.NonEmptyVector
import japgolly.univeq._
import shipreq.webapp.base.data.{FilterDead, Project}
import shipreq.webapp.base.data.reqtable._
import SavedView.Id

object SavedViewLogic {

  sealed trait Menu {
    protected def unsortedItems: NonEmptyVector[Menu.Item]
    def isActive: Menu.Item => Boolean

    final val items: NonEmptyVector[Menu.Item] =
      unsortedItems.sortBy(_.name.toUpperCase)

    assert(items.whole.count(isActive) == 1)
  }

  object Menu {
    case object NoSaved extends Menu {
      override protected def unsortedItems = NonEmptyVector.one(Item.Unsaved(NonEmptyVector.one(Action.SaveAsNew)))
      override def isActive = _ => true
    }

    final case class SavedClean(default: Item.Default, nonDefaults: Set[Item.NonDefault], active: Id) extends Menu {
      override protected def unsortedItems = NonEmptyVector(default, nonDefaults.toVector)
      override def isActive = _.optionId.exists(_ ==* active)
    }

    final case class SavedDirty(default: Item.Default, nonDefaults: Set[Item.NonDefault], unsaved: Item.Unsaved) extends Menu {
      override protected def unsortedItems = NonEmptyVector(default, nonDefaults.toVector :+ unsaved)
      override def isActive = _.optionId.isEmpty
    }

    sealed trait Item {
      def optionId: Option[Id]
      def name: String
      def default: Boolean
      def actions: NonEmptyVector[Action]
    }

    object Item {
      final case class Unsaved(actions: NonEmptyVector[Action.Unsaved]) extends Item {
        override def optionId = None
        override def name = "Unsaved view" // TODO Prohibit in name validation
        override def default = false
      }

      sealed trait Saved extends Item {
        def id: Id
        final override val optionId = Some(id)
        override def actions: NonEmptyVector[Action.Saved]
      }

      final case class Default(id: Id, name: String) extends Saved {
        override def default = true
        def actions: NonEmptyVector[Action.Saved] = Action.default
      }

      final case class NonDefault(id: Id, name: String) extends Saved {
        override def default = false
        def actions: NonEmptyVector[Action.Saved] = Action.nonDefault
      }

      def dirty(saved: SavedView): Unsaved =
        Unsaved(NonEmptyVector(Action.SaveAsNew, Action.Replace(saved.id, saved.name.value)))

      def default(v: SavedView): Default =
        Default(v.id, v.name.value)

      def nonDefault(v: SavedView): NonDefault =
        NonDefault(v.id, v.name.value)
    }

    sealed trait Action
    object Action {
      sealed trait Unsaved extends Action
      sealed trait Saved   extends Action

      case object SaveAsNew                          extends Unsaved
      case object Rename                             extends Saved
      case object Delete                             extends Saved
      case object SetAsDefault                       extends Saved
      final case class Replace(id: Id, name: String) extends Unsaved

      val default: NonEmptyVector[Action.Saved] =
        NonEmptyVector(Rename, Delete)

      val nonDefault: NonEmptyVector[Action.Saved] =
        NonEmptyVector(SetAsDefault, Rename, Delete)
    }

    implicit def univEqActionU: UnivEq[Action.Unsaved ] = UnivEq.derive
    implicit def univEqItemD  : UnivEq[Item.Default   ] = UnivEq.derive
    implicit def univEqItemND : UnivEq[Item.NonDefault] = UnivEq.derive
    implicit def univEqItemS  : UnivEq[Item.Saved     ] = UnivEq.derive
    implicit def univEqItemU  : UnivEq[Item.Unsaved   ] = UnivEq.derive
    implicit def univEq       : UnivEq[Menu           ] = UnivEq.derive

    def determine(savedViews       : SavedViews.Optional)
                 (currentFilterDead: FilterDead,
                  currentSettings  : TableSettings,
                  lastSelected     : Option[Id]): Menu =
      savedViews match {
        case None =>
          NoSaved
        case Some(svs) =>
          val active      = lastSelected.flatMap(svs.get).getOrElse(svs.default)
          val clean       = (currentFilterDead ==* active.filterDead) && (currentSettings ==* toTableSettingsDirect(active))
          val default     = Item.default(svs.default)
          val nonDefaults = svs.nonDefault.valuesIterator.map(Item.nonDefault).toSet
          if (clean)
            SavedClean(default, nonDefaults, active.id)
          else
            SavedDirty(default, nonDefaults, Item.dirty(active))
      }
  }

  // ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████

  def toTableSettingsDirect(v: SavedView): TableSettings =
    TableSettings(v.columns, v.sortCriteria, v.filter)

//  def toTableSettingsAdaptive(v: SavedView, p: Project): TableSettings =
//    // handle removed/dead stuff
//    ???

}
