package com.beardedlogic.usecase.lib.change

import com.beardedlogic.usecase.lib.Types.StepAndLabelBiMap

trait ChangeResponder[+V] {
  def respondToChange(c: Change)(implicit stepsAndLabels: StepAndLabelBiMap): ChangeResult[V, Change]
}
