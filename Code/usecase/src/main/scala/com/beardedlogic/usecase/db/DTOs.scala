package com.beardedlogic.usecase
package db

import lib.ExternalId
import lib.Types._

// NOTE: These fields names need to match the attributes in list.html
case class UseCaseSummary(
  eid: UseCaseIdentEI,
  number: UseCaseNumber,
  title: String,
  updatedAt: String) {

  def this(id: UseCaseIdentId, number: UseCaseNumber, title: String, updatedAt: String) =
    this(ExternalId.UseCase(id), number, title, updatedAt)

  def this(ucr: UseCaseRev, updatedAt: String) =
    this(ucr.identId, ucr.ident.number, ucr.header.title, updatedAt)

  def parseId = ExternalId.UseCase.parseO(eid)
}

