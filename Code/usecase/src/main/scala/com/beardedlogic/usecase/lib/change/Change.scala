package com.beardedlogic.usecase.lib.change

import scalaz.NonEmptyList
import com.beardedlogic.usecase.lib.Types._

trait Change {
  val asOnlyChange = NonEmptyList(this)
  def @:[V](newValue: V) = Changed(newValue, asOnlyChange)
  def +(otherChange: Change): NonEmptyList[Change] = NonEmptyList(this, otherChange)
  def +:(otherChange: Change): NonEmptyList[Change] = NonEmptyList(otherChange, this)
  def ++(otherChanges: NonEmptyList[Change]): NonEmptyList[Change] = this <:: otherChanges
  def ++(otherChanges: List[Change]): NonEmptyList[Change] = NonEmptyList.nel(this, otherChanges)
}

trait ChangeResponder[+V] {
  def respondToChange(c: Change)(implicit stepsAndLabels: StepAndLabelBiMap): ChangeResult[V, Change]
}
