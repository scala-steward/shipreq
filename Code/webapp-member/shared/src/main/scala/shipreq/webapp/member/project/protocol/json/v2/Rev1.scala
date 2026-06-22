package shipreq.webapp.member.project.protocol.json.v2

import io.circe._
import io.circe.syntax._
import japgolly.microlibs.adt_macros.AdtMacros
import shipreq.base.util.JsonUtil._
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.event.RetiredGenericData._
import shipreq.webapp.member.project.event._
import shipreq.webapp.member.project.filter.Filter
import shipreq.webapp.member.protocol.json.JsonCodec
import shipreq.webapp.member.protocol.json.JsonCodec.Implicits._

object Rev1 {
  import shipreq.webapp.member.project.protocol.json.v1.BaseData._
  import shipreq.webapp.member.project.protocol.json.v1.BaseMemberData1._
  import shipreq.webapp.member.project.protocol.json.v1.BaseMemberData1.SavedViewCodecs._
  import shipreq.webapp.member.project.protocol.json.v1.Events._
  import shipreq.webapp.member.project.protocol.json.v1.Events.EventData._
  import shipreq.webapp.member.project.protocol.json.v1.PostEvents._
  import shipreq.webapp.member.project.protocol.json.v1.Rev1._
  import shipreq.webapp.member.project.protocol.json.v1.Rev1.EventData._
  import shipreq.webapp.member.project.protocol.json.v1.Rev1.SavedViewCodecs._
  import shipreq.webapp.member.project.protocol.json.v1.Rev4._
  import shipreq.webapp.member.project.protocol.json.v1.Rev6.AtomCodecs.instances._
  import shipreq.webapp.member.project.protocol.json.v1.Rev6.EventData._
  import shipreq.webapp.member.project.protocol.json.v1.Rev6._
  import shipreq.webapp.member.project.protocol.json.v2.Rev0._
  import shipreq.webapp.member.project.protocol.json.v2.Rev0.EventData._
  import this.EventData._
  import this.SavedViewCodecs._

  // ===================================================================================================================
  // Project data

  implicit lazy val keyDecoderCustomFieldNumberId: KeyDecoder[CustomField.Number.Id] =
    KeyDecoder.decodeKeyInt.map(CustomField.Number.Id.apply)

  implicit lazy val keyEncoderCustomFieldNumberId: KeyEncoder[CustomField.Number.Id] =
    KeyEncoder.encodeKeyInt.contramap(_.value)

  implicit lazy val codecCustomFieldNumberId: JsonCodec[CustomField.Number.Id] =
    codecTaggedI(CustomField.Number.Id)

  implicit lazy val decoderCustomFieldId: Decoder[CustomFieldId] = decodeSumBySoleKey {
    case ("imp" , c) => c.as[CustomField.Implication.Id]
    case ("num" , c) => c.as[CustomField.Number.Id]
    case ("tag" , c) => c.as[CustomField.Tag.Id]
    case ("text", c) => c.as[CustomField.Text.Id]
  }

  implicit lazy val encoderCustomFieldId: Encoder[CustomFieldId] = Encoder.instance {
    case a: CustomField.Implication.Id => Json.obj("imp"  -> a.asJson)
    case a: CustomField.Number.Id      => Json.obj("num"  -> a.asJson)
    case a: CustomField.Tag.Id         => Json.obj("tag"  -> a.asJson)
    case a: CustomField.Text.Id        => Json.obj("text" -> a.asJson)
  }

  implicit lazy val decoderFieldId: Decoder[FieldId] = decodeSumBySoleKeyOrConst[FieldId](
    "stepsNA"   -> StaticField.NormalAltStepTree,
    "stepsE"    -> StaticField.ExceptionStepTree,
    "impGraph"  -> StaticField.ImplicationGraph,
    "stepGraph" -> StaticField.StepGraph,
    "otherTags" -> StaticField.OtherTags,
    "allTags"   -> StaticField.AllTags,
  ) {
    case ("imp" , c) => c.as[CustomField.Implication.Id]
    case ("num" , c) => c.as[CustomField.Number.Id]
    case ("tag" , c) => c.as[CustomField.Tag.Id]
    case ("text", c) => c.as[CustomField.Text.Id]
  }

  implicit lazy val encoderFieldId: Encoder[FieldId] = Encoder.instance {
    case StaticField.NormalAltStepTree => Json.fromString("stepsNA")
    case StaticField.ExceptionStepTree => Json.fromString("stepsE")
    case StaticField.ImplicationGraph  => Json.fromString("impGraph")
    case StaticField.StepGraph         => Json.fromString("stepGraph")
    case StaticField.OtherTags         => Json.fromString("otherTags")
    case StaticField.AllTags           => Json.fromString("allTags")
    case a: CustomField.Implication.Id => Json.obj("imp"  -> a.asJson)
    case a: CustomField.Number.Id      => Json.obj("num"  -> a.asJson)
    case a: CustomField.Tag.Id         => Json.obj("tag"  -> a.asJson)
    case a: CustomField.Text.Id        => Json.obj("text" -> a.asJson)
  }

  // ===================================================================================================================
  // Saved views

  object SavedViewCodecs {
    import shipreq.webapp.member.project.data.savedview._

    implicit val codecColumnCustomField: JsonCodec[Column.CustomField] =
      JsonCodec.xmap(Column.CustomField.apply)(_.id)

    implicit val decoderColumnCustomField: Decoder[Column.CustomField] =
      Decoder[CustomFieldId].map(Column.CustomField.apply)

    implicit val encoderColumnCustomField: Encoder[Column.CustomField] =
      Encoder[CustomFieldId].contramap(_.id)

    private[this] final val KeyCustomField    = "custom"
    private[this] final val KeyImplications   = "imps"
    private[this] final val KeyCode           = "code"
    private[this] final val KeyDeletionReason = "delReason"
    private[this] final val KeyPubid          = "pubid"
    private[this] final val KeyReqType        = "reqType"
    private[this] final val KeyTags           = "tags"
    private[this] final val KeyOtherTags      = "otherTags"
    private[this] final val KeyAllTags        = "allTags"
    private[this] final val KeyTitle          = "title"

    implicit val decoderColumn: Decoder[Column] = decodeSumBySoleKeyOrConst[Column](
      KeyCode           -> Column.Code,
      KeyDeletionReason -> Column.DeletionReason,
      KeyPubid          -> Column.Pubid,
      KeyReqType        -> Column.ReqType,
      KeyTags           -> Column.OtherTags,
      KeyOtherTags      -> Column.OtherTags,
      KeyAllTags        -> Column.AllTags,
      KeyTitle          -> Column.Title,
    ) {
      case (KeyCustomField   , c) => c.as[Column.CustomField]
      case (KeyImplications  , c) => c.as[Column.Implications]
    }

    implicit val encoderColumn: Encoder[Column] = Encoder.instance {
      case Column.Code            => Json.fromString(KeyCode)
      case Column.DeletionReason  => Json.fromString(KeyDeletionReason)
      case Column.Pubid           => Json.fromString(KeyPubid)
      case Column.ReqType         => Json.fromString(KeyReqType)
      case Column.OtherTags       => Json.fromString(KeyOtherTags)
      case Column.AllTags         => Json.fromString(KeyAllTags)
      case Column.Title           => Json.fromString(KeyTitle)
      case a: Column.CustomField  => Json.obj(KeyCustomField  -> a.asJson)
      case a: Column.Implications => Json.obj(KeyImplications -> a.asJson)
    }

    implicit val decoderColumnSortInconclusive: Decoder[Column.SortInconclusive] =
      decodeSumBySoleKeyOrConst[Column.SortInconclusive](
        KeyCode           -> Column.Code,
        KeyDeletionReason -> Column.DeletionReason,
        KeyReqType        -> Column.ReqType,
        KeyTags           -> Column.OtherTags,
        KeyOtherTags      -> Column.OtherTags,
        KeyAllTags        -> Column.AllTags,
        KeyTitle          -> Column.Title,
      ) {
        case (KeyCustomField , c) => c.as[Column.CustomField]
        case (KeyImplications, c) => c.as[Column.Implications]
      }

    implicit val encoderColumnSortInconclusive: Encoder[Column.SortInconclusive] = Encoder.instance {
      case Column.Code            => Json.fromString(KeyCode)
      case Column.DeletionReason  => Json.fromString(KeyDeletionReason)
      case Column.ReqType         => Json.fromString(KeyReqType)
      case Column.OtherTags       => Json.fromString(KeyOtherTags)
      case Column.AllTags         => Json.fromString(KeyAllTags)
      case Column.Title           => Json.fromString(KeyTitle)
      case a: Column.CustomField  => Json.obj(KeyCustomField  -> a.asJson)
      case a: Column.Implications => Json.obj(KeyImplications -> a.asJson)
    }

    implicit val decoderColumnSortInconclusiveHasBlanks: Decoder[Column.SortInconclusiveHasBlanks] =
      decodeSumBySoleKeyOrConst[Column.SortInconclusiveHasBlanks](
        KeyCode           -> Column.Code,
        KeyDeletionReason -> Column.DeletionReason,
        KeyTags           -> Column.OtherTags,
        KeyOtherTags      -> Column.OtherTags,
        KeyAllTags        -> Column.AllTags,
        KeyTitle          -> Column.Title,
      ) {
      case (KeyCustomField , c) => c.as[Column.CustomField]
      case (KeyImplications, c) => c.as[Column.Implications]
    }

    implicit val encoderColumnSortInconclusiveHasBlanks: Encoder[Column.SortInconclusiveHasBlanks] = Encoder.instance {
      case Column.Code            => Json.fromString(KeyCode)
      case Column.DeletionReason  => Json.fromString(KeyDeletionReason)
      case Column.OtherTags       => Json.fromString(KeyOtherTags)
      case Column.AllTags         => Json.fromString(KeyAllTags)
      case Column.Title           => Json.fromString(KeyTitle)
      case a: Column.CustomField  => Json.obj(KeyCustomField  -> a.asJson)
      case a: Column.Implications => Json.obj(KeyImplications -> a.asJson)
    }

    implicit val codecColumnNEV: JsonCodec[NonEmptyVector[Column]] =
      codecNEV

    implicit val codecColumnSortInconclusiveNoBlanks: JsonCodec[Column.SortInconclusiveNoBlanks] =
      JsonCodec.enumAdt(AdtMacros.adtIsoSet[Column.SortInconclusiveNoBlanks, String] {
        case Column.ReqType => KeyReqType
      })

    implicit val decoderSortCriterionInconclusiveCB: Decoder[SortCriterion.InconclusiveCB] =
      Decoder.forProduct2("column", "method")(SortCriterion.InconclusiveCB.apply)

    implicit val encoderSortCriterionInconclusiveCB: Encoder[SortCriterion.InconclusiveCB] =
      Encoder.forProduct2("column", "method")(a => (a.column, a.method))

    implicit val decoderSortCriterionInconclusiveIB: Decoder[SortCriterion.InconclusiveIB] =
      Decoder.forProduct2("column", "method")(SortCriterion.InconclusiveIB.apply)

    implicit val encoderSortCriterionInconclusiveIB: Encoder[SortCriterion.InconclusiveIB] =
      Encoder.forProduct2("column", "method")(a => (a.column, a.method))

    implicit val decoderSortCriterionI: Decoder[SortCriterion.Inconclusive] = decodeSumBySoleKey {
      case ("CB", c) => c.as[SortCriterion.InconclusiveCB]
      case ("IB", c) => c.as[SortCriterion.InconclusiveIB]
    }

    implicit val encoderSortCriterionI: Encoder[SortCriterion.Inconclusive] = Encoder.instance {
      case a: SortCriterion.InconclusiveCB => Json.obj("CB" -> a.asJson)
      case a: SortCriterion.InconclusiveIB => Json.obj("IB" -> a.asJson)
    }

    implicit val decoderSortCriterion: Decoder[SortCriterion] = decodeSumBySoleKey {
      case ("c" , c) => c.as[SortCriterion.Conclusive]
      case ("CB", c) => c.as[SortCriterion.InconclusiveCB]
      case ("IB", c) => c.as[SortCriterion.InconclusiveIB]
    }

    implicit val encoderSortCriterion: Encoder[SortCriterion] = Encoder.instance {
      case a: SortCriterion.Conclusive     => Json.obj("c"  -> a.asJson)
      case a: SortCriterion.InconclusiveCB => Json.obj("CB" -> a.asJson)
      case a: SortCriterion.InconclusiveIB => Json.obj("IB" -> a.asJson)
    }

    implicit val decoderSortCriteria: Decoder[SortCriteria] =
      Decoder.forProduct2("init", "last")(SortCriteria.apply)

    implicit val encoderSortCriteria: Encoder[SortCriteria] =
      Encoder.forProduct2("init", "last")(a => (a.init, a.last))

    implicit val decoderView: Decoder[View] =
      Decoder.instance { c =>
        for {
          columns        <- c.get[NonEmptyVector[Column]]("columns")
          order          <- c.get[SortCriteria          ]("order")
          filterDead     <- c.get[FilterDead            ]("filterDead")
          filter         <- c.get[Option[Filter.Valid]  ]("filter")
          impGraphConfig <- c.get[Option[ImpGraphConfig]]("impGraphConfig")
        } yield View(columns, order, filterDead, filter, impGraphConfig)
      }

    implicit val encoderView: Encoder[View] =
      Encoder.instance(value => Json.obj(
        "columns"        -> value.columns       .asJson,
        "order"          -> value.order         .asJson,
        "filterDead"     -> value.filterDead    .asJson,
        "filter"         -> value.filter        .asJson,
        "impGraphConfig" -> value.impGraphConfig.asJson,
      ).dropNullValues)

    implicit val decoderSavedView: Decoder[SavedView] =
      Decoder.forProduct3("id", "name", "view")(SavedView.apply)

    implicit val encoderSavedView: Encoder[SavedView] =
      Encoder.forProduct3("id", "name", "view")(a => (a.id, a.name, a.view))

    implicit val codecSavedViewsND: JsonCodec[SavedViews.NonDefault] =
      codecIMap(SavedViews.emptyNonDefault)

    implicit val decoderSavedViews: Decoder[SavedViews.NonEmpty] =
      Decoder.forProduct2("default", "nonDefault")(SavedViews.NonEmpty.apply)

    implicit val encoderSavedViews: Encoder[SavedViews.NonEmpty] =
      Encoder.forProduct2("default", "nonDefault")(a => (a.default, a.nonDefault))
  }

  // ===================================================================================================================
  // Filter

  private[this] object FilterAstKeys {
    final val KeyAstAllOf          = "all"
    final val KeyAstAnyOf          = "any"
    final val KeyAstHasIssue       = "issue"
    final val KeyAstHashRef        = "hash"
    final val KeyAstFieldProp      = "field"
    final val KeyAstImpliedByAnyOf = "impBy"
    final val KeyAstImpliesAnyOf   = "imp"
    final val KeyAstNot            = "not"
    final val KeyAstPresence       = "has"
    final val KeyAstRegex          = "regex"
    final val KeyAstReqType        = "reqType"
    final val KeyAstReqs           = "reqs"
    final val KeyAstText           = "text"
    final val KeyAstScoped1        = "scoped1"
    final val KeyAstScoped2        = "scoped2"
    final val KeyAstRelativeTags   = "relTags"
  }

  implicit lazy val codecValidFilter: JsonCodec[Filter.Valid] = {
    import shipreq.webapp.member.project.filter.{IntensionalReqSet, FilterAst}
    import Filter._
    import Filter.Implicits._
    import Filter.Valid.FieldCriteriaF
    import FilterAstKeys._

    implicit val codecNonEmptySetInt: JsonCodec[NonEmptySet[Int]] =
      codecNES

    implicit def decoderIRSetWhole[RT: Decoder]: Decoder[IntensionalReqSet.WholeType[RT]] =
      Decoder[RT].map(IntensionalReqSet.WholeType.apply[RT])

    implicit def encoderIRSetWhole[RT: Encoder]: Encoder[IntensionalReqSet.WholeType[RT]] =
      Encoder[RT].contramap(_.reqType)

    implicit def decoderIRSetSome[RT: Decoder]: Decoder[IntensionalReqSet.SomeOfType[RT]] =
      Decoder.forProduct2("reqType", "numbers")(IntensionalReqSet.SomeOfType.apply[RT])

    implicit def encoderIRSetSome[RT: Encoder]: Encoder[IntensionalReqSet.SomeOfType[RT]] =
      Encoder.forProduct2("reqType", "numbers")(a => (a.reqType, a.numbers))

    def decoderIRSet[RT](implicit d1: Decoder[IntensionalReqSet.SomeOfType[RT]], d2: Decoder[IntensionalReqSet.WholeType[RT]]): Decoder[IntensionalReqSet[RT]] = decodeSumBySoleKey {
      case ("some" , c) => c.as[IntensionalReqSet.SomeOfType[RT]]
      case ("whole", c) => c.as[IntensionalReqSet.WholeType[RT]]
    }

    def encoderIRSet[RT](implicit e1: Encoder[IntensionalReqSet.SomeOfType[RT]], e2: Encoder[IntensionalReqSet.WholeType[RT]]): Encoder[IntensionalReqSet[RT]] = Encoder.instance {
      case a: IntensionalReqSet.SomeOfType[RT] => Json.obj("some"  -> a.asJson)
      case a: IntensionalReqSet.WholeType[RT]  => Json.obj("whole" -> a.asJson)
    }

    implicit lazy val codecValidHashTag: JsonCodec[Valid.HashTag] =
      codecDisj[CustomIssueTypeId, ApplicableTagId]

    implicit lazy val codecValidField: JsonCodec[Valid.Field] = {
      val encoder =
        Encoder.instance[Valid.Field] {
          case \/-(f)                         => f.asJson
          case -\/(SpecialBuiltInField.Title) => Json.fromString("title")
        }

      val decFieldId = decoderFieldId.map[Valid.Field](\/-(_))

      val decBuiltIn = Decoder[String].emap[Valid.Field] {
        case "title" => Right(-\/(SpecialBuiltInField.Title))
        case x       => Left("Unknown field: " + x)
      }

      JsonCodec(encoder, decFieldId or decBuiltIn)
    }

    implicit val codecValidIssueCatNEV: JsonCodec[NonEmptyVector[Valid.IssueCat]] =
      codecNEV

    implicit val codecValidReqSubset: JsonCodec[Valid.ReqSubset] =
      JsonCodec(encoderIRSet, decoderIRSet)

    implicit val codecValidReqSet: JsonCodec[Valid.ReqSet] =
      codecNEV

    implicit lazy val codecFilterAstAttr: JsonCodec[FilterAst.Attr] =
      JsonCodec.enumAdt(AdtMacros.adtIsoSet[FilterAst.Attr, String] {
        case FilterAst.Attr.AnyIssue => "issue"
        case FilterAst.Attr.AnyTag   => "tag"
      })

    implicit lazy val codecFilterAstFieldAttr: JsonCodec[FilterAst.FieldAttr] =
      JsonCodec.enumAdt(AdtMacros.adtIsoSet[FilterAst.FieldAttr, String] {
        case FilterAst.FieldAttr.Blank         => "blank"
        case FilterAst.FieldAttr.NotBlank      => "notBlank"
        case FilterAst.FieldAttr.NotApplicable => "n/a"
        case FilterAst.FieldAttr.DefaultInUse  => "default"
      })

    implicit val decoderFilterAstText: Decoder[FilterAst.Text] =
      Decoder.forProduct2("text", "quote")(FilterAst.Text.apply)

    implicit val encoderFilterAstText: Encoder[FilterAst.Text] =
      Encoder.forProduct2("text", "quote")(a => (a.text, a.quoteChar))

    implicit val codecFilterAstRegex: JsonCodec[FilterAst.Regex] =
      JsonCodec.xmap(FilterAst.Regex.apply)(_.text)

    implicit val codecFilterAstPresence: JsonCodec[FilterAst.Presence[Valid.Attr]] =
      JsonCodec.xmap(FilterAst.Presence.apply[Valid.Attr])(_.attr)

    implicit val decoderFilterAstHasIssue: Decoder[FilterAst.HasIssue[Valid.IssueCat]] =
      Decoder.forProduct2("on", "criteria")(FilterAst.HasIssue.apply)

    implicit val encoderFilterAstHasIssue: Encoder[FilterAst.HasIssue[Valid.IssueCat]] =
      Encoder.forProduct2("on", "criteria")(a => (a.on, a.criteria))

    implicit val decoderFieldCriteriaAttr: Decoder[FilterAst.FieldCriteria.Attr[FilterAst.FieldAttr]] =
      Decoder[FilterAst.FieldAttr].map(FilterAst.FieldCriteria.Attr.apply)

    implicit val encoderFieldCriteriaAttr: Encoder[FilterAst.FieldCriteria.Attr[FilterAst.FieldAttr]] =
      Encoder[FilterAst.FieldAttr].contramap(_.value)

    implicit val decoderFieldCriteriaReqTypePosSet: Decoder[FilterAst.FieldCriteria.ReqTypePosSet] =
      Decoder[NonEmptySet[ReqTypePos]].map(FilterAst.FieldCriteria.ReqTypePosSet.apply)

    implicit val encoderFieldCriteriaReqTypePosSet: Encoder[FilterAst.FieldCriteria.ReqTypePosSet] =
      Encoder[NonEmptySet[ReqTypePos]].contramap(_.value)

    implicit val decoderFieldCriteriaCompareNumber: Decoder[FilterAst.FieldCriteria.CompareNumber] =
      Decoder[Double].map(FilterAst.FieldCriteria.CompareNumber.apply)

    implicit val encoderFieldCriteriaCompareNumber: Encoder[FilterAst.FieldCriteria.CompareNumber] =
      Encoder[Double].contramap(_.value)

    implicit val decoderFieldCriteriaQuery: Decoder[FilterAst.FieldCriteria.Query[ACursor]] =
      Decoder.instance(c => Right(FilterAst.FieldCriteria.Query(c)))

    implicit val encoderFieldCriteriaQuery: Encoder[FilterAst.FieldCriteria.Query[Json]] =
      Encoder[Json].contramap(_.value)

    implicit val decoderFieldCriteria: Decoder[FieldCriteriaF[ACursor]] = decodeSumBySoleKey {
      case ("attr" ,  c) => c.as[FilterAst.FieldCriteria.Attr[FilterAst.FieldAttr]]
      case ("rtpos",  c) => c.as[FilterAst.FieldCriteria.ReqTypePosSet]
      case ("query",  c) => c.as[FilterAst.FieldCriteria.Query[ACursor]]
      case ("cmpNum", c) => c.as[FilterAst.FieldCriteria.CompareNumber]
    }

    implicit val encoderFieldCriteria: Encoder[FieldCriteriaF[Json]] = Encoder.instance {
      case a: FilterAst.FieldCriteria.Attr[FilterAst.FieldAttr] => Json.obj("attr"   -> a.asJson)
      case a: FilterAst.FieldCriteria.ReqTypePosSet             => Json.obj("rtpos"  -> a.asJson)
      case a: FilterAst.FieldCriteria.Query[Json]               => Json.obj("query"  -> a.asJson)
      case a: FilterAst.FieldCriteria.CompareNumber             => Json.obj("cmpNum" -> a.asJson)
    }

    implicit val decoderFilterAstFieldProp: Decoder[Valid.FieldPropF[ACursor]] =
      Decoder.instance { c =>
        for {
          field    <- c.get[Valid.Field]("field")
          criteria <- c.get[Valid.FieldCriteriaF[ACursor]]("criteria") orElse c.get[FilterAst.FieldAttr]("attr").map(FilterAst.FieldCriteria.Attr(_))
        } yield FilterAst.FieldProp(field, criteria)
      }

    implicit val encoderFilterAstFieldProp: Encoder[Valid.FieldPropF[Json]] =
      Encoder.forProduct2("field", "criteria")(a => (a.field, a.criteria))

    implicit val decoderFilterAstHashRef: Decoder[FilterAst.HashRef[Valid.HashTag]] =
      Decoder[Valid.HashTag].map(FilterAst.HashRef.apply)

    implicit val encoderFilterAstHashRef: Encoder[FilterAst.HashRef[Valid.HashTag]] =
      Encoder[Valid.HashTag].contramap(_.value)

    implicit def decoderFilterAstImpCriteriaReqs[R: Decoder]: Decoder[FilterAst.ImpCriteria.Reqs[R]] =
      Decoder[R].map(FilterAst.ImpCriteria.Reqs.apply[R])

    implicit def encoderFilterAstImpCriteriaReqs[R: Encoder]: Encoder[FilterAst.ImpCriteria.Reqs[R]] =
      Encoder[R].contramap(_.value)

    implicit val decoderFilterAstImpCriteriaQuery: Decoder[FilterAst.ImpCriteria.Query[ACursor]] =
      Decoder.instance(c => Right(FilterAst.ImpCriteria.Query(c)))

    implicit val encoderFilterAstImpCriteriaQuery: Encoder[FilterAst.ImpCriteria.Query[Json]] =
      Encoder[Json].contramap(_.value)

    implicit def decoderFilterAstImpCriteria[R, Q](implicit d1: Decoder[FilterAst.ImpCriteria.Query[Q]], d2: Decoder[FilterAst.ImpCriteria.Reqs[R]]): Decoder[FilterAst.ImpCriteria[R, Q]] = decodeSumBySoleKey {
      case ("query", c) => c.as[FilterAst.ImpCriteria.Query[Q]]
      case ("reqs" , c) => c.as[FilterAst.ImpCriteria.Reqs[R]]
    }

    implicit def encoderFilterAstImpCriteria[R, Q](implicit e1: Encoder[FilterAst.ImpCriteria.Query[Q]], e2: Encoder[FilterAst.ImpCriteria.Reqs[R]]): Encoder[FilterAst.ImpCriteria[R, Q]] = Encoder.instance {
      case a: FilterAst.ImpCriteria.Query[Q] => Json.obj("query" -> a.asJson)
      case a: FilterAst.ImpCriteria.Reqs[R]  => Json.obj("reqs"  -> a.asJson)
    }

    val decoderImpCriteria: Decoder[Valid.ImpCriteriaF[ACursor]] =
      decoderFilterAstImpCriteria[Valid.ReqSet, ACursor]
        .or(Decoder[Valid.ReqSet].map(FilterAst.ImpCriteria.Reqs(_))) // backwards-compatibility

    val encoderImpCriteria: Encoder[Valid.ImpCriteriaF[Json]] =
      encoderFilterAstImpCriteria

    implicit val decoderFilterAstImpliesAnyOf: Decoder[FilterAst.ImpliesAnyOf[Valid.ImpCriteriaF, ACursor]] =
      decoderImpCriteria.map(FilterAst.ImpliesAnyOf(_))

    implicit val encoderFilterAstImpliesAnyOf: Encoder[FilterAst.ImpliesAnyOf[Valid.ImpCriteriaF, Json]] =
      encoderImpCriteria.contramap(_.criteria)

    implicit val decoderFilterAstImpliedByAnyOf: Decoder[FilterAst.ImpliedByAnyOf[Valid.ImpCriteriaF, ACursor]] =
      decoderImpCriteria.map(FilterAst.ImpliedByAnyOf(_))

    implicit val encoderFilterAstImpliedByAnyOf: Encoder[FilterAst.ImpliedByAnyOf[Valid.ImpCriteriaF, Json]] =
      encoderImpCriteria.contramap(_.criteria)

    implicit val decoderFilterAstReqs: Decoder[FilterAst.Reqs[Valid.ReqSet]] =
      Decoder[Valid.ReqSet].map(FilterAst.Reqs.apply)

    implicit val encoderFilterAstReqs: Encoder[FilterAst.Reqs[Valid.ReqSet]] =
      Encoder[Valid.ReqSet].contramap(_.reqs)

    implicit val decoderFilterAstReqType: Decoder[FilterAst.ReqType[Valid.ReqType]] =
      Decoder[Valid.ReqType].map(FilterAst.ReqType.apply)

    implicit val encoderFilterAstReqType: Encoder[FilterAst.ReqType[Valid.ReqType]] =
      Encoder[Valid.ReqType].contramap(_.reqType)

    implicit def decoderFilterAstScopeDerivation[A: Decoder]: Decoder[FilterAst.Scope.Derivation[A]] =
      Decoder[Option[A]].map(FilterAst.Scope.Derivation.apply[A])

    implicit def encoderFilterAstScopeDerivation[A: Encoder]: Encoder[FilterAst.Scope.Derivation[A]] =
      Encoder[Option[A]].contramap(_.field)

    implicit def decoderFilterAstScope[A](implicit d1: Decoder[FilterAst.Scope.Derivation[A]]): Decoder[FilterAst.Scope[A]] = decodeSumBySoleKey {
      case ("derivation", c) => c.as[FilterAst.Scope.Derivation[A]]
    }

    implicit def encoderFilterAstScope[A](implicit e1: Encoder[FilterAst.Scope.Derivation[A]]): Encoder[FilterAst.Scope[A]] = Encoder.instance {
      case a: FilterAst.Scope.Derivation[A] => Json.obj("derivation" -> a.asJson)
    }

    implicit val codecFilterAstScope: JsonCodec[Valid.Scope] =
      codecNES

    implicit val decoderFilterAstScoped1: Decoder[FilterAst.Scoped1[Valid.Scope, ACursor]] =
      Decoder.instance { c =>
        for {
          main   <- c.get[Boolean]("main")
          scope  <- c.get[Valid.Scope]("scope")
        } yield FilterAst.Scoped1(main, scope, c.downField("clause"))
      }

    implicit val encoderFilterAstScoped1: Encoder[FilterAst.Scoped1[Valid.Scope, Json]] =
      Encoder.forProduct3("main", "scope", "clause")(a => (a.main, a.scope, a.clause))

    implicit val decoderFilterAstScoped2: Decoder[FilterAst.Scoped2[Valid.Scope, ACursor]] =
      Decoder.instance { c =>
        for {
          scope <- c.get[Valid.Scope]("scope")
        } yield FilterAst.Scoped2(scope, c.downField("clause"), c.downField("main"))
      }

    implicit val encoderFilterAstScoped2: Encoder[FilterAst.Scoped2[Valid.Scope, Json]] =
      Encoder.forProduct3("scope", "clause", "main")(a => (a.scope, a.clause, a.mainClause))

    implicit val codecFilterAstOrderOp: JsonCodec[FilterAst.OrderOp] =
      JsonCodec.enumAdt(AdtMacros.adtIsoSet[FilterAst.OrderOp, String] {
        case FilterAst.OrderOp.<  => "<"
        case FilterAst.OrderOp.>  => ">"
        case FilterAst.OrderOp.<= => "<="
        case FilterAst.OrderOp.>= => ">="
      })

    implicit val decoderFilterAstRelativeTags: Decoder[FilterAst.RelativeTags[Valid.ApTag]] =
      Decoder.forProduct2("op", "subject")(FilterAst.RelativeTags.apply[Valid.ApTag])

    implicit val encoderFilterAstRelativeTags: Encoder[FilterAst.RelativeTags[Valid.ApTag]] =
      Encoder.forProduct2("op", "subject")(a => (a.op, a.subject))

    JsonCodec.fix[ValidF]({
      case a: FilterAst.Text                           => Json.obj(KeyAstText           -> a.asJson)
      case a: FilterAst.Regex                          => Json.obj(KeyAstRegex          -> a.asJson)
      case a: FilterAst.Presence      [Valid.Attr]     => Json.obj(KeyAstPresence       -> a.asJson)
      case a: FilterAst.HasIssue      [Valid.IssueCat] => Json.obj(KeyAstHasIssue       -> a.asJson)
      case a: FilterAst.HashRef       [Valid.HashTag]  => Json.obj(KeyAstHashRef        -> a.asJson)
      case a: FilterAst.RelativeTags  [Valid.ApTag]    => Json.obj(KeyAstRelativeTags   -> a.asJson)
      case a@ FilterAst.ImpliesAnyOf  (_)              => Json.obj(KeyAstImpliesAnyOf   -> a.asJson)
      case a@ FilterAst.ImpliedByAnyOf(_)              => Json.obj(KeyAstImpliedByAnyOf -> a.asJson)
      case a: FilterAst.Reqs          [Valid.ReqSet]   => Json.obj(KeyAstReqs           -> a.asJson)
      case a: FilterAst.ReqType       [Valid.ReqType]  => Json.obj(KeyAstReqType        -> a.asJson)
      case a@ FilterAst.FieldProp     (_, _)           => Json.obj(KeyAstFieldProp      -> a.asJson)
      case a@ FilterAst.Scoped1       (_, _, _)        => Json.obj(KeyAstScoped1        -> a.asJson)
      case a@ FilterAst.Scoped2       (_, _, _)        => Json.obj(KeyAstScoped2        -> a.asJson)
      case FilterAst.Not              (clause)         => Json.obj(KeyAstNot            -> clause)
      case FilterAst.AllOf            (clauses)        => Json.obj(KeyAstAllOf          -> Json.arr(clauses.whole: _*))
      case FilterAst.AnyOf            (head, tail)     => Json.obj(KeyAstAnyOf          -> Json.arr(head +: tail.whole: _*))
    }, decoderFnSumBySoleKey {
      case (KeyAstText          , c) => c.as[FilterAst.Text]
      case (KeyAstRegex         , c) => c.as[FilterAst.Regex]
      case (KeyAstPresence      , c) => c.as[FilterAst.Presence      [Valid.Attr]]
      case (KeyAstHasIssue      , c) => c.as[FilterAst.HasIssue      [Valid.IssueCat]]
      case (KeyAstHashRef       , c) => c.as[FilterAst.HashRef       [Valid.HashTag]]
      case (KeyAstRelativeTags  , c) => c.as[FilterAst.RelativeTags  [Valid.ApTag]]
      case (KeyAstImpliesAnyOf  , c) => c.as[FilterAst.ImpliesAnyOf  [Valid.ImpCriteriaF, ACursor]]
      case (KeyAstImpliedByAnyOf, c) => c.as[FilterAst.ImpliedByAnyOf[Valid.ImpCriteriaF, ACursor]]
      case (KeyAstReqs          , c) => c.as[FilterAst.Reqs          [Valid.ReqSet]]
      case (KeyAstReqType       , c) => c.as[FilterAst.ReqType       [Valid.ReqType]]
      case (KeyAstScoped1       , c) => c.as[FilterAst.Scoped1       [Valid.Scope, ACursor]]
      case (KeyAstScoped2       , c) => c.as[FilterAst.Scoped2       [Valid.Scope, ACursor]]
      case (KeyAstFieldProp     , c) => c.as[Valid.FieldPropF        [ACursor]]
      case (KeyAstNot           , c) => Right(FilterAst.Not(c))

      case (KeyAstAllOf, c) =>
        val c1 = c.downArray
        val cn = Iterator.iterate(c1)(_.right).takeWhile(_.succeeded).toVector
        Right(FilterAst.AllOf(NonEmptyVector(c1, cn)))

      case (KeyAstAnyOf, c) =>
        val c1 = c.downArray
        val c2 = c1.right
        val cn = Iterator.iterate(c2)(_.right).takeWhile(_.succeeded).toVector
        Right(FilterAst.AnyOf(c1, NonEmptyVector(c2, cn)))
    })
  }

  // ===================================================================================================================
  // Events

  private[json] implicit lazy val codecSavedViewGDv1: JsonCodec[SavedViewGDv1.NonEmptyValues] = {
    import SavedViewGDv1._

    implicit val codecValueForColumns    = JsonCodec.xmap(ValueForColumns   .apply)(_.value)
    implicit val codecValueForFilter     = JsonCodec.xmap(ValueForFilter    .apply)(_.value)
    implicit val codecValueForFilterDead = JsonCodec.xmap(ValueForFilterDead.apply)(_.value)
    implicit val codecValueForName       = JsonCodec.xmap(ValueForName      .apply)(_.value)
    implicit val codecValueForOrder      = JsonCodec.xmap(ValueForOrder     .apply)(_.value)

    implicit val decoderValue: Decoder[Value] = decodeSumBySoleKey {
      case ("columns"   , c) => c.as[ValueForColumns]
      case ("filter"    , c) => c.as[ValueForFilter]
      case ("filterDead", c) => c.as[ValueForFilterDead]
      case ("name"      , c) => c.as[ValueForName]
      case ("order"     , c) => c.as[ValueForOrder]
    }

    implicit val encoderValue: Encoder[Value] = Encoder.instance {
      case a: ValueForColumns    => Json.obj("columns"    -> a.asJson)
      case a: ValueForFilter     => Json.obj("filter"     -> a.asJson)
      case a: ValueForFilterDead => Json.obj("filterDead" -> a.asJson)
      case a: ValueForName       => Json.obj("name"       -> a.asJson)
      case a: ValueForOrder      => Json.obj("order"      -> a.asJson)
    }

    implicit val values: JsonCodec[Values] = codecIMap(emptyValues)
    codecNonEmptyMono[Values]
  }

  private[json] implicit lazy val codecSavedViewGD: JsonCodec[SavedViewGD.NonEmptyValues] = {
    import SavedViewGD._

    implicit val codecValueForColumns        = JsonCodec.xmap(ValueForColumns       .apply)(_.value)
    implicit val codecValueForFilter         = JsonCodec.xmap(ValueForFilter        .apply)(_.value)
    implicit val codecValueForFilterDead     = JsonCodec.xmap(ValueForFilterDead    .apply)(_.value)
    implicit val codecValueForName           = JsonCodec.xmap(ValueForName          .apply)(_.value)
    implicit val codecValueForOrder          = JsonCodec.xmap(ValueForOrder         .apply)(_.value)
    implicit val codecValueForImpGraphConfig = JsonCodec.xmap(ValueForImpGraphConfig.apply)(_.value)

    implicit val decoderValue: Decoder[Value] = decodeSumBySoleKey {
      case ("columns"       , c) => c.as[ValueForColumns]
      case ("filter"        , c) => c.as[ValueForFilter]
      case ("filterDead"    , c) => c.as[ValueForFilterDead]
      case ("name"          , c) => c.as[ValueForName]
      case ("order"         , c) => c.as[ValueForOrder]
      case ("impGraphConfig", c) => c.as[ValueForImpGraphConfig]
    }

    implicit val encoderValue: Encoder[Value] = Encoder.instance {
      case a: ValueForColumns        => Json.obj("columns"        -> a.asJson)
      case a: ValueForFilter         => Json.obj("filter"         -> a.asJson)
      case a: ValueForFilterDead     => Json.obj("filterDead"     -> a.asJson)
      case a: ValueForName           => Json.obj("name"           -> a.asJson)
      case a: ValueForOrder          => Json.obj("order"          -> a.asJson)
      case a: ValueForImpGraphConfig => Json.obj("impGraphConfig" -> a.asJson)
    }

    implicit val values: JsonCodec[Values] = codecIMap(emptyValues)
    codecNonEmptyMono[Values]
  }

  private[json] implicit val codecEventNonEmptyCustomNumberMap: JsonCodec[Event.NonEmptyCustomNumberMap] =
    codecNonEmptyMono

  private[json] implicit val codecGenericReqGD: JsonCodec[GenericReqGD.Values] = {
    import GenericReqGD._

    implicit val codecValueForCodes      = JsonCodec.xmap(ValueForCodes     .apply)(_.value)
    implicit val codecValueForCustomNums = JsonCodec.xmap(ValueForCustomNums.apply)(_.value)
    implicit val codecValueForCustomText = JsonCodec.xmap(ValueForCustomText.apply)(_.value)
    implicit val codecValueForImpSrcs    = JsonCodec.xmap(ValueForImpSrcs   .apply)(_.value)
    implicit val codecValueForImpTgts    = JsonCodec.xmap(ValueForImpTgts   .apply)(_.value)
    implicit val codecValueForTags       = JsonCodec.xmap(ValueForTags      .apply)(_.value)
    implicit val codecValueForTitle      = JsonCodec.xmap(ValueForTitle     .apply)(_.value)

    implicit val decoderValue: Decoder[Value] = decodeSumBySoleKey {
      case ("codes"     , c) => c.as[ValueForCodes]
      case ("customNums", c) => c.as[ValueForCustomNums]
      case ("customText", c) => c.as[ValueForCustomText]
      case ("impSrcs"   , c) => c.as[ValueForImpSrcs]
      case ("impTgts"   , c) => c.as[ValueForImpTgts]
      case ("tags"      , c) => c.as[ValueForTags]
      case ("title"     , c) => c.as[ValueForTitle]
    }

    implicit val encoderValue: Encoder[Value] = Encoder.instance {
      case a: ValueForCodes      => Json.obj("codes"      -> a.asJson)
      case a: ValueForCustomNums => Json.obj("customNums" -> a.asJson)
      case a: ValueForCustomText => Json.obj("customText" -> a.asJson)
      case a: ValueForImpSrcs    => Json.obj("impSrcs"    -> a.asJson)
      case a: ValueForImpTgts    => Json.obj("impTgts"    -> a.asJson)
      case a: ValueForTags       => Json.obj("tags"       -> a.asJson)
      case a: ValueForTitle      => Json.obj("title"      -> a.asJson)
    }

    codecIMap(emptyValues)
  }

  private[json] implicit val codecUseCaseGD: JsonCodec[UseCaseGD.Values] = {
    import UseCaseGD._

    implicit val codecValueForCodes      = JsonCodec.xmap(ValueForCodes     .apply)(_.value)
    implicit val codecValueForCustomNums = JsonCodec.xmap(ValueForCustomNums.apply)(_.value)
    implicit val codecValueForCustomText = JsonCodec.xmap(ValueForCustomText.apply)(_.value)
    implicit val codecValueForImpSrcs    = JsonCodec.xmap(ValueForImpSrcs   .apply)(_.value)
    implicit val codecValueForImpTgts    = JsonCodec.xmap(ValueForImpTgts   .apply)(_.value)
    implicit val codecValueForTags       = JsonCodec.xmap(ValueForTags      .apply)(_.value)
    implicit val codecValueForTitle      = JsonCodec.xmap(ValueForTitle     .apply)(_.value)

    implicit val decoderValue: Decoder[Value] = decodeSumBySoleKey {
      case ("codes"     , c) => c.as[ValueForCodes]
      case ("customNums", c) => c.as[ValueForCustomNums]
      case ("customText", c) => c.as[ValueForCustomText]
      case ("impSrcs"   , c) => c.as[ValueForImpSrcs]
      case ("impTgts"   , c) => c.as[ValueForImpTgts]
      case ("tags"      , c) => c.as[ValueForTags]
      case ("title"     , c) => c.as[ValueForTitle]
    }

    implicit val encoderValue: Encoder[Value] = Encoder.instance {
      case a: ValueForCodes      => Json.obj("codes"      -> a.asJson)
      case a: ValueForCustomNums => Json.obj("customNums" -> a.asJson)
      case a: ValueForCustomText => Json.obj("customText" -> a.asJson)
      case a: ValueForImpSrcs    => Json.obj("impSrcs"    -> a.asJson)
      case a: ValueForImpTgts    => Json.obj("impTgts"    -> a.asJson)
      case a: ValueForTags       => Json.obj("tags"       -> a.asJson)
      case a: ValueForTitle      => Json.obj("title"      -> a.asJson)
    }

    codecIMap(emptyValues)
  }

  object EventData {

    implicit val decoderEventProjectDelete: Decoder[Event.ProjectDelete] =
      Decoder.forProduct1("reason")(Event.ProjectDelete.apply)

    implicit val encoderEventProjectDelete: Encoder[Event.ProjectDelete] =
      Encoder.forProduct1("reason")(_.reason)

    implicit val jsonCodecEventProjectRestore: JsonCodec[Event.ProjectRestore.type] =
      JsonCodec.const(Event.ProjectRestore)

    implicit val decoderEventFieldReposition: Decoder[Event.FieldReposition] =
      Decoder.forProduct2("id", "newPos")(Event.FieldReposition.apply)

    implicit val encoderEventFieldReposition: Encoder[Event.FieldReposition] =
      Encoder.forProduct2("id", "newPos")(a => (a.id, a.newPos))

    implicit val decoderEventFieldCustomDelete: Decoder[Event.FieldCustomDelete] =
      Decoder[CustomFieldId].map(Event.FieldCustomDelete.apply)

    implicit val encoderEventFieldCustomDelete: Encoder[Event.FieldCustomDelete] =
      Encoder[CustomFieldId].contramap(_.id)

    implicit val decoderEventFieldCustomRestore: Decoder[Event.FieldCustomRestore] =
      Decoder[CustomFieldId].map(Event.FieldCustomRestore.apply)

    implicit val encoderEventFieldCustomRestore: Encoder[Event.FieldCustomRestore] =
      Encoder[CustomFieldId].contramap(_.id)

    implicit val decoderEventSavedViewCreateV1: Decoder[Event.SavedViewCreateV1] =
      Decoder.forProduct6("id", "name", "columns", "order", "filterDead", "filter")(Event.SavedViewCreateV1.apply)

    implicit val encoderEventSavedViewCreateV1: Encoder[Event.SavedViewCreateV1] =
      Encoder.forProduct6("id", "name", "columns", "order", "filterDead", "filter")(a => (a.id, a.name, a.columns, a.order, a.filterDead, a.filter))

    implicit val decoderEventSavedViewUpdateV1: Decoder[Event.SavedViewUpdateV1] =
      Decoder.forProduct2("id", "values")(Event.SavedViewUpdateV1.apply)

    implicit val encoderEventSavedViewUpdateV1: Encoder[Event.SavedViewUpdateV1] =
      Encoder.forProduct2("id", "values")(a => (a.id, a.vs))

    implicit val decoderEventSavedViewCreate: Decoder[Event.SavedViewCreate] =
      Decoder.forProduct7("id", "name", "columns", "order", "filterDead", "filter", "impGraphConfig")(
        Event.SavedViewCreate.apply)

    implicit val encoderEventSavedViewCreate: Encoder[Event.SavedViewCreate] =
      Encoder.forProduct7("id", "name", "columns", "order", "filterDead", "filter", "impGraphConfig")(
        a => (a.id, a.name, a.columns, a.order, a.filterDead, a.filter, a.impGraphConfig))

    implicit val decoderEventSavedViewUpdate: Decoder[Event.SavedViewUpdate] =
      Decoder.forProduct2("id", "values")(Event.SavedViewUpdate.apply)

    implicit val encoderEventSavedViewUpdate: Encoder[Event.SavedViewUpdate] =
      Encoder.forProduct2("id", "values")(a => (a.id, a.vs))

    private[json] implicit lazy val codecCustomNumberFieldGD: JsonCodec[CustomNumberFieldGD.NonEmptyValues] = {
      import CustomNumberFieldGD._

      implicit val codecValueForName              = JsonCodec.xmap(ValueForName             .apply)(_.value)
      implicit val codecValueForDesc              = JsonCodec.xmap(ValueForDesc             .apply)(_.value)
      implicit val codecValueForRange             = JsonCodec.xmap(ValueForRange            .apply)(_.value)
      implicit val codecValueForDecimalPlaces     = JsonCodec.xmap(ValueForDecimalPlaces    .apply)(_.value)
      implicit val codecValueForFieldReqTypeRules = JsonCodec.xmap(ValueForFieldReqTypeRules.apply)(_.value)

      implicit val decoderValue: Decoder[Value] = decodeSumBySoleKey {
        case ("name"    , c) => c.as[ValueForName]
        case ("desc"    , c) => c.as[ValueForDesc]
        case ("range"   , c) => c.as[ValueForRange]
        case ("dp"      , c) => c.as[ValueForDecimalPlaces]
        case ("reqTypes", c) => c.as[ValueForFieldReqTypeRules]
      }

      implicit val encoderValue: Encoder[Value] = Encoder.instance {
        case a: ValueForName              => Json.obj("name"     -> a.asJson)
        case a: ValueForDesc              => Json.obj("desc"     -> a.asJson)
        case a: ValueForRange             => Json.obj("range"    -> a.asJson)
        case a: ValueForDecimalPlaces     => Json.obj("dp"       -> a.asJson)
        case a: ValueForFieldReqTypeRules => Json.obj("reqTypes" -> a.asJson)
      }

      implicit val values: JsonCodec[Values] = codecIMap(emptyValues)
      codecNonEmptyMono[Values]
    }

    implicit val decoderEventFieldCustomNumberCreate: Decoder[Event.FieldCustomNumberCreate] =
      Decoder.forProduct2("id", "values")(Event.FieldCustomNumberCreate.apply)

    implicit val encoderEventFieldCustomNumberCreate: Encoder[Event.FieldCustomNumberCreate] =
      Encoder.forProduct2("id", "values")(a => (a.id, a.vs))

    implicit val decoderEventFieldCustomNumberUpdate: Decoder[Event.FieldCustomNumberUpdate] =
      Decoder.forProduct2("id", "values")(Event.FieldCustomNumberUpdate.apply)

    implicit val encoderEventFieldCustomNumberUpdate: Encoder[Event.FieldCustomNumberUpdate] =
      Encoder.forProduct2("id", "values")(a => (a.id, a.vs))

    implicit val decoderEventReqFieldCustomNumberSet: Decoder[Event.ReqFieldCustomNumberSet] =
      Decoder.forProduct3("id", "fid", "value")(Event.ReqFieldCustomNumberSet.apply)

    implicit val encoderEventReqFieldCustomNumberSet: Encoder[Event.ReqFieldCustomNumberSet] =
      Encoder.forProduct3("id", "fid", "value")(a => (a.id, a.fid, a.value))

    implicit val decoderEventGenericReqCreate: Decoder[Event.GenericReqCreate] =
      Decoder.forProduct3("reqId", "reqTypeId", "values")(Event.GenericReqCreate.apply)

    implicit val encoderEventGenericReqCreate: Encoder[Event.GenericReqCreate] =
      Encoder.forProduct3("reqId", "reqTypeId", "values")(a => (a.id, a.rt, a.vs))

    implicit val decoderEventUseCaseCreate: Decoder[Event.UseCaseCreate] =
      Decoder.forProduct3("id", "stepId", "values")(Event.UseCaseCreate.apply)

    implicit val encoderEventUseCaseCreate: Encoder[Event.UseCaseCreate] =
      Encoder.forProduct3("id", "stepId", "values")(a => (a.id, a.stepId, a.vs))
  }

  implicit lazy val decoderEvent: Decoder[Event] = decodeSumBySoleKey {
    case ("AccessUpdate"           , c) => c.as[Event.AccessUpdate]
    case ("ApplicableTagCreate:2"  , c) => c.as[Event.ApplicableTagCreate]
    case ("ApplicableTagCreate"    , c) => c.as[Event.ApplicableTagCreateV1]
    case ("ApplicableTagUpdate:2"  , c) => c.as[Event.ApplicableTagUpdate]
    case ("ApplicableTagUpdate"    , c) => c.as[Event.ApplicableTagUpdateV1]
    case ("CodeGroupCreate"        , c) => c.as[Event.CodeGroupCreate]
    case ("CodeGroupsDelete"       , c) => c.as[Event.CodeGroupsDelete]
    case ("CodeGroupUpdate"        , c) => c.as[Event.CodeGroupUpdate]
    case ("ContentRestore"         , c) => c.as[Event.ContentRestore]
    case ("CustomIssueTypeCreate"  , c) => c.as[Event.CustomIssueTypeCreate]
    case ("CustomIssueTypeDelete"  , c) => c.as[Event.CustomIssueTypeDelete]
    case ("CustomIssueTypeRestore" , c) => c.as[Event.CustomIssueTypeRestore]
    case ("CustomIssueTypeUpdate"  , c) => c.as[Event.CustomIssueTypeUpdate]
    case ("CustomReqTypeCreate:2"  , c) => c.as[Event.CustomReqTypeCreate]
    case ("CustomReqTypeCreate"    , c) => c.as[Event.CustomReqTypeCreateV1]
    case ("CustomReqTypeDelete"    , c) => c.as[Event.CustomReqTypeDelete]
    case ("CustomReqTypeDeleteHard", c) => c.as[Event.CustomReqTypeDeleteHard]
    case ("CustomReqTypeDeleteSoft", c) => c.as[Event.CustomReqTypeDeleteSoft]
    case ("CustomReqTypeRestore"   , c) => c.as[Event.CustomReqTypeRestore]
    case ("CustomReqTypeUpdate:2"  , c) => c.as[Event.CustomReqTypeUpdate]
    case ("CustomReqTypeUpdate"    , c) => c.as[Event.CustomReqTypeUpdateV1]
    case ("FieldCustomDelete"      , c) => c.as[Event.FieldCustomDelete]
    case ("FieldCustomImpCreate:2" , c) => c.as[Event.FieldCustomImpCreate]
    case ("FieldCustomImpCreate"   , c) => c.as[Event.FieldCustomImpCreateV1]
    case ("FieldCustomImpUpdate:2" , c) => c.as[Event.FieldCustomImpUpdate]
    case ("FieldCustomImpUpdate"   , c) => c.as[Event.FieldCustomImpUpdateV1]
    case ("FieldCustomNumberCreate", c) => c.as[Event.FieldCustomNumberCreate]
    case ("FieldCustomNumberUpdate", c) => c.as[Event.FieldCustomNumberUpdate]
    case ("FieldCustomRestore"     , c) => c.as[Event.FieldCustomRestore]
    case ("FieldCustomTagCreate:2" , c) => c.as[Event.FieldCustomTagCreate]
    case ("FieldCustomTagCreate"   , c) => c.as[Event.FieldCustomTagCreateV1]
    case ("FieldCustomTagUpdate:2" , c) => c.as[Event.FieldCustomTagUpdate]
    case ("FieldCustomTagUpdate"   , c) => c.as[Event.FieldCustomTagUpdateV1]
    case ("FieldCustomTextCreate:2", c) => c.as[Event.FieldCustomTextCreate]
    case ("FieldCustomTextCreate"  , c) => c.as[Event.FieldCustomTextCreateV1]
    case ("FieldCustomTextUpdate:2", c) => c.as[Event.FieldCustomTextUpdate]
    case ("FieldCustomTextUpdate"  , c) => c.as[Event.FieldCustomTextUpdateV1]
    case ("FieldReposition"        , c) => c.as[Event.FieldReposition]
    case ("FieldStaticAdd"         , c) => c.as[Event.FieldStaticAdd]
    case ("FieldStaticRemove"      , c) => c.as[Event.FieldStaticRemove]
    case ("GenericReqCreate"       , c) => c.as[Event.GenericReqCreate]
    case ("GenericReqTitleSet"     , c) => c.as[Event.GenericReqTitleSet]
    case ("GenericReqTypeSet"      , c) => c.as[Event.GenericReqTypeSet]
    case ("ManualIssueCreate"      , c) => c.as[Event.ManualIssueCreate]
    case ("ManualIssueDelete"      , c) => c.as[Event.ManualIssueDelete]
    case ("ManualIssueUpdate"      , c) => c.as[Event.ManualIssueUpdate]
    case ("ProjectDelete"          , c) => c.as[Event.ProjectDelete]
    case ("ProjectNameSet"         , c) => c.as[Event.ProjectNameSet]
    case ("ProjectRestore"         , c) => c.as[Event.ProjectRestore.type]
    case ("ProjectTemplateApply"   , c) => c.as[Event.ProjectTemplateApply]
    case ("ReqCodesPatch"          , c) => c.as[Event.ReqCodesPatch]
    case ("ReqFieldCustomNumberSet", c) => c.as[Event.ReqFieldCustomNumberSet]
    case ("ReqFieldCustomTextSet"  , c) => c.as[Event.ReqFieldCustomTextSet]
    case ("ReqImplicationsPatch"   , c) => c.as[Event.ReqImplicationsPatch]
    case ("ReqsDelete"             , c) => c.as[Event.ReqsDelete]
    case ("ReqTagsPatch"           , c) => c.as[Event.ReqTagsPatch]
    case ("SavedViewCreate:2"      , c) => c.as[Event.SavedViewCreate]
    case ("SavedViewCreate"        , c) => c.as[Event.SavedViewCreateV1]
    case ("SavedViewDefaultSet"    , c) => c.as[Event.SavedViewDefaultSet]
    case ("SavedViewDelete"        , c) => c.as[Event.SavedViewDelete]
    case ("SavedViewUpdate:2"      , c) => c.as[Event.SavedViewUpdate]
    case ("SavedViewUpdate"        , c) => c.as[Event.SavedViewUpdateV1]
    case ("TagDelete"              , c) => c.as[Event.TagDelete]
    case ("TagGroupCreate"         , c) => c.as[Event.TagGroupCreate]
    case ("TagGroupUpdate"         , c) => c.as[Event.TagGroupUpdate]
    case ("TagRestore"             , c) => c.as[Event.TagRestore]
    case ("UseCaseCreate"          , c) => c.as[Event.UseCaseCreate]
    case ("UseCaseStepCreate"      , c) => c.as[Event.UseCaseStepCreate]
    case ("UseCaseStepDelete"      , c) => c.as[Event.UseCaseStepDelete]
    case ("UseCaseStepRestore"     , c) => c.as[Event.UseCaseStepRestore]
    case ("UseCaseStepShiftLeft"   , c) => c.as[Event.UseCaseStepShiftLeft]
    case ("UseCaseStepShiftRight"  , c) => c.as[Event.UseCaseStepShiftRight]
    case ("UseCaseStepUpdate"      , c) => c.as[Event.UseCaseStepUpdate]
    case ("UseCaseTitleSet"        , c) => c.as[Event.UseCaseTitleSet]
  }

  implicit lazy val encoderEvent: Encoder[Event] = Encoder.instance {
    case a: Event.AccessUpdate            => Json.obj("AccessUpdate"            -> a.asJson)
    case a: Event.ApplicableTagCreate     => Json.obj("ApplicableTagCreate:2"   -> a.asJson)
    case a: Event.ApplicableTagCreateV1   => Json.obj("ApplicableTagCreate"     -> a.asJson)
    case a: Event.ApplicableTagUpdate     => Json.obj("ApplicableTagUpdate:2"   -> a.asJson)
    case a: Event.ApplicableTagUpdateV1   => Json.obj("ApplicableTagUpdate"     -> a.asJson)
    case a: Event.CodeGroupCreate         => Json.obj("CodeGroupCreate"         -> a.asJson)
    case a: Event.CodeGroupsDelete        => Json.obj("CodeGroupsDelete"        -> a.asJson)
    case a: Event.CodeGroupUpdate         => Json.obj("CodeGroupUpdate"         -> a.asJson)
    case a: Event.ContentRestore          => Json.obj("ContentRestore"          -> a.asJson)
    case a: Event.CustomIssueTypeCreate   => Json.obj("CustomIssueTypeCreate"   -> a.asJson)
    case a: Event.CustomIssueTypeDelete   => Json.obj("CustomIssueTypeDelete"   -> a.asJson)
    case a: Event.CustomIssueTypeRestore  => Json.obj("CustomIssueTypeRestore"  -> a.asJson)
    case a: Event.CustomIssueTypeUpdate   => Json.obj("CustomIssueTypeUpdate"   -> a.asJson)
    case a: Event.CustomReqTypeCreate     => Json.obj("CustomReqTypeCreate:2"   -> a.asJson)
    case a: Event.CustomReqTypeCreateV1   => Json.obj("CustomReqTypeCreate"     -> a.asJson)
    case a: Event.CustomReqTypeDelete     => Json.obj("CustomReqTypeDelete"     -> a.asJson)
    case a: Event.CustomReqTypeDeleteHard => Json.obj("CustomReqTypeDeleteHard" -> a.asJson)
    case a: Event.CustomReqTypeDeleteSoft => Json.obj("CustomReqTypeDeleteSoft" -> a.asJson)
    case a: Event.CustomReqTypeRestore    => Json.obj("CustomReqTypeRestore"    -> a.asJson)
    case a: Event.CustomReqTypeUpdate     => Json.obj("CustomReqTypeUpdate:2"   -> a.asJson)
    case a: Event.CustomReqTypeUpdateV1   => Json.obj("CustomReqTypeUpdate"     -> a.asJson)
    case a: Event.FieldCustomDelete       => Json.obj("FieldCustomDelete"       -> a.asJson)
    case a: Event.FieldCustomImpCreate    => Json.obj("FieldCustomImpCreate:2"  -> a.asJson)
    case a: Event.FieldCustomImpCreateV1  => Json.obj("FieldCustomImpCreate"    -> a.asJson)
    case a: Event.FieldCustomImpUpdate    => Json.obj("FieldCustomImpUpdate:2"  -> a.asJson)
    case a: Event.FieldCustomImpUpdateV1  => Json.obj("FieldCustomImpUpdate"    -> a.asJson)
    case a: Event.FieldCustomNumberCreate => Json.obj("FieldCustomNumberCreate" -> a.asJson)
    case a: Event.FieldCustomNumberUpdate => Json.obj("FieldCustomNumberUpdate" -> a.asJson)
    case a: Event.FieldCustomRestore      => Json.obj("FieldCustomRestore"      -> a.asJson)
    case a: Event.FieldCustomTagCreate    => Json.obj("FieldCustomTagCreate:2"  -> a.asJson)
    case a: Event.FieldCustomTagCreateV1  => Json.obj("FieldCustomTagCreate"    -> a.asJson)
    case a: Event.FieldCustomTagUpdate    => Json.obj("FieldCustomTagUpdate:2"  -> a.asJson)
    case a: Event.FieldCustomTagUpdateV1  => Json.obj("FieldCustomTagUpdate"    -> a.asJson)
    case a: Event.FieldCustomTextCreate   => Json.obj("FieldCustomTextCreate:2" -> a.asJson)
    case a: Event.FieldCustomTextCreateV1 => Json.obj("FieldCustomTextCreate"   -> a.asJson)
    case a: Event.FieldCustomTextUpdate   => Json.obj("FieldCustomTextUpdate:2" -> a.asJson)
    case a: Event.FieldCustomTextUpdateV1 => Json.obj("FieldCustomTextUpdate"   -> a.asJson)
    case a: Event.FieldReposition         => Json.obj("FieldReposition"         -> a.asJson)
    case a: Event.FieldStaticAdd          => Json.obj("FieldStaticAdd"          -> a.asJson)
    case a: Event.FieldStaticRemove       => Json.obj("FieldStaticRemove"       -> a.asJson)
    case a: Event.GenericReqCreate        => Json.obj("GenericReqCreate"        -> a.asJson)
    case a: Event.GenericReqTitleSet      => Json.obj("GenericReqTitleSet"      -> a.asJson)
    case a: Event.GenericReqTypeSet       => Json.obj("GenericReqTypeSet"       -> a.asJson)
    case a: Event.ManualIssueCreate       => Json.obj("ManualIssueCreate"       -> a.asJson)
    case a: Event.ManualIssueDelete       => Json.obj("ManualIssueDelete"       -> a.asJson)
    case a: Event.ManualIssueUpdate       => Json.obj("ManualIssueUpdate"       -> a.asJson)
    case a: Event.ProjectDelete           => Json.obj("ProjectDelete"           -> a.asJson)
    case a: Event.ProjectNameSet          => Json.obj("ProjectNameSet"          -> a.asJson)
    case a: Event.ProjectRestore.type     => Json.obj("ProjectRestore"          -> a.asJson)
    case a: Event.ProjectTemplateApply    => Json.obj("ProjectTemplateApply"    -> a.asJson)
    case a: Event.ReqCodesPatch           => Json.obj("ReqCodesPatch"           -> a.asJson)
    case a: Event.ReqFieldCustomNumberSet => Json.obj("ReqFieldCustomNumberSet" -> a.asJson)
    case a: Event.ReqFieldCustomTextSet   => Json.obj("ReqFieldCustomTextSet"   -> a.asJson)
    case a: Event.ReqImplicationsPatch    => Json.obj("ReqImplicationsPatch"    -> a.asJson)
    case a: Event.ReqsDelete              => Json.obj("ReqsDelete"              -> a.asJson)
    case a: Event.ReqTagsPatch            => Json.obj("ReqTagsPatch"            -> a.asJson)
    case a: Event.SavedViewCreate         => Json.obj("SavedViewCreate:2"       -> a.asJson)
    case a: Event.SavedViewCreateV1       => Json.obj("SavedViewCreate"         -> a.asJson)
    case a: Event.SavedViewDefaultSet     => Json.obj("SavedViewDefaultSet"     -> a.asJson)
    case a: Event.SavedViewDelete         => Json.obj("SavedViewDelete"         -> a.asJson)
    case a: Event.SavedViewUpdate         => Json.obj("SavedViewUpdate:2"       -> a.asJson)
    case a: Event.SavedViewUpdateV1       => Json.obj("SavedViewUpdate"         -> a.asJson)
    case a: Event.TagDelete               => Json.obj("TagDelete"               -> a.asJson)
    case a: Event.TagGroupCreate          => Json.obj("TagGroupCreate"          -> a.asJson)
    case a: Event.TagGroupUpdate          => Json.obj("TagGroupUpdate"          -> a.asJson)
    case a: Event.TagRestore              => Json.obj("TagRestore"              -> a.asJson)
    case a: Event.UseCaseCreate           => Json.obj("UseCaseCreate"           -> a.asJson)
    case a: Event.UseCaseStepCreate       => Json.obj("UseCaseStepCreate"       -> a.asJson)
    case a: Event.UseCaseStepDelete       => Json.obj("UseCaseStepDelete"       -> a.asJson)
    case a: Event.UseCaseStepRestore      => Json.obj("UseCaseStepRestore"      -> a.asJson)
    case a: Event.UseCaseStepShiftLeft    => Json.obj("UseCaseStepShiftLeft"    -> a.asJson)
    case a: Event.UseCaseStepShiftRight   => Json.obj("UseCaseStepShiftRight"   -> a.asJson)
    case a: Event.UseCaseStepUpdate       => Json.obj("UseCaseStepUpdate"       -> a.asJson)
    case a: Event.UseCaseTitleSet         => Json.obj("UseCaseTitleSet"         -> a.asJson)
  }

  implicit lazy val decoderVerifiedEvent: Decoder[VerifiedEvent] =
    Decoder.forProduct4("#", "event", "author", "createdAt")(VerifiedEvent.apply)

  implicit lazy val encoderVerifiedEvent: Encoder[VerifiedEvent] =
    Encoder.forProduct4("#", "event", "author", "createdAt")(a => (a.ord, a.event, a.author, a.createdAt))

}
