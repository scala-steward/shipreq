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

  def parseId = ExternalId.UseCase.parseO(eid)
}
object UseCaseSummary {
  def as(id: UseCaseIdentId, number: UseCaseNumber, title: String, updatedAt: String): UseCaseSummary =
    apply(ExternalId.UseCase(id), number, title, updatedAt)

  def as(ucr: UseCaseRev, updatedAt: String): UseCaseSummary =
    as(ucr.identId, ucr.ident.number, ucr.header.title, updatedAt)
}