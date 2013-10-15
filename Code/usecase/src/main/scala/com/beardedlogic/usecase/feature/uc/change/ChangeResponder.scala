package com.beardedlogic.usecase.feature.uc.change

import com.beardedlogic.usecase.feature.uc.UcParsingCtx

trait ChangeResponder[+V] {
  def respondToChange(c: Change)(implicit ctx: UcParsingCtx): ChangeResult[V, Change]
}
