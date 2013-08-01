package com.beardedlogic.usecase
package model

import scala.slick.jdbc.{GetResult, SetParameter, StaticQuery => Q}
import scala.slick.session.PositionedParameters
import lib._
import db.DBHelpers._
import DbOpResult._
import ExternalId.{toExternal, toInternal}

case class UseCaseRec(
  value: PlainValue[DataType.UseCase],
  header: UseCaseHeader,
  fieldListId: Long) extends Value[DataType.UseCase] {

  @inline final def dataId = value.dataId
  @inline final def valueId = value.valueId

  def stateEquals(that: UseCaseRec): Boolean =
    this.header == that.header &&
    this.fieldListId == that.fieldListId

  def withTitle(newTitle: String) =
    if (header.title == newTitle) this
    else copy(header = header.copy(title = newTitle))
}

// These fields names need to match the attributes in list.html
case class UseCaseSummary(
  dataEid: String,
  valueEid: String,
  number: Short,
  title: String,
  updatedAt: String) {
  def dataId = toInternal(dataEid)
  def valueId = toInternal(valueEid)
}
object UseCaseSummary {
  def apply(dataId: Long,
    valueId: Long,
    number: Short,
    title: String,
    updatedAt: String) = new UseCaseSummary(toExternal(dataId), toExternal(valueId), number, title, updatedAt)
}

// ---------------------------------------------------------------------------------------------------------------------

object UseCaseAccessor {
  implicit val GetResultPlainValue = ValueAccessor.GetValueResult[DataType.UseCase]
  implicit val GetResultUseCase = GetResult(r => UseCaseRec(GetResultPlainValue(r), UseCaseHeader(r.<<, r.<<), r.<<))
  implicit val GetResultUseCaseSummary = GetResult(r => UseCaseSummary(r.nextLong, r.nextLong, r.nextShort, r.nextString, r.nextString))

  implicit object SetParameterUseCase extends SetParameter[UseCaseRec] {
    def apply(v: UseCaseRec, pp: PositionedParameters) {
      pp.setLong(v.valueId)
      pp.setString(v.header.title)
      pp.setShort(v.header.number)
      pp.setLong(v.fieldListId)
    }
  }

  val Insert = Q.update[UseCaseRec]("INSERT INTO usecase VALUES(?,?,?,?)")

  val NextNumber = "select coalesce(max(number),0)+1 from usecase"
  val InsertNext = Q.query[(Long, String, Long), Short](s"INSERT INTO usecase VALUES(?,?,($NextNumber),?) RETURNING number")

  private val fieldSelection = s"v.${ValueAccessor.*}, title, number, field_list_id"

  private val selectSql = s"SELECT $fieldSelection FROM value v, usecase u WHERE u.id=? AND v.id = u.id"
  val Select = Q.query[Long, UseCaseRec](selectSql)
  val Select2 = Q.query[(Long, Long), UseCaseRec](s"$selectSql AND v.data_id=?")

  private def SelectLatestSql(dataIdSql: String) = s"""
    with history as (
      select id, rev, data_id, row_number() over (order by rev desc) rn
      from value
      where data_id = $dataIdSql
    )
    select $fieldSelection
    from history v, usecase u
    where v.id = u.id
      and v.rn = 1
    """.sql

  val SelectLatestByDataId = Q.query[Long, UseCaseRec](SelectLatestSql("?"))
  val SelectLatestByValueId = Q.query[Long, UseCaseRec](SelectLatestSql("(select data_id from value where id = ?)"))

  private def SelectSummariesSql(innerCond: String) = {
    val latestRevs = new LatestRevSubquery().where(innerCond).withTableAlias("t")
    s"""
      ${latestRevs.toWithClause}
      select data_id, v.id, number, title, to_iso8601_str(updated_at)
      from usecase u, value v
      where ${latestRevs.applyWithTableAsValueIdFilter("u.id")}
      and u.id=v.id
      order by number
      """.sql
  }
  val SelectSummaries = Q.queryNA[UseCaseSummary](SelectSummariesSql(
    s"data_id in (select id from data where type_id = ${DataType.UseCase.ordinal})"))
  val SelectSummary = Q.query[Long, UseCaseSummary](SelectSummariesSql(s"data_id=?"))

  val UpdateTitleDirectly = Q.update[(String, Long)]("UPDATE usecase SET title=? WHERE id=?")
}

trait UseCaseAccessor extends DatabaseAccessor {
  self: DAO =>

  import UseCaseAccessor._

  /** Creates a single `usecase` row. Doesn't create a new `value`. */
  def createUseCase(value: PlainValue[DataType.UseCase], header: UseCaseHeader, fieldList: FieldListRec): UseCaseRec = {
    val uch = InputCorrection.correct(header)
    val uc = UseCaseRec(value, uch, fieldList.valueId)
    createCorrectedUseCase(uc)
    uc
  }

  /** Creates a single `usecase` row. Doesn't create a new `value`. */
  private def createCorrectedUseCase(uc: UseCaseRec): Unit = Insert.execute(uc)

  // TODO New-UC has GLOBAL scope.
  // TODO New-UC: Use table locking for mutex?
  // TODO New-UC: Lacking appropriate number uniqueness constraint
  def createInitialUseCase(title: String, fieldList: FieldListRec): UseCaseRec = withTransaction {
    // TODO need a usecase state so we can call correct() instead of correctUseCaseTitle(). Would also make stateEquals() redundant
    val correctedTitle = InputCorrection.useCaseTitle(title)
    val v = createInitialValue(DataType.UseCase)
    val number = InsertNext.first(v.valueId, correctedTitle, fieldList.valueId)
    UseCaseRec(v, UseCaseHeader(correctedTitle, number), fieldList.valueId)
  }

  def findUseCase(valueId: Long): Option[UseCaseRec] = Select.firstOption(valueId)
  def findUseCase(dataId: Long, valueId: Long): Option[UseCaseRec] = Select2.firstOption(valueId, dataId)

  def findLatestUseCase(uc: UseCaseRec): Option[UseCaseRec] = findLatestUseCaseByDataId(uc.dataId)
  def findLatestUseCaseByDataId(dataId: Long): Option[UseCaseRec] = SelectLatestByDataId.firstOption(dataId)
  def findLatestUseCaseByValueId(valueId: Long): Option[UseCaseRec] = SelectLatestByValueId.firstOption(valueId)

  def findUseCaseSummary(uc: UseCaseRec): Option[UseCaseSummary] = SelectSummary.firstOption(uc.dataId)
  def findAllUseCaseSummaries(): List[UseCaseSummary] = SelectSummaries.list

  /**
   * Updates the header of an existing use case (ie. just the contents of the `usecase` table ignoring its relations).
   *
   * When updating just the title of an Untitled rev #1 UC, the update is direct. In all other cases requiring an
   * update, a new revision is created.
   *
   * @param tgtUseCase An existing use case with updated values.
   * @return A result indicator, and a resulting `UseCaseRec` if successful. Possible results are:
   *         AlreadyUpToDate, DirectUpdate, NewRevision, StaleRevision.
   */
  def updateUseCaseHeader(tgtUseCase: UseCaseRec): DbOpResult[UseCaseRec] = withTransaction {
    // TODO locking? race conditions here? ensure DB mutex

    val h = InputCorrection.correct(tgtUseCase.header)
    val tgt = tgtUseCase.copy(header = h)
    findLatestUseCase(tgt) match {
      // NOP
      case Some(latest) if tgt.stateEquals(latest) =>
        Success(AlreadyUpToDate, latest)

      // Rev #1 title update
      case Some(latest) if latest.value.rev == 1 && tgt.withTitle(Defaults.Title).stateEquals(latest) => {
        UpdateTitleDirectly.execute(tgt.header.title, tgt.valueId)
        Success(DirectUpdate, tgt)
      }

      // Audited Update (ensuring not stale)
      case Some(latest) if latest.valueId == tgt.valueId => {
        val newValue = createValue(tgt.value, LatestRev)
        val newUc = tgt.copy(value = newValue)
        createCorrectedUseCase(newUc)
        propagateRelations(latest, newUc)
        Success(NewRevision, newUc)
      }

      case _ => StaleRevision
    }
  }
}
