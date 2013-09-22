package com.beardedlogic.usecase
package db

import lib.ExternalId
import lib.Types._

// NOTE: These fields names need to match the attributes in list.html
case class UseCaseSummary(
  eid: UseCaseIdentEI,
  number: Short,
  title: String,
  updatedAt: String) {

  def this(id: UseCaseIdentId, number: Short, title: String, updatedAt: String) =
    this(ExternalId.UseCase(id), number, title, updatedAt)

  def this(uc: UseCaseRev, updatedAt: String) =
    this(uc.identId, uc.header.number, uc.header.title, updatedAt)

  def parseId = ExternalId.UseCase.parseO(eid)
}

