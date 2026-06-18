package shipreq.webapp.member.project.protocol.binary.v2

import java.time.Instant
import shipreq.base.util.ErrorMsg
import shipreq.webapp.base.data._
import shipreq.webapp.base.util.On
import shipreq.webapp.member.project.data.{Live => _, _}
import shipreq.webapp.member.project.data.savedview.{ImpGraphConfig, SavedView}
import shipreq.webapp.member.project.event._
import shipreq.webapp.member.project.filter.Filter
import shipreq.webapp.member.project.sort.SortMethod
import shipreq.webapp.member.project.text.Text

/** v2.1
  *
  * - Added CustomFieldType.Number
  * - Added CustomField.Number
  * - Added CustomField.Number.Id
  * - Added Event.ProjectDelete
  * - Added Event.ProjectRestore
  * - Added Project.deletionReason
  * - Added ProjectContent.reqNums (and deps)
  * - Added ProjectMetaData.live
  */
object Rev1 {
  import boopickle.DefaultBasic._
  import shipreq.webapp.base.protocol.binary.v1.BaseData._
  import shipreq.webapp.member.project.protocol.binary.v1.BaseMemberData1._
  import shipreq.webapp.member.project.protocol.binary.v1.BaseMemberData1.SavedViewPicklers._
  import shipreq.webapp.member.project.protocol.binary.v1.BaseMemberData2._
  import shipreq.webapp.member.project.protocol.binary.v1.Rev1._
  import shipreq.webapp.member.project.protocol.binary.v1.Rev1.SavedViewPicklers._
  import shipreq.webapp.member.project.protocol.binary.v1.Rev4._
  import shipreq.webapp.member.project.protocol.binary.v1.Rev6._
  import shipreq.webapp.member.project.protocol.binary.v1.Events._
  import shipreq.webapp.member.project.protocol.binary.v1.PostEvents._
  import shipreq.webapp.member.project.protocol.binary.v2.Rev0._
  import AtomPicklers.instances._
  import this.SavedViewPicklers._

  // ===================================================================================================================
  // Saved view data

  object SavedViewPicklers {
    import shipreq.webapp.member.project.protocol.binary.v1.BaseMemberData1.SavedViewPicklers._
    import shipreq.webapp.member.project.data.savedview._

    implicit val picklerColumnCustomField: Pickler[Column.CustomField] =
      transformPickler(Column.CustomField.apply)(_.id)

    implicit val picklerColumn: Pickler[Column] =
      new Pickler[Column] {
        import Column._
        private[this] final val KeyCode           = 0
        private[this] final val KeyCustomField    = 1
        private[this] final val KeyDeletionReason = 2
        private[this] final val KeyImplications   = 3
        private[this] final val KeyPubid          = 4
        private[this] final val KeyReqType        = 5
        private[this] final val KeyOtherTags      = 6
        private[this] final val KeyTitle          = 7
        private[this] final val KeyAllTags        = 8
        override def pickle(a: Column)(implicit state: PickleState): Unit =
          a match {
            case Code              => state.enc.writeByte(KeyCode          )
            case b: CustomField    => state.enc.writeByte(KeyCustomField   ); state.pickle(b)
            case DeletionReason    => state.enc.writeByte(KeyDeletionReason)
            case b: Implications   => state.enc.writeByte(KeyImplications  ); state.pickle(b)
            case Pubid             => state.enc.writeByte(KeyPubid         )
            case ReqType           => state.enc.writeByte(KeyReqType       )
            case OtherTags         => state.enc.writeByte(KeyOtherTags     )
            case AllTags           => state.enc.writeByte(KeyAllTags       )
            case Title             => state.enc.writeByte(KeyTitle         )
          }
        override def unpickle(implicit state: UnpickleState): Column =
          state.dec.readByte match {
            case KeyCode           => Code
            case KeyCustomField    => state.unpickle[CustomField]
            case KeyDeletionReason => DeletionReason
            case KeyImplications   => state.unpickle[Implications]
            case KeyPubid          => Pubid
            case KeyReqType        => ReqType
            case KeyOtherTags      => OtherTags
            case KeyAllTags        => AllTags
            case KeyTitle          => Title
          }
      }

    implicit val picklerColumnSortInconclusive: Pickler[Column.SortInconclusive] =
      new Pickler[Column.SortInconclusive] {
        private[this] final val KeyCode           = 0
        private[this] final val KeyCustomField    = 1
        private[this] final val KeyDeletionReason = 2
        private[this] final val KeyImplications   = 3
        private[this] final val KeyReqType        = 4
        private[this] final val KeyOtherTags      = 5
        private[this] final val KeyTitle          = 6
        private[this] final val KeyAllTags        = 7
        override def pickle(a: Column.SortInconclusive)(implicit state: PickleState): Unit =
          a match {
            case Column.Code              => state.enc.writeByte(KeyCode          )
            case b: Column.CustomField    => state.enc.writeByte(KeyCustomField   ); state.pickle(b)
            case Column.DeletionReason    => state.enc.writeByte(KeyDeletionReason)
            case b: Column.Implications   => state.enc.writeByte(KeyImplications  ); state.pickle(b)
            case Column.ReqType           => state.enc.writeByte(KeyReqType       )
            case Column.OtherTags         => state.enc.writeByte(KeyOtherTags     )
            case Column.AllTags           => state.enc.writeByte(KeyAllTags       )
            case Column.Title             => state.enc.writeByte(KeyTitle         )
          }
        override def unpickle(implicit state: UnpickleState): Column.SortInconclusive =
          state.dec.readByte match {
            case KeyCode           => Column.Code
            case KeyCustomField    => state.unpickle[Column.CustomField]
            case KeyDeletionReason => Column.DeletionReason
            case KeyImplications   => state.unpickle[Column.Implications]
            case KeyReqType        => Column.ReqType
            case KeyOtherTags      => Column.OtherTags
            case KeyAllTags        => Column.AllTags
            case KeyTitle          => Column.Title
          }
      }

    implicit val picklerColumnSortInconclusiveHasBlanks: Pickler[Column.SortInconclusiveHasBlanks] =
      new Pickler[Column.SortInconclusiveHasBlanks] {
        private[this] final val KeyCode           = 0
        private[this] final val KeyCustomField    = 1
        private[this] final val KeyDeletionReason = 2
        private[this] final val KeyImplications   = 3
        private[this] final val KeyOtherTags      = 4
        private[this] final val KeyTitle          = 5
        private[this] final val KeyAllTags        = 6
        override def pickle(a: Column.SortInconclusiveHasBlanks)(implicit state: PickleState): Unit =
          a match {
            case Column.Code              => state.enc.writeByte(KeyCode          )
            case b: Column.CustomField    => state.enc.writeByte(KeyCustomField   ); state.pickle(b)
            case Column.DeletionReason    => state.enc.writeByte(KeyDeletionReason)
            case b: Column.Implications   => state.enc.writeByte(KeyImplications  ); state.pickle(b)
            case Column.OtherTags         => state.enc.writeByte(KeyOtherTags     )
            case Column.AllTags           => state.enc.writeByte(KeyAllTags       )
            case Column.Title             => state.enc.writeByte(KeyTitle         )
          }
        override def unpickle(implicit state: UnpickleState): Column.SortInconclusiveHasBlanks =
          state.dec.readByte match {
            case KeyCode           => Column.Code
            case KeyCustomField    => state.unpickle[Column.CustomField]
            case KeyDeletionReason => Column.DeletionReason
            case KeyImplications   => state.unpickle[Column.Implications]
            case KeyOtherTags      => Column.OtherTags
            case KeyAllTags        => Column.AllTags
            case KeyTitle          => Column.Title
          }
      }

    implicit val pickleColumnSIs: Pickler[Vector[Column.SortInconclusive]] =
      iterablePickler

    implicit val pickleColumnNEV: Pickler[NonEmptyVector[Column]] =
      pickleNEV


    implicit val picklerSortCriterionInconclusiveCB: Pickler[SortCriterion.InconclusiveCB] =
      new Pickler[SortCriterion.InconclusiveCB] {
        override def pickle(a: SortCriterion.InconclusiveCB)(implicit state: PickleState): Unit = {
          state.pickle(a.column)
          state.pickle(a.method)
        }
        override def unpickle(implicit state: UnpickleState): SortCriterion.InconclusiveCB = {
          val column = state.unpickle[Column.SortInconclusiveHasBlanks]
          val method = state.unpickle[SortMethod.ConsiderBlanks]
          SortCriterion.InconclusiveCB(column, method)
        }
      }

    implicit val picklerSortCriterionInconclusive: Pickler[SortCriterion.Inconclusive] =
      new Pickler[SortCriterion.Inconclusive] {
        private[this] final val KeyInconclusiveCB = 0
        private[this] final val KeyInconclusiveIB = 1
        override def pickle(a: SortCriterion.Inconclusive)(implicit state: PickleState): Unit =
          a match {
            case b: SortCriterion.InconclusiveCB => state.enc.writeByte(KeyInconclusiveCB); state.pickle(b)
            case b: SortCriterion.InconclusiveIB => state.enc.writeByte(KeyInconclusiveIB); state.pickle(b)
          }
        override def unpickle(implicit state: UnpickleState): SortCriterion.Inconclusive =
          state.dec.readByte match {
            case KeyInconclusiveCB => state.unpickle[SortCriterion.InconclusiveCB]
            case KeyInconclusiveIB => state.unpickle[SortCriterion.InconclusiveIB]
          }
      }

    implicit val picklerSortCriterion: Pickler[SortCriterion] =
      new Pickler[SortCriterion] {
        import SortCriterion._
        private[this] final val KeyConclusive     = 0
        private[this] final val KeyInconclusiveCB = 1
        private[this] final val KeyInconclusiveIB = 2
        override def pickle(a: SortCriterion)(implicit state: PickleState): Unit =
          a match {
            case b: Conclusive     => state.enc.writeByte(KeyConclusive    ); state.pickle(b)
            case b: InconclusiveCB => state.enc.writeByte(KeyInconclusiveCB); state.pickle(b)
            case b: InconclusiveIB => state.enc.writeByte(KeyInconclusiveIB); state.pickle(b)
          }
        override def unpickle(implicit state: UnpickleState): SortCriterion =
          state.dec.readByte match {
            case KeyConclusive     => state.unpickle[Conclusive]
            case KeyInconclusiveCB => state.unpickle[InconclusiveCB]
            case KeyInconclusiveIB => state.unpickle[InconclusiveIB]
          }
      }

    implicit val pickleSortCriterionIs: Pickler[Vector[SortCriterion.Inconclusive]] =
      iterablePickler

    implicit val picklerSortCriteria: Pickler[SortCriteria] =
      new Pickler[SortCriteria] {
        override def pickle(a: SortCriteria)(implicit state: PickleState): Unit = {
          state.pickle(a.init)
          state.pickle(a.last)
        }
        override def unpickle(implicit state: UnpickleState): SortCriteria = {
          val init = state.unpickle[Vector[SortCriterion.Inconclusive]]
          val last = state.unpickle[SortCriterion.Conclusive]
          SortCriteria(init, last)
        }
      }

    implicit val picklerView: Pickler[View] =
      new Pickler[View] {
        override def pickle(a: View)(implicit state: PickleState): Unit = {
          writeVersion(1) // v1.1
          state.pickle(a.columns)
          state.pickle(a.order)
          state.pickle(a.filterDead)
          state.pickle(a.filter)
          state.pickle(a.impGraphConfig)
        }
        override def unpickle(implicit state: UnpickleState): View =
          readByVersion(1) {

            // v1.0
            case 0 =>
              val columns    = state.unpickle[NonEmptyVector[Column]]
              val order      = state.unpickle[SortCriteria]
              val filterDead = state.unpickle[FilterDead]
              val filter     = state.unpickle[Option[Filter.Valid]]
              View(columns, order, filterDead, filter, None)

            // v1.1
            case 1 =>
              val columns        = state.unpickle[NonEmptyVector[Column]]
              val order          = state.unpickle[SortCriteria]
              val filterDead     = state.unpickle[FilterDead]
              val filter         = state.unpickle[Option[Filter.Valid]]
              val impGraphConfig = state.unpickle[Option[ImpGraphConfig]]
              View(columns, order, filterDead, filter, impGraphConfig)
          }
      }

    implicit val picklerSavedView: Pickler[SavedView] =
      new Pickler[SavedView] {
        override def pickle(a: SavedView)(implicit state: PickleState): Unit = {
          state.pickle(a.id)
          state.pickle(a.name)
          state.pickle(a.view)
        }
        override def unpickle(implicit state: UnpickleState): SavedView = {
          val id   = state.unpickle[SavedView.Id]
          val name = state.unpickle[SavedView.Name]
          val view = state.unpickle[View]
          SavedView(id, name, view)
        }
      }

    implicit val pickleSavedViewsND: Pickler[SavedViews.NonDefault] =
      pickleIMap(SavedViews.emptyNonDefault)

    implicit val pickleSavedViews: Pickler[SavedViews.NonEmpty] =
      new Pickler[SavedViews.NonEmpty] {
        override def pickle(a: SavedViews.NonEmpty)(implicit state: PickleState): Unit = {
          state.pickle(a.default)
          state.pickle(a.nonDefault)
        }
        override def unpickle(implicit state: UnpickleState): SavedViews.NonEmpty = {
          val default    = state.unpickle[SavedView]
          val nonDefault = state.unpickle[SavedViews.NonDefault]
          SavedViews.NonEmpty(default, nonDefault)
        }
      }
  }

  // ===================================================================================================================
  // Project data

  implicit lazy val picklerCustomFieldNumberId: Pickler[CustomField.Number.Id] =
    pickleTaggedI(CustomField.Number.Id).reuseByUnivEq

  implicit lazy val picklerCustomFieldType: Pickler[CustomFieldType] =
    new Pickler[CustomFieldType] {
      private[this] final val KeyImplication = 'i'
      private[this] final val KeyNumber      = 'n'
      private[this] final val KeyTag         = 't'
      private[this] final val KeyText        = 'x'
      override def pickle(a: CustomFieldType)(implicit state: PickleState): Unit =
        a match {
          case CustomFieldType.Implication => state.enc.writeByte(KeyImplication)
          case CustomFieldType.Number      => state.enc.writeByte(KeyNumber     )
          case CustomFieldType.Tag         => state.enc.writeByte(KeyTag        )
          case CustomFieldType.Text        => state.enc.writeByte(KeyText       )
        }
      override def unpickle(implicit state: UnpickleState): CustomFieldType =
        state.dec.readByte match {
          case KeyImplication => CustomFieldType.Implication
          case KeyNumber      => CustomFieldType.Number
          case KeyTag         => CustomFieldType.Tag
          case KeyText        => CustomFieldType.Text
        }
    }

  implicit lazy val picklerCustomFieldId: Pickler[CustomFieldId] =
    new Pickler[CustomFieldId] {
      private[this] final val KeyImplication = 'i'
      private[this] final val KeyNumber      = 'n'
      private[this] final val KeyTag         = 't'
      private[this] final val KeyText        = 'x'
      override def pickle(a: CustomFieldId)(implicit state: PickleState): Unit =
        a match {
          case b: CustomField.Implication.Id => state.enc.writeByte(KeyImplication); state.pickle(b)
          case b: CustomField.Number     .Id => state.enc.writeByte(KeyNumber     ); state.pickle(b)
          case b: CustomField.Tag        .Id => state.enc.writeByte(KeyTag        ); state.pickle(b)
          case b: CustomField.Text       .Id => state.enc.writeByte(KeyText       ); state.pickle(b)
        }
      override def unpickle(implicit state: UnpickleState): CustomFieldId =
        state.dec.readByte match {
          case KeyImplication => state.unpickle[CustomField.Implication.Id]
          case KeyNumber      => state.unpickle[CustomField.Number     .Id]
          case KeyTag         => state.unpickle[CustomField.Tag        .Id]
          case KeyText        => state.unpickle[CustomField.Text       .Id]
        }
    }

  implicit lazy val picklerCustomFieldNumber: Pickler[CustomField.Number] =
    new Pickler[CustomField.Number] {
      override def pickle(a: CustomField.Number)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.name)
        state.pickle(a.desc)
        state.pickle(a.min)
        state.pickle(a.max)
        state.pickle(a.decimalPlaces)
        state.pickle(a.fieldReqTypeRules)
        state.pickle(a.liveExplicitly)
      }
      override def unpickle(implicit state: UnpickleState): CustomField.Number = {
        val id                = state.unpickle[CustomField.Number.Id]
        val name              = state.unpickle[String]
        val desc              = state.unpickle[Option[String]]
        val min               = state.unpickle[Double]
        val max               = state.unpickle[Double]
        val decimalPlaces     = state.unpickle[Int]
        val fieldReqTypeRules = state.unpickle[FieldReqTypeRules.ForNumField]
        val liveExplicitly    = state.unpickle[Live]
        CustomField.Number(id, name, desc, (min, max), decimalPlaces, fieldReqTypeRules, liveExplicitly)
      }
    }

  implicit lazy val picklerCustomField: Pickler[CustomField] =
    new Pickler[CustomField] {
      private[this] final val KeyImplication = 'i'
      private[this] final val KeyNumber      = 'n'
      private[this] final val KeyTag         = 't'
      private[this] final val KeyText        = 'x'
      override def pickle(a: CustomField)(implicit state: PickleState): Unit =
        a match {
          case b: CustomField.Implication => state.enc.writeByte(KeyImplication); state.pickle(b)
          case b: CustomField.Number      => state.enc.writeByte(KeyNumber     ); state.pickle(b)
          case b: CustomField.Tag         => state.enc.writeByte(KeyTag        ); state.pickle(b)
          case b: CustomField.Text        => state.enc.writeByte(KeyText       ); state.pickle(b)
        }
      override def unpickle(implicit state: UnpickleState): CustomField =
        state.dec.readByte match {
          case KeyImplication => state.unpickle[CustomField.Implication]
          case KeyNumber      => state.unpickle[CustomField.Number     ]
          case KeyTag         => state.unpickle[CustomField.Tag        ]
          case KeyText        => state.unpickle[CustomField.Text       ]
        }
    }

  implicit lazy val picklerFieldId: Pickler[FieldId] =
    new Pickler[FieldId] {
      private[this] final val KeyCustomImplication       = 'i'
      private[this] final val KeyCustomNumber            = 'n'
      private[this] final val KeyCustomTag               = 't'
      private[this] final val KeyCustomText              = 'x'
      private[this] final val KeyStaticExceptionStepTree = 'E'
      private[this] final val KeyStaticImplicationGraph  = 'I'
      private[this] final val KeyStaticNormalAltStepTree = 'N'
      private[this] final val KeyStaticStepGraph         = 'G'
      private[this] final val KeyStaticOtherTags         = '#'
      private[this] final val KeyStaticAllTags           = 'T'
      override def pickle(a: FieldId)(implicit state: PickleState): Unit =
        a match {
          case b: CustomField.Implication.Id => state.enc.writeByte(KeyCustomImplication          ); state.pickle(b)
          case b: CustomField.Number     .Id => state.enc.writeByte(KeyCustomNumber               ); state.pickle(b)
          case b: CustomField.Tag        .Id => state.enc.writeByte(KeyCustomTag                  ); state.pickle(b)
          case b: CustomField.Text       .Id => state.enc.writeByte(KeyCustomText                 ); state.pickle(b)
          case StaticField.ExceptionStepTree => state.enc.writeByte(KeyStaticExceptionStepTree    )
          case StaticField.ImplicationGraph  => state.enc.writeByte(KeyStaticImplicationGraph     )
          case StaticField.NormalAltStepTree => state.enc.writeByte(KeyStaticNormalAltStepTree    )
          case StaticField.StepGraph         => state.enc.writeByte(KeyStaticStepGraph            )
          case StaticField.OtherTags         => state.enc.writeByte(KeyStaticOtherTags            )
          case StaticField.AllTags           => state.enc.writeByte(KeyStaticAllTags              )
        }
      override def unpickle(implicit state: UnpickleState): FieldId =
        state.dec.readByte match {
          case KeyCustomImplication       => state.unpickle[CustomField.Implication.Id]
          case KeyCustomNumber            => state.unpickle[CustomField.Number     .Id]
          case KeyCustomTag               => state.unpickle[CustomField.Tag        .Id]
          case KeyCustomText              => state.unpickle[CustomField.Text       .Id]
          case KeyStaticExceptionStepTree => StaticField.ExceptionStepTree
          case KeyStaticImplicationGraph  => StaticField.ImplicationGraph
          case KeyStaticNormalAltStepTree => StaticField.NormalAltStepTree
          case KeyStaticStepGraph         => StaticField.StepGraph
          case KeyStaticOtherTags         => StaticField.OtherTags
          case KeyStaticAllTags           => StaticField.AllTags
        }
    }

  implicit lazy val picklerFieldType: Pickler[FieldType] =
    new Pickler[FieldType] {
      private[this] final val KeyImplication      = 'i'
      private[this] final val KeyImplicationGraph = 'I'
      private[this] final val KeyUseCaseStepGraph = 'G'
      private[this] final val KeyUseCaseSteps     = 'T'
      private[this] final val KeyCustomTag        = 't'
      private[this] final val KeyNumber           = 'n'
      private[this] final val KeyText             = 'x'
      private[this] final val KeyStaticTag        = '#'
      override def pickle(a: FieldType)(implicit state: PickleState): Unit =
        a match {
          case CustomFieldType.Implication      => state.enc.writeByte(KeyImplication     )
          case StaticFieldType.ImplicationGraph => state.enc.writeByte(KeyImplicationGraph)
          case StaticFieldType.UseCaseSteps     => state.enc.writeByte(KeyUseCaseSteps    )
          case StaticFieldType.UseCaseStepGraph => state.enc.writeByte(KeyUseCaseStepGraph)
          case CustomFieldType.Number           => state.enc.writeByte(KeyNumber          )
          case CustomFieldType.Text             => state.enc.writeByte(KeyText            )
          case CustomFieldType.Tag              => state.enc.writeByte(KeyCustomTag       )
          case StaticFieldType.Tag              => state.enc.writeByte(KeyStaticTag       )
        }
      override def unpickle(implicit state: UnpickleState): FieldType =
        state.dec.readByte match {
          case KeyImplication      => CustomFieldType.Implication
          case KeyImplicationGraph => StaticFieldType.ImplicationGraph
          case KeyUseCaseSteps     => StaticFieldType.UseCaseSteps
          case KeyUseCaseStepGraph => StaticFieldType.UseCaseStepGraph
          case KeyNumber           => CustomFieldType.Number
          case KeyText             => CustomFieldType.Text
          case KeyCustomTag        => CustomFieldType.Tag
          case KeyStaticTag        => StaticFieldType.Tag
        }
    }

  implicit lazy val picklerFieldSetCustomFields: Pickler[FieldSet.CustomFields] =
    pickleIMap(FieldSet.emptyCustomFields)

  implicit lazy val picklerFieldSet: Pickler[FieldSet] =
    new Pickler[FieldSet] {
      override def pickle(a: FieldSet)(implicit state: PickleState): Unit = {
        state.pickle(a.customFields)
        state.pickle(a.order)
      }
      override def unpickle(implicit state: UnpickleState): FieldSet = {
        val customFields = state.unpickle[FieldSet.CustomFields]
        val order        = state.unpickle[FieldSet.Order]
        FieldSet(customFields, order)
      }
    }

  implicit lazy val picklerProjectConfig: Pickler[ProjectConfig] =
    new Pickler[ProjectConfig] {
      override def pickle(a: ProjectConfig)(implicit state: PickleState): Unit = {
        state.pickle(a.customIssueTypes)
        state.pickle(a.reqTypes)
        state.pickle(a.fields)
        state.pickle(a.tags)
      }
      override def unpickle(implicit state: UnpickleState): ProjectConfig = {
        val customIssueTypes = state.unpickle[CustomIssueTypeIMap]
        val reqTypes         = state.unpickle[ReqTypes]
        val fields           = state.unpickle[FieldSet]
        val tags             = state.unpickle[Tags]
        ProjectConfig(customIssueTypes, reqTypes, fields, tags)
      }
    }

  implicit lazy val picklerReqDataNumbers: Pickler[ReqData.Numbers] = {
    implicit val picklerValues: Pickler[Map[ReqId, Double]] = pickleMap
    pickleMap
  }

  implicit lazy val picklerProjectContent: Pickler[ProjectContent] =
    new Pickler[ProjectContent] {
      override def pickle(a: ProjectContent)(implicit state: PickleState): Unit = {
        writeVersion(1)
        state.pickle(a.reqs)
        state.pickle(a.reqCodes)
        state.pickle(a.reqNums)
        state.pickle(a.reqText)
        state.pickle(a.reqTags)
        state.pickle(a.implications)
        state.pickle(a.deletionReasons)
      }
      override def unpickle(implicit state: UnpickleState): ProjectContent =
        readByVersion(1) {

          // v1.0
          case 0 =>
            val reqs            = state.unpickle[Requirements]
            val reqCodes        = state.unpickle[ReqCodes]
            val reqNums         = ReqData.Numbers.empty
            val reqText         = state.unpickle[ReqData.Text]
            val reqTags         = state.unpickle[ReqData.Tags]
            val implications    = state.unpickle[Implications]
            val deletionReasons = state.unpickle[DeletionReasons]
            ProjectContent(reqs, reqCodes, reqNums, reqText, reqTags, implications, deletionReasons)

          // v1.1
          case 1 =>
            val reqs            = state.unpickle[Requirements]
            val reqCodes        = state.unpickle[ReqCodes]
            val reqNums         = state.unpickle[ReqData.Numbers]
            val reqText         = state.unpickle[ReqData.Text]
            val reqTags         = state.unpickle[ReqData.Tags]
            val implications    = state.unpickle[Implications]
            val deletionReasons = state.unpickle[DeletionReasons]
            ProjectContent(reqs, reqCodes, reqNums, reqText, reqTags, implications, deletionReasons)
        }
    }

  implicit lazy val picklerProject: Pickler[Project] =
    new Pickler[Project] {
      override def pickle(p: Project)(implicit state: PickleState): Unit = {
        writeVersion(1)
        state.pickle(p.name)
        state.pickle(p.config)
        state.pickle(p.content)
        state.pickle(p.manualIssues)
        state.pickle(p.savedViews)
        state.pickle(p.access)
        state.pickle(p.deletionReason)
        state.pickle(p.history)
        state.pickle(p.idCeilings)
      }
      override def unpickle(implicit state: UnpickleState): Project =
        readByVersion(1) {

          // v1.0
          case 0 =>
            val name           = state.unpickle[Project.Name]
            val config         = state.unpickle[ProjectConfig]
            val content        = state.unpickle[ProjectContent]
            val manualIssues   = state.unpickle[ManualIssues]
            val savedViews     = state.unpickle[savedview.SavedViews.Optional]
            val access         = state.unpickle[ProjectAccess]
            val deletionReason = Option.empty[Text.DeletionReason.OptionalText]
            val history        = state.unpickle[ProjectEvents]
            val idCeilings     = state.unpickle[IdCeilings]
            Project(name, config, content, manualIssues, savedViews, access, deletionReason, history, idCeilings)

          // v1.1
          case 1 =>
            val name           = state.unpickle[Project.Name]
            val config         = state.unpickle[ProjectConfig]
            val content        = state.unpickle[ProjectContent]
            val manualIssues   = state.unpickle[ManualIssues]
            val savedViews     = state.unpickle[savedview.SavedViews.Optional]
            val access         = state.unpickle[ProjectAccess]
            val deletionReason = state.unpickle[Option[Text.DeletionReason.OptionalText]]
            val history        = state.unpickle[ProjectEvents]
            val idCeilings     = state.unpickle[IdCeilings]
            Project(name, config, content, manualIssues, savedViews, access, deletionReason, history, idCeilings)
        }
    }

  // ===================================================================================================================
  // Filter

  implicit lazy val pickleValidFilter: Pickler[Filter.Valid] = {
    import shipreq.webapp.member.project.filter.{IntensionalReqSet, FilterAst}
    import Filter._
    import Filter.Implicits._
    import Filter.Valid.FieldCriteriaF

    implicit val picklerNonEmptyVectorUnit: Pickler[NonEmptyVector[Unit]] =
      implicitly[Pickler[Int]].xmap(NonEmptyVector force Vector.fill(_)(()))(_.length)

    implicit val picklerNonEmptySetInt: Pickler[NonEmptySet[Int]] =
      pickleNES

    implicit def picklerIRSetS[RT: Pickler]: Pickler[IntensionalReqSet.SomeOfType[RT]] =
      new Pickler[IntensionalReqSet.SomeOfType[RT]] {
        override def pickle(a: IntensionalReqSet.SomeOfType[RT])(implicit state: PickleState): Unit = {
          state.pickle(a.reqType)
          state.pickle(a.numbers)
        }
        override def unpickle(implicit state: UnpickleState): IntensionalReqSet.SomeOfType[RT] = {
          val reqType = state.unpickle[RT]
          val numbers = state.unpickle[NonEmptySet[Int]]
          IntensionalReqSet.SomeOfType(reqType, numbers)
        }
      }

    implicit def picklerIRSetW[RT: Pickler]: Pickler[IntensionalReqSet.WholeType[RT]] =
      transformPickler(IntensionalReqSet.WholeType.apply[RT])(_.reqType)

    def picklerIRSet[RT: Pickler]: Pickler[IntensionalReqSet[RT]] =
      new Pickler[IntensionalReqSet[RT]] {
        import IntensionalReqSet._
        private[this] final val KeySomeOfType = 0
        private[this] final val KeyWholeType  = 1
        override def pickle(a: IntensionalReqSet[RT])(implicit state: PickleState): Unit =
          a match {
            case b: SomeOfType[RT] => state.enc.writeByte(KeySomeOfType); state.pickle(b)
            case b: WholeType[RT]  => state.enc.writeByte(KeyWholeType ); state.pickle(b)
          }
        override def unpickle(implicit state: UnpickleState): IntensionalReqSet[RT] =
          state.dec.readByte match {
            case KeySomeOfType => state.unpickle[SomeOfType[RT]]
            case KeyWholeType  => state.unpickle[WholeType[RT]]
          }
      }

    implicit val picklerSpecialBuiltInFieldFilterOk: Pickler[SpecialBuiltInField.FilterOk] =
      new Pickler[SpecialBuiltInField.FilterOk] {
        private[this] final val KeyTitle = 't'
        override def pickle(a: SpecialBuiltInField.FilterOk)(implicit state: PickleState): Unit =
          a match {
            case SpecialBuiltInField.Title => state.enc.writeByte(KeyTitle)
          }
        override def unpickle(implicit state: UnpickleState): SpecialBuiltInField.FilterOk =
          state.dec.readByte match {
            case KeyTitle => SpecialBuiltInField.Title
          }
      }

    implicit val picklerValidHashTag: Pickler[Valid.HashTag] =
      pickleDisj

    implicit lazy val picklerValidField: Pickler[Valid.Field] =
      pickleDisj

    implicit val picklerValidIssueCatNEV: Pickler[NonEmptyVector[Valid.IssueCat]] =
      pickleNEV

    implicit val picklerValidReqSubset: Pickler[Valid.ReqSubset] =
      picklerIRSet

    implicit val picklerValidReqSet: Pickler[Valid.ReqSet] =
      pickleNEV

    implicit val picklerFilterAstAttr: Pickler[FilterAst.Attr] =
      new Pickler[FilterAst.Attr] {
        private[this] final val KeyAnyIssue = 'i'
        private[this] final val KeyAnyTag   = 't'
        override def pickle(a: FilterAst.Attr)(implicit state: PickleState): Unit =
          a match {
            case FilterAst.Attr.AnyIssue => state.enc.writeByte(KeyAnyIssue)
            case FilterAst.Attr.AnyTag   => state.enc.writeByte(KeyAnyTag  )
          }
        override def unpickle(implicit state: UnpickleState): FilterAst.Attr =
          state.dec.readByte match {
            case KeyAnyIssue => FilterAst.Attr.AnyIssue
            case KeyAnyTag   => FilterAst.Attr.AnyTag
          }
      }

    implicit val picklerFilterAstText: Pickler[FilterAst.Text] =
      new Pickler[FilterAst.Text] {
        override def pickle(a: FilterAst.Text)(implicit state: PickleState): Unit = {
          state.pickle(a.text)
          state.pickle(a.quoteChar)
        }
        override def unpickle(implicit state: UnpickleState): FilterAst.Text = {
          val text      = state.unpickle[String]
          val quoteChar = state.unpickle[Option[Char]]
          FilterAst.Text(text, quoteChar)
        }
      }

    implicit val picklerFilterAstRegex: Pickler[FilterAst.Regex] =
      transformPickler(FilterAst.Regex.apply)(_.text)

    implicit val picklerFilterAstPresence: Pickler[FilterAst.Presence[Valid.Attr]] =
      transformPickler(FilterAst.Presence.apply[Valid.Attr])(_.attr)

    implicit val picklerFilterAstHasIssue: Pickler[FilterAst.HasIssue[Valid.IssueCat]] =
      new Pickler[FilterAst.HasIssue[Valid.IssueCat]] {
        override def pickle(a: FilterAst.HasIssue[Valid.IssueCat])(implicit state: PickleState): Unit = {
          state.pickle(a.on)
          state.pickle(a.criteria)
        }
        override def unpickle(implicit state: UnpickleState): FilterAst.HasIssue[Valid.IssueCat] = {
          val on       = state.unpickle[On]
          val criteria = state.unpickle[NonEmptyVector[Valid.IssueCat]]
          FilterAst.HasIssue(on, criteria)
        }
      }

    implicit val picklerFilterAstHashRef: Pickler[FilterAst.HashRef[Valid.HashTag]] =
      transformPickler(FilterAst.HashRef.apply[Valid.HashTag])(_.value)

    implicit def picklerFilterAstImpCriteriaReqs[R: Pickler]: Pickler[FilterAst.ImpCriteria.Reqs[R]] =
      transformPickler(FilterAst.ImpCriteria.Reqs.apply[R])(_.value)

    implicit def picklerFilterAstImpCriteriaQuery[Q: Pickler]: Pickler[FilterAst.ImpCriteria.Query[Q]] =
      transformPickler(FilterAst.ImpCriteria.Query.apply[Q])(_.value)

    implicit def picklerFilterAstImpCriteria[R, Q](implicit p1: Pickler[FilterAst.ImpCriteria.Query[Q]], p2: Pickler[FilterAst.ImpCriteria.Reqs[R]]): Pickler[FilterAst.ImpCriteria[R, Q]] =
      new Pickler[FilterAst.ImpCriteria[R, Q]] {
        private[this] final val KeyQuery = 'q'
        private[this] final val KeyReqs  = 'r'
        override def pickle(a: FilterAst.ImpCriteria[R, Q])(implicit state: PickleState): Unit =
          a match {
            case b: FilterAst.ImpCriteria.Query[Q] => state.enc.writeByte(KeyQuery); state.pickle(b)
            case b: FilterAst.ImpCriteria.Reqs[R]  => state.enc.writeByte(KeyReqs ); state.pickle(b)
          }
        override def unpickle(implicit state: UnpickleState): FilterAst.ImpCriteria[R, Q] =
          state.dec.readByte match {
            case KeyQuery => state.unpickle[FilterAst.ImpCriteria.Query[Q]]
            case KeyReqs  => state.unpickle[FilterAst.ImpCriteria.Reqs[R]]
          }
      }

    implicit val picklerFilterAstImpliesAnyOf: Pickler[FilterAst.ImpliesAnyOf[Valid.ImpCriteriaF, Unit]] = {
      type F[A] = Valid.ImpCriteriaF[A]
      type T = FilterAst.ImpliesAnyOf[Valid.ImpCriteriaF, Unit]
      new Pickler[T] {
        override def pickle(a: T)(implicit state: PickleState): Unit = {
          writeVersion(1)
          state.pickle(a.criteria)
        }
        override def unpickle(implicit state: UnpickleState): T =
          readByVersion(1) {

            // v1.0
            case 0 =>
              val reqs = state.unpickle[Valid.ReqSet]
              val criteria = FilterAst.ImpCriteria.Reqs(reqs): F[Unit]
              FilterAst.ImpliesAnyOf(criteria)

            // v1.1
            case 1 =>
              val criteria = state.unpickle[F[Unit]]
              FilterAst.ImpliesAnyOf(criteria)
          }
      }
    }

    implicit val picklerFilterAstImpliedByAnyOf: Pickler[FilterAst.ImpliedByAnyOf[Valid.ImpCriteriaF, Unit]] =
      picklerFilterAstImpliesAnyOf.xmap(x => FilterAst.ImpliedByAnyOf(x.criteria))(x => FilterAst.ImpliesAnyOf(x.criteria))

    implicit val picklerFilterAstReqs: Pickler[FilterAst.Reqs[Valid.ReqSet]] =
      transformPickler(FilterAst.Reqs.apply[Valid.ReqSet])(_.reqs)

    implicit val picklerFilterAstReqType: Pickler[FilterAst.ReqType[Valid.ReqType]] =
      transformPickler(FilterAst.ReqType.apply[Valid.ReqType])(_.reqType)

    implicit val picklerFilterAstNot: Pickler[FilterAst.Not[Unit]] =
      transformPickler(FilterAst.Not.apply[Unit])(_.clause)

    implicit val picklerFilterAstAllOf: Pickler[FilterAst.AllOf[Unit]] =
      transformPickler(FilterAst.AllOf.apply[Unit])(_.clauses)

    implicit val picklerFilterAstAnyOf: Pickler[FilterAst.AnyOf[Unit]] =
      new Pickler[FilterAst.AnyOf[Unit]] {
        override def pickle(a: FilterAst.AnyOf[Unit])(implicit state: PickleState): Unit = {
          state.pickle(a.tail)
        }
        override def unpickle(implicit state: UnpickleState): FilterAst.AnyOf[Unit] = {
          val tail = state.unpickle[NonEmptyVector[Unit]]
          FilterAst.AnyOf((), tail)
        }
      }

    implicit val picklerFilterAstFieldAttr: Pickler[FilterAst.FieldAttr] =
      new Pickler[FilterAst.FieldAttr] {
        private[this] final val KeyBlank         = 0
        private[this] final val KeyDefaultInUse  = 1
        private[this] final val KeyNotApplicable = 2
        private[this] final val KeyNotBlank      = 3
        override def pickle(a: FilterAst.FieldAttr)(implicit state: PickleState): Unit =
          a match {
            case FilterAst.FieldAttr.Blank         => state.enc.writeByte(KeyBlank        )
            case FilterAst.FieldAttr.DefaultInUse  => state.enc.writeByte(KeyDefaultInUse )
            case FilterAst.FieldAttr.NotApplicable => state.enc.writeByte(KeyNotApplicable)
            case FilterAst.FieldAttr.NotBlank      => state.enc.writeByte(KeyNotBlank)
          }
        override def unpickle(implicit state: UnpickleState): FilterAst.FieldAttr =
          state.dec.readByte match {
            case KeyBlank         => FilterAst.FieldAttr.Blank
            case KeyDefaultInUse  => FilterAst.FieldAttr.DefaultInUse
            case KeyNotApplicable => FilterAst.FieldAttr.NotApplicable
            case KeyNotBlank      => FilterAst.FieldAttr.NotBlank
          }
      }

    implicit val picklerFieldCriteriaAttr: Pickler[FilterAst.FieldCriteria.Attr[FilterAst.FieldAttr]] =
      transformPickler(FilterAst.FieldCriteria.Attr.apply[FilterAst.FieldAttr])(_.value)

    implicit val picklerFieldCriteriaReqTypePosSet: Pickler[FilterAst.FieldCriteria.ReqTypePosSet] =
      transformPickler(FilterAst.FieldCriteria.ReqTypePosSet.apply)(_.value)

    implicit val picklerFieldCriteriaQuery: Pickler[FilterAst.FieldCriteria.Query[Unit]] =
      transformPickler(FilterAst.FieldCriteria.Query.apply[Unit])(_.value)

    implicit val picklerFieldCriteria: Pickler[FieldCriteriaF[Unit]] =
      new Pickler[FieldCriteriaF[Unit]] {
        private[this] final val KeyAttr          = 'a'
        private[this] final val KeyReqTypePosSet = 'p'
        private[this] final val KeyQuery         = 'q'
        override def pickle(a: FieldCriteriaF[Unit])(implicit state: PickleState): Unit =
          a match {
            case b: FilterAst.FieldCriteria.Attr[FilterAst.FieldAttr] => state.enc.writeByte(KeyAttr         ); state.pickle(b)
            case b: FilterAst.FieldCriteria.ReqTypePosSet             => state.enc.writeByte(KeyReqTypePosSet); state.pickle(b)
            case b: FilterAst.FieldCriteria.Query[Unit]               => state.enc.writeByte(KeyQuery        ); state.pickle(b)
          }
        override def unpickle(implicit state: UnpickleState): FieldCriteriaF[Unit] =
          state.dec.readByte match {
            case KeyAttr          => state.unpickle[FilterAst.FieldCriteria.Attr[FilterAst.FieldAttr]]
            case KeyReqTypePosSet => state.unpickle[FilterAst.FieldCriteria.ReqTypePosSet]
            case KeyQuery         => state.unpickle[FilterAst.FieldCriteria.Query[Unit]]
          }
      }

    implicit val picklerFilterAstFieldProp: Pickler[Valid.FieldPropF[Unit]] =
      new Pickler[Valid.FieldPropF[Unit]] {
        override def pickle(a: Valid.FieldPropF[Unit])(implicit state: PickleState): Unit = {
          state.pickle(a.field)
          state.pickle(a.criteria)
        }
        override def unpickle(implicit state: UnpickleState) = {
          val field = state.unpickle[Valid.Field]
          val attr  = state.unpickle[FieldCriteriaF[Unit]]
          FilterAst.FieldProp(field, attr)
        }
      }

    implicit def picklerFilterAstScopeDerivation[A: Pickler]: Pickler[FilterAst.Scope.Derivation[A]] =
      new Pickler[FilterAst.Scope.Derivation[A]] {
        override def pickle(a: FilterAst.Scope.Derivation[A])(implicit state: PickleState): Unit = {
          state.pickle(a.field)
        }
        override def unpickle(implicit state: UnpickleState): FilterAst.Scope.Derivation[A] = {
          val field = state.unpickle[Option[A]]
          FilterAst.Scope.Derivation(field)
        }
      }

    implicit def picklerFilterAstScope[A](implicit p1: Pickler[FilterAst.Scope.Derivation[A]]): Pickler[FilterAst.Scope[A]] =
      new Pickler[FilterAst.Scope[A]] {
        private[this] final val KeyDerivation = 0
        override def pickle(a: FilterAst.Scope[A])(implicit state: PickleState): Unit =
          a match {
            case b: FilterAst.Scope.Derivation[A] => state.enc.writeByte(KeyDerivation); state.pickle(b)
          }
        override def unpickle(implicit state: UnpickleState): FilterAst.Scope[A] =
          state.dec.readByte match {
            case KeyDerivation => state.unpickle[FilterAst.Scope.Derivation[A]]
          }
      }

    implicit val picklerValidScope: Pickler[Valid.Scope] =
      pickleNES

    implicit val picklerFilterAstScoped1: Pickler[FilterAst.Scoped1[Valid.Scope, Unit]] =
      new Pickler[FilterAst.Scoped1[Valid.Scope, Unit]] {
        override def pickle(a: FilterAst.Scoped1[Valid.Scope, Unit])(implicit state: PickleState): Unit = {
          state.pickle(a.main)
          state.pickle(a.scope)
        }
        override def unpickle(implicit state: UnpickleState): FilterAst.Scoped1[Valid.Scope, Unit] = {
          val main  = state.unpickle[Boolean]
          val scope = state.unpickle[Valid.Scope]
          FilterAst.Scoped1(main, scope, ())
        }
      }

    implicit val picklerFilterAstScoped2: Pickler[FilterAst.Scoped2[Valid.Scope, Unit]] =
      new Pickler[FilterAst.Scoped2[Valid.Scope, Unit]] {
        override def pickle(a: FilterAst.Scoped2[Valid.Scope, Unit])(implicit state: PickleState): Unit = {
          state.pickle(a.scope)
        }
        override def unpickle(implicit state: UnpickleState): FilterAst.Scoped2[Valid.Scope, Unit] = {
          val scope = state.unpickle[Valid.Scope]
          FilterAst.Scoped2(scope, (), ())
        }
      }

    implicit val picklerFilterAstOrderOp: Pickler[FilterAst.OrderOp] =
      new Pickler[FilterAst.OrderOp] {
        private[this] final val Key_<  = 0
        private[this] final val Key_>  = 1
        private[this] final val Key_<= = 2
        private[this] final val Key_>= = 3
        override def pickle(a: FilterAst.OrderOp)(implicit state: PickleState): Unit =
          a match {
            case FilterAst.OrderOp.<  => state.enc.writeByte(Key_< )
            case FilterAst.OrderOp.>  => state.enc.writeByte(Key_> )
            case FilterAst.OrderOp.<= => state.enc.writeByte(Key_<=)
            case FilterAst.OrderOp.>= => state.enc.writeByte(Key_>=)
          }
        override def unpickle(implicit state: UnpickleState): FilterAst.OrderOp =
          state.dec.readByte match {
            case Key_<  => FilterAst.OrderOp.<
            case Key_>  => FilterAst.OrderOp.>
            case Key_<= => FilterAst.OrderOp.<=
            case Key_>= => FilterAst.OrderOp.>=
          }
      }

    implicit val picklerFilterAstRelativeTags: Pickler[FilterAst.RelativeTags[Valid.ApTag]] =
      new Pickler[FilterAst.RelativeTags[Valid.ApTag]] {
        override def pickle(a: FilterAst.RelativeTags[Valid.ApTag])(implicit state: PickleState): Unit = {
          state.pickle(a.op)
          state.pickle(a.subject)
        }
        override def unpickle(implicit state: UnpickleState): FilterAst.RelativeTags[Valid.ApTag] = {
          val op      = state.unpickle[FilterAst.OrderOp]
          val subject = state.unpickle[Valid.ApTag]
          FilterAst.RelativeTags(op, subject)
        }
      }

    implicit val picklerValidF: Pickler[ValidF[Unit]] =
      new Pickler[ValidF[Unit]] {
        private[this] final val KeyAllOf          = 0
        private[this] final val KeyAnyOf          = 1
        private[this] final val KeyHasIssue       = 2
        private[this] final val KeyHashRef        = 3
        private[this] final val KeyImpliedByAnyOf = 4
        private[this] final val KeyImpliesAnyOf   = 5
        private[this] final val KeyNot            = 6
        private[this] final val KeyPresence       = 7
        private[this] final val KeyRegex          = 8
        private[this] final val KeyReqType        = 9
        private[this] final val KeyReqs           = 10
        private[this] final val KeyText           = 11
        private[this] final val KeyFieldProp      = 12
        private[this] final val KeyScoped1        = 13
        private[this] final val KeyScoped2        = 14
        private[this] final val KeyRelativeTags   = 15
        override def pickle(a: ValidF[Unit])(implicit state: PickleState): Unit =
          a match {
            case b: FilterAst.AllOf         [Unit]                     => state.enc.writeByte(KeyAllOf         ); state.pickle(b)
            case b: FilterAst.AnyOf         [Unit]                     => state.enc.writeByte(KeyAnyOf         ); state.pickle(b)
            case b: FilterAst.HasIssue      [Valid.IssueCat]           => state.enc.writeByte(KeyHasIssue      ); state.pickle(b)
            case b: FilterAst.HashRef       [Valid.HashTag]            => state.enc.writeByte(KeyHashRef       ); state.pickle(b)
            case b: FilterAst.ImpliedByAnyOf[Valid.ImpCriteriaF, Unit] => state.enc.writeByte(KeyImpliedByAnyOf); state.pickle(b)
            case b: FilterAst.ImpliesAnyOf  [Valid.ImpCriteriaF, Unit] => state.enc.writeByte(KeyImpliesAnyOf  ); state.pickle(b)
            case b: FilterAst.Not           [Unit]                     => state.enc.writeByte(KeyNot           ); state.pickle(b)
            case b: FilterAst.Presence      [Valid.Attr]               => state.enc.writeByte(KeyPresence      ); state.pickle(b)
            case b: FilterAst.Regex                                    => state.enc.writeByte(KeyRegex         ); state.pickle(b)
            case b: FilterAst.ReqType       [Valid.ReqType]            => state.enc.writeByte(KeyReqType       ); state.pickle(b)
            case b: FilterAst.Reqs          [Valid.ReqSet]             => state.enc.writeByte(KeyReqs          ); state.pickle(b)
            case b: FilterAst.Text                                     => state.enc.writeByte(KeyText          ); state.pickle(b)
            case b: Valid.FieldPropF        [Unit]                     => state.enc.writeByte(KeyFieldProp     ); state.pickle(b)
            case b: FilterAst.Scoped1       [Valid.Scope, Unit       ] => state.enc.writeByte(KeyScoped1       ); state.pickle(b)
            case b: FilterAst.Scoped2       [Valid.Scope, Unit       ] => state.enc.writeByte(KeyScoped2       ); state.pickle(b)
            case b: FilterAst.RelativeTags  [Valid.ApTag             ] => state.enc.writeByte(KeyRelativeTags  ); state.pickle(b)
          }
        override def unpickle(implicit state: UnpickleState): ValidF[Unit] =
          state.dec.readByte match {
            case KeyAllOf          => state.unpickle[FilterAst.AllOf         [Unit                    ]]
            case KeyAnyOf          => state.unpickle[FilterAst.AnyOf         [Unit                    ]]
            case KeyHasIssue       => state.unpickle[FilterAst.HasIssue      [Valid.IssueCat          ]]
            case KeyHashRef        => state.unpickle[FilterAst.HashRef       [Valid.HashTag           ]]
            case KeyImpliedByAnyOf => state.unpickle[FilterAst.ImpliedByAnyOf[Valid.ImpCriteriaF, Unit]]
            case KeyImpliesAnyOf   => state.unpickle[FilterAst.ImpliesAnyOf  [Valid.ImpCriteriaF, Unit]]
            case KeyNot            => state.unpickle[FilterAst.Not           [Unit                    ]]
            case KeyPresence       => state.unpickle[FilterAst.Presence      [Valid.Attr              ]]
            case KeyRegex          => state.unpickle[FilterAst.Regex                                   ]
            case KeyReqType        => state.unpickle[FilterAst.ReqType       [Valid.ReqType           ]]
            case KeyReqs           => state.unpickle[FilterAst.Reqs          [Valid.ReqSet            ]]
            case KeyText           => state.unpickle[FilterAst.Text                                    ]
            case KeyFieldProp      => state.unpickle[Valid.FieldPropF        [Unit                    ]]
            case KeyScoped1        => state.unpickle[FilterAst.Scoped1       [Valid.Scope, Unit       ]]
            case KeyScoped2        => state.unpickle[FilterAst.Scoped2       [Valid.Scope, Unit       ]]
            case KeyRelativeTags   => state.unpickle[FilterAst.RelativeTags  [Valid.ApTag             ]]
          }
      }

    pickleFix[ValidF]
  }

  // ===================================================================================================================
  // Events

  private[binary] implicit val picklerEventFieldCustomDelete: Pickler[Event.FieldCustomDelete] =
    transformPickler(Event.FieldCustomDelete.apply)(_.id)

  private[binary] implicit val picklerEventFieldCustomRestore: Pickler[Event.FieldCustomRestore] =
    transformPickler(Event.FieldCustomRestore.apply)(_.id)

  private[binary] implicit val picklerEventFieldReposition: Pickler[Event.FieldReposition] =
    new Pickler[Event.FieldReposition] {
      override def pickle(a: Event.FieldReposition)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.newPos)
      }
      override def unpickle(implicit state: UnpickleState): Event.FieldReposition = {
        val id     = state.unpickle[FieldId]
        val newPos = state.unpickle[Option[FieldId]]
        Event.FieldReposition(id, newPos)
      }
    }

  private[binary] implicit lazy val picklerEventProjectDelete: Pickler[Event.ProjectDelete] =
    transformPickler(Event.ProjectDelete.apply)(_.reason)

  private[binary] implicit lazy val picklerEventProjectRestore: Pickler[Event.ProjectRestore.type] =
    unitPickler.xmap(_ => Event.ProjectRestore)(_ => ())

  implicit lazy val pickleSavedViewGDv1: Pickler[RetiredGenericData.SavedViewGDv1.NonEmptyValues] = {
    import RetiredGenericData.SavedViewGDv1._

    implicit val picklerValueForColumns    = transformPickler(ValueForColumns   .apply)(_.value)
    implicit val picklerValueForFilter     = transformPickler(ValueForFilter    .apply)(_.value)
    implicit val picklerValueForFilterDead = transformPickler(ValueForFilterDead.apply)(_.value)
    implicit val picklerValueForName       = transformPickler(ValueForName      .apply)(_.value)
    implicit val picklerValueForOrder      = transformPickler(ValueForOrder     .apply)(_.value)

    implicit val picklerValue: Pickler[Value] =
      new Pickler[Value] {
        private[this] final val KeyColumns    = 'C'
        private[this] final val KeyFilter     = 'F'
        private[this] final val KeyFilterDead = 'D'
        private[this] final val KeyName       = 'N'
        private[this] final val KeyOrder      = 'O'
        override def pickle(a: Value)(implicit state: PickleState): Unit =
          a match {
            case b: ValueForColumns    => state.enc.writeByte(KeyColumns   ); state.pickle(b)
            case b: ValueForFilter     => state.enc.writeByte(KeyFilter    ); state.pickle(b)
            case b: ValueForFilterDead => state.enc.writeByte(KeyFilterDead); state.pickle(b)
            case b: ValueForName       => state.enc.writeByte(KeyName      ); state.pickle(b)
            case b: ValueForOrder      => state.enc.writeByte(KeyOrder     ); state.pickle(b)
          }
        override def unpickle(implicit state: UnpickleState): Value =
          state.dec.readByte match {
            case KeyColumns    => state.unpickle[ValueForColumns]
            case KeyFilter     => state.unpickle[ValueForFilter]
            case KeyFilterDead => state.unpickle[ValueForFilterDead]
            case KeyName       => state.unpickle[ValueForName]
            case KeyOrder      => state.unpickle[ValueForOrder]
          }
      }

    val values: Pickler[Values] = pickleIMap(emptyValues)
    pickleNonEmptyMono[Values](values, implicitly)
  }

  implicit lazy val pickleSavedViewGD: Pickler[SavedViewGD.NonEmptyValues] = {
    import SavedViewGD._

    implicit val picklerValueForColumns        = transformPickler(ValueForColumns       .apply)(_.value)
    implicit val picklerValueForFilter         = transformPickler(ValueForFilter        .apply)(_.value)
    implicit val picklerValueForFilterDead     = transformPickler(ValueForFilterDead    .apply)(_.value)
    implicit val picklerValueForName           = transformPickler(ValueForName          .apply)(_.value)
    implicit val picklerValueForOrder          = transformPickler(ValueForOrder         .apply)(_.value)
    implicit val picklerValueForImpGraphConfig = transformPickler(ValueForImpGraphConfig.apply)(_.value)

    implicit val picklerValue: Pickler[Value] =
      new Pickler[Value] {
        private[this] final val KeyColumns        = 'C'
        private[this] final val KeyFilter         = 'F'
        private[this] final val KeyFilterDead     = 'D'
        private[this] final val KeyName           = 'N'
        private[this] final val KeyOrder          = 'O'
        private[this] final val KeyImpGraphConfig = 'I'
        override def pickle(a: Value)(implicit state: PickleState): Unit =
          a match {
            case b: ValueForColumns        => state.enc.writeByte(KeyColumns       ); state.pickle(b)
            case b: ValueForFilter         => state.enc.writeByte(KeyFilter        ); state.pickle(b)
            case b: ValueForFilterDead     => state.enc.writeByte(KeyFilterDead    ); state.pickle(b)
            case b: ValueForName           => state.enc.writeByte(KeyName          ); state.pickle(b)
            case b: ValueForOrder          => state.enc.writeByte(KeyOrder         ); state.pickle(b)
            case b: ValueForImpGraphConfig => state.enc.writeByte(KeyImpGraphConfig); state.pickle(b)
          }
        override def unpickle(implicit state: UnpickleState): Value =
          state.dec.readByte match {
            case KeyColumns        => state.unpickle[ValueForColumns]
            case KeyFilter         => state.unpickle[ValueForFilter]
            case KeyFilterDead     => state.unpickle[ValueForFilterDead]
            case KeyName           => state.unpickle[ValueForName]
            case KeyOrder          => state.unpickle[ValueForOrder]
            case KeyImpGraphConfig => state.unpickle[ValueForImpGraphConfig]
          }
      }

    val values: Pickler[Values] = pickleIMap(emptyValues)
    pickleNonEmptyMono[Values](values, implicitly)
  }

  private[binary] implicit lazy val picklerEventSavedViewCreateV1: Pickler[Event.SavedViewCreateV1] =
    new Pickler[Event.SavedViewCreateV1] {
      override def pickle(a: Event.SavedViewCreateV1)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.name)
        state.pickle(a.columns)
        state.pickle(a.order)
        state.pickle(a.filterDead)
        state.pickle(a.filter)
      }
      override def unpickle(implicit state: UnpickleState): Event.SavedViewCreateV1 = {
        val id         = state.unpickle[SavedView.Id]
        val name       = state.unpickle[SavedView.Name]
        val columns    = state.unpickle[NonEmptyVector[savedview.Column]]
        val order      = state.unpickle[savedview.SortCriteria]
        val filterDead = state.unpickle[FilterDead]
        val filter     = state.unpickle[Option[Filter.Valid]]
        Event.SavedViewCreateV1(id, name, columns, order, filterDead, filter)
      }
    }

  private[binary] implicit lazy val picklerEventSavedViewUpdateV1: Pickler[Event.SavedViewUpdateV1] =
    new Pickler[Event.SavedViewUpdateV1] {
      override def pickle(a: Event.SavedViewUpdateV1)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.vs)
      }
      override def unpickle(implicit state: UnpickleState): Event.SavedViewUpdateV1 = {
        val id = state.unpickle[SavedView.Id]
        val vs = state.unpickle[RetiredGenericData.SavedViewGDv1.NonEmptyValues]
        Event.SavedViewUpdateV1(id, vs)
      }
    }

  private[binary] implicit lazy val picklerEventSavedViewCreate: Pickler[Event.SavedViewCreate] =
    new Pickler[Event.SavedViewCreate] {
      override def pickle(a: Event.SavedViewCreate)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.name)
        state.pickle(a.columns)
        state.pickle(a.order)
        state.pickle(a.filterDead)
        state.pickle(a.filter)
        state.pickle(a.impGraphConfig)
      }
      override def unpickle(implicit state: UnpickleState): Event.SavedViewCreate = {
        val id             = state.unpickle[SavedView.Id]
        val name           = state.unpickle[SavedView.Name]
        val columns        = state.unpickle[NonEmptyVector[savedview.Column]]
        val order          = state.unpickle[savedview.SortCriteria]
        val filterDead     = state.unpickle[FilterDead]
        val filter         = state.unpickle[Option[Filter.Valid]]
        val impGraphConfig = state.unpickle[Option[ImpGraphConfig]]
        Event.SavedViewCreate(id, name, columns, order, filterDead, filter, impGraphConfig)
      }
    }

  private[binary] implicit lazy val picklerEventSavedViewUpdate: Pickler[Event.SavedViewUpdate] =
    new Pickler[Event.SavedViewUpdate] {
      override def pickle(a: Event.SavedViewUpdate)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.vs)
      }
      override def unpickle(implicit state: UnpickleState): Event.SavedViewUpdate = {
        val id = state.unpickle[SavedView.Id]
        val vs = state.unpickle[SavedViewGD.NonEmptyValues]
        Event.SavedViewUpdate(id, vs)
      }
    }

  implicit lazy val pickleCustomNumberFieldGD: Pickler[CustomNumberFieldGD.NonEmptyValues] = {
    import CustomNumberFieldGD._

    implicit val picklerValueForName              = transformPickler(ValueForName             .apply)(_.value)
    implicit val picklerValueForDesc              = transformPickler(ValueForDesc             .apply)(_.value)
    implicit val picklerValueForRange             = transformPickler(ValueForRange            .apply)(_.value)
    implicit val picklerValueForDecimalPlaces     = transformPickler(ValueForDecimalPlaces    .apply)(_.value)
    implicit val picklerValueForFieldReqTypeRules = transformPickler(ValueForFieldReqTypeRules.apply)(_.value)

    implicit val picklerValue: Pickler[Value] =
      new Pickler[Value] {
        private[this] final val KeyName          = 'n'
        private[this] final val KeyDesc          = 'd'
        private[this] final val KeyRange         = 'r'
        private[this] final val KeyDecimalPlaces = 'p'
        private[this] final val KeyReqTypes      = 'R'
        override def pickle(a: Value)(implicit state: PickleState): Unit =
          a match {
            case b: ValueForName              => state.enc.writeByte(KeyName         ); state.pickle(b)
            case b: ValueForDesc              => state.enc.writeByte(KeyDesc         ); state.pickle(b)
            case b: ValueForRange             => state.enc.writeByte(KeyRange        ); state.pickle(b)
            case b: ValueForDecimalPlaces     => state.enc.writeByte(KeyDecimalPlaces); state.pickle(b)
            case b: ValueForFieldReqTypeRules => state.enc.writeByte(KeyReqTypes     ); state.pickle(b)
          }
        override def unpickle(implicit state: UnpickleState): Value =
          state.dec.readByte match {
            case KeyName          => state.unpickle[ValueForName]
            case KeyDesc          => state.unpickle[ValueForDesc]
            case KeyRange         => state.unpickle[ValueForRange]
            case KeyDecimalPlaces => state.unpickle[ValueForDecimalPlaces]
            case KeyReqTypes      => state.unpickle[ValueForFieldReqTypeRules]
          }
      }

    val values: Pickler[Values] = pickleIMap(emptyValues)
    pickleNonEmptyMono[Values](values, implicitly)
  }

  private[binary] implicit lazy val picklerEventFieldCustomNumberCreate: Pickler[Event.FieldCustomNumberCreate] =
    new Pickler[Event.FieldCustomNumberCreate] {
      override def pickle(a: Event.FieldCustomNumberCreate)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.vs)
      }
      override def unpickle(implicit state: UnpickleState): Event.FieldCustomNumberCreate = {
        val id = state.unpickle[CustomField.Number.Id]
        val vs = state.unpickle[CustomNumberFieldGD.NonEmptyValues]
        Event.FieldCustomNumberCreate(id, vs)
      }
    }

  private[binary] implicit lazy val picklerEventFieldCustomNumberUpdate: Pickler[Event.FieldCustomNumberUpdate] =
    new Pickler[Event.FieldCustomNumberUpdate] {
      override def pickle(a: Event.FieldCustomNumberUpdate)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.vs)
      }
      override def unpickle(implicit state: UnpickleState): Event.FieldCustomNumberUpdate = {
        val id = state.unpickle[CustomField.Number.Id]
        val vs = state.unpickle[CustomNumberFieldGD.NonEmptyValues]
        Event.FieldCustomNumberUpdate(id, vs)
      }
    }

  private[binary] implicit lazy val picklerEventReqFieldCustomNumberSet: Pickler[Event.ReqFieldCustomNumberSet] =
    new Pickler[Event.ReqFieldCustomNumberSet] {
      override def pickle(a: Event.ReqFieldCustomNumberSet)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.fid)
        state.pickle(a.value)
      }
      override def unpickle(implicit state: UnpickleState): Event.ReqFieldCustomNumberSet = {
        val id    = state.unpickle[ReqId]
        val fid   = state.unpickle[CustomField.Number.Id]
        val value = state.unpickle[Option[Double]]
        Event.ReqFieldCustomNumberSet(id, fid, value)
      }
    }

  implicit val picklerEventNonEmptyCustomNumberMap: Pickler[Event.NonEmptyCustomNumberMap] =
    pickleNonEmptyMono

  implicit val pickleGenericReqGD: Pickler[GenericReqGD.Values] = {
    import GenericReqGD._

    implicit val picklerValueForCodes      = transformPickler(ValueForCodes     .apply)(_.value)
    implicit val picklerValueForCustomNums = transformPickler(ValueForCustomNums.apply)(_.value)
    implicit val picklerValueForCustomText = transformPickler(ValueForCustomText.apply)(_.value)
    implicit val picklerValueForImpSrcs    = transformPickler(ValueForImpSrcs   .apply)(_.value)
    implicit val picklerValueForImpTgts    = transformPickler(ValueForImpTgts   .apply)(_.value)
    implicit val picklerValueForTags       = transformPickler(ValueForTags      .apply)(_.value)
    implicit val picklerValueForTitle      = transformPickler(ValueForTitle     .apply)(_.value)

    implicit val picklerValue: Pickler[Value] =
      new Pickler[Value] {
        private[this] final val KeyCodes      = 'C'
        private[this] final val KeyCustomNums = 'N'
        private[this] final val KeyCustomText = 'X'
        private[this] final val KeyImpSrcs    = '>'
        private[this] final val KeyImpTgts    = '<'
        private[this] final val KeyTags       = '#'
        private[this] final val KeyTitle      = 'T'
        override def pickle(a: Value)(implicit state: PickleState): Unit =
          a match {
            case b: ValueForCodes      => state.enc.writeByte(KeyCodes     ); state.pickle(b)
            case b: ValueForCustomNums => state.enc.writeByte(KeyCustomNums); state.pickle(b)
            case b: ValueForCustomText => state.enc.writeByte(KeyCustomText); state.pickle(b)
            case b: ValueForImpSrcs    => state.enc.writeByte(KeyImpSrcs   ); state.pickle(b)
            case b: ValueForImpTgts    => state.enc.writeByte(KeyImpTgts   ); state.pickle(b)
            case b: ValueForTags       => state.enc.writeByte(KeyTags      ); state.pickle(b)
            case b: ValueForTitle      => state.enc.writeByte(KeyTitle     ); state.pickle(b)
          }
        override def unpickle(implicit state: UnpickleState): Value =
          state.dec.readByte match {
            case KeyCodes      => state.unpickle[ValueForCodes]
            case KeyCustomNums => state.unpickle[ValueForCustomNums]
            case KeyCustomText => state.unpickle[ValueForCustomText]
            case KeyImpSrcs    => state.unpickle[ValueForImpSrcs]
            case KeyImpTgts    => state.unpickle[ValueForImpTgts]
            case KeyTags       => state.unpickle[ValueForTags]
            case KeyTitle      => state.unpickle[ValueForTitle]
          }
      }

    pickleIMap(emptyValues)
  }

  implicit val pickleUseCaseGD: Pickler[UseCaseGD.Values] = {
    import UseCaseGD._

    implicit val picklerValueForCodes      = transformPickler(ValueForCodes     .apply)(_.value)
    implicit val picklerValueForCustomNums = transformPickler(ValueForCustomNums.apply)(_.value)
    implicit val picklerValueForCustomText = transformPickler(ValueForCustomText.apply)(_.value)
    implicit val picklerValueForImpSrcs    = transformPickler(ValueForImpSrcs   .apply)(_.value)
    implicit val picklerValueForImpTgts    = transformPickler(ValueForImpTgts   .apply)(_.value)
    implicit val picklerValueForTags       = transformPickler(ValueForTags      .apply)(_.value)
    implicit val picklerValueForTitle      = transformPickler(ValueForTitle     .apply)(_.value)

    implicit val picklerValue: Pickler[Value] =
      new Pickler[Value] {
        private[this] final val KeyCodes      = 'C'
        private[this] final val KeyCustomNums = 'N'
        private[this] final val KeyCustomText = 'X'
        private[this] final val KeyImpSrcs    = '>'
        private[this] final val KeyImpTgts    = '<'
        private[this] final val KeyTags       = '#'
        private[this] final val KeyTitle      = 'T'
        override def pickle(a: Value)(implicit state: PickleState): Unit =
          a match {
            case b: ValueForCodes      => state.enc.writeByte(KeyCodes     ); state.pickle(b)
            case b: ValueForCustomNums => state.enc.writeByte(KeyCustomNums); state.pickle(b)
            case b: ValueForCustomText => state.enc.writeByte(KeyCustomText); state.pickle(b)
            case b: ValueForImpSrcs    => state.enc.writeByte(KeyImpSrcs   ); state.pickle(b)
            case b: ValueForImpTgts    => state.enc.writeByte(KeyImpTgts   ); state.pickle(b)
            case b: ValueForTags       => state.enc.writeByte(KeyTags      ); state.pickle(b)
            case b: ValueForTitle      => state.enc.writeByte(KeyTitle     ); state.pickle(b)
          }
        override def unpickle(implicit state: UnpickleState): Value =
          state.dec.readByte match {
            case KeyCodes      => state.unpickle[ValueForCodes]
            case KeyCustomNums => state.unpickle[ValueForCustomNums]
            case KeyCustomText => state.unpickle[ValueForCustomText]
            case KeyImpSrcs    => state.unpickle[ValueForImpSrcs]
            case KeyImpTgts    => state.unpickle[ValueForImpTgts]
            case KeyTags       => state.unpickle[ValueForTags]
            case KeyTitle      => state.unpickle[ValueForTitle]
          }
      }

    pickleIMap(emptyValues)
  }

  private[binary] implicit val picklerEventGenericReqCreate: Pickler[Event.GenericReqCreate] =
    new Pickler[Event.GenericReqCreate] {
      override def pickle(a: Event.GenericReqCreate)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.rt)
        state.pickle(a.vs)
      }
      override def unpickle(implicit state: UnpickleState): Event.GenericReqCreate = {
        val id = state.unpickle[GenericReqId]
        val rt = state.unpickle[CustomReqTypeId]
        val vs = state.unpickle[GenericReqGD.Values]
        Event.GenericReqCreate(id, rt, vs)
      }
    }

  private[binary] implicit val picklerEventUseCaseCreate: Pickler[Event.UseCaseCreate] =
    new Pickler[Event.UseCaseCreate] {
      override def pickle(a: Event.UseCaseCreate)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.stepId)
        state.pickle(a.vs)
      }
      override def unpickle(implicit state: UnpickleState): Event.UseCaseCreate = {
        val id     = state.unpickle[UseCaseId]
        val stepId = state.unpickle[UseCaseStepId]
        val vs     = state.unpickle[UseCaseGD.Values]
        Event.UseCaseCreate(id, stepId, vs)
      }
    }

  implicit lazy val picklerEvent: Pickler[Event] =
    new Pickler[Event] {
      import Event._
      private[this] final val KeyApplicableTagCreateV1   =  0
      private[this] final val KeyApplicableTagUpdateV1   =  1
      private[this] final val KeyCodeGroupCreate         =  2
      private[this] final val KeyCodeGroupUpdate         =  3
      private[this] final val KeyCodeGroupsDelete        =  4
      private[this] final val KeyContentRestore          =  5
      private[this] final val KeyCustomIssueTypeCreate   =  6
      private[this] final val KeyCustomIssueTypeDelete   =  7
      private[this] final val KeyCustomIssueTypeRestore  =  8
      private[this] final val KeyCustomIssueTypeUpdate   =  9
      private[this] final val KeyCustomReqTypeCreateV1   = 10
      private[this] final val KeyCustomReqTypeDelete     = 11
      private[this] final val KeyCustomReqTypeRestore    = 12
      private[this] final val KeyCustomReqTypeUpdateV1   = 13
      private[this] final val KeyFieldCustomDelete       = 14
      private[this] final val KeyFieldCustomImpCreateV1  = 15
      private[this] final val KeyFieldCustomImpUpdateV1  = 16
      private[this] final val KeyFieldCustomRestore      = 17
      private[this] final val KeyFieldCustomTagCreateV1  = 18
      private[this] final val KeyFieldCustomTagUpdateV1  = 19
      private[this] final val KeyFieldCustomTextCreateV1 = 20
      private[this] final val KeyFieldCustomTextUpdateV1 = 21
      private[this] final val KeyFieldReposition         = 22
      private[this] final val KeyFieldStaticAdd          = 23
      private[this] final val KeyFieldStaticRemove       = 24
      private[this] final val KeyGenericReqCreate        = 25
      private[this] final val KeyGenericReqTitleSet      = 26
      private[this] final val KeyGenericReqTypeSet       = 27
      private[this] final val KeyManualIssueCreate       = 28
      private[this] final val KeyManualIssueDelete       = 29
      private[this] final val KeyManualIssueUpdate       = 30
      private[this] final val KeyProjectNameSet          = 31
      private[this] final val KeyProjectTemplateApply    = 32
      private[this] final val KeyReqCodesPatch           = 33
      private[this] final val KeyReqFieldCustomTextSet   = 34
      private[this] final val KeyReqImplicationsPatch    = 35
      private[this] final val KeyReqTagsPatch            = 36
      private[this] final val KeyReqsDelete              = 37
      private[this] final val KeySavedViewCreateV1       = 38
      private[this] final val KeySavedViewDefaultSet     = 39
      private[this] final val KeySavedViewDelete         = 40
      private[this] final val KeySavedViewUpdateV1       = 41
      private[this] final val KeyTagDelete               = 42
      private[this] final val KeyTagGroupCreate          = 43
      private[this] final val KeyTagGroupUpdate          = 44
      private[this] final val KeyTagRestore              = 45
      private[this] final val KeyUseCaseCreate           = 46
      private[this] final val KeyUseCaseStepCreate       = 47
      private[this] final val KeyUseCaseStepDelete       = 48
      private[this] final val KeyUseCaseStepRestore      = 49
      private[this] final val KeyUseCaseStepShiftLeft    = 50
      private[this] final val KeyUseCaseStepShiftRight   = 51
      private[this] final val KeyUseCaseStepUpdate       = 52
      private[this] final val KeyUseCaseTitleSet         = 53
      private[this] final val KeyApplicableTagCreateV2   = 54
      private[this] final val KeyApplicableTagUpdateV2   = 55
      private[this] final val KeyCustomReqTypeDeleteHard = 56
      private[this] final val KeyCustomReqTypeDeleteSoft = 57
      private[this] final val KeyFieldCustomImpCreateV2  = 58
      private[this] final val KeyFieldCustomImpUpdateV2  = 59
      private[this] final val KeyFieldCustomTagCreateV2  = 60
      private[this] final val KeyFieldCustomTagUpdateV2  = 61
      private[this] final val KeyFieldCustomTextCreateV2 = 62
      private[this] final val KeyFieldCustomTextUpdateV2 = 63
      private[this] final val KeyCustomReqTypeCreateV2   = 64
      private[this] final val KeyCustomReqTypeUpdateV2   = 65
      private[this] final val KeySavedViewCreateV2       = 66
      private[this] final val KeySavedViewUpdateV2       = 67
      private[this] final val KeyAccessUpdate            = 68
      private[this] final val KeyProjectDelete           = 69
      private[this] final val KeyProjectRestore          = 70
      private[this] final val KeyFieldCustomNumberCreate = 71
      private[this] final val KeyFieldCustomNumberUpdate = 72
      private[this] final val KeyReqFieldCustomNumberSet = 73

      override def pickle(a: Event)(implicit state: PickleState): Unit =
        a match {
          case b: AccessUpdate            => state.enc.writeByte(KeyAccessUpdate           ); state.pickle(b)
          case b: ApplicableTagCreate     => state.enc.writeByte(KeyApplicableTagCreateV2  ); state.pickle(b)
          case b: ApplicableTagCreateV1   => state.enc.writeByte(KeyApplicableTagCreateV1  ); state.pickle(b)
          case b: ApplicableTagUpdate     => state.enc.writeByte(KeyApplicableTagUpdateV2  ); state.pickle(b)
          case b: ApplicableTagUpdateV1   => state.enc.writeByte(KeyApplicableTagUpdateV1  ); state.pickle(b)
          case b: CodeGroupCreate         => state.enc.writeByte(KeyCodeGroupCreate        ); state.pickle(b)
          case b: CodeGroupsDelete        => state.enc.writeByte(KeyCodeGroupsDelete       ); state.pickle(b)
          case b: CodeGroupUpdate         => state.enc.writeByte(KeyCodeGroupUpdate        ); state.pickle(b)
          case b: ContentRestore          => state.enc.writeByte(KeyContentRestore         ); state.pickle(b)
          case b: CustomIssueTypeCreate   => state.enc.writeByte(KeyCustomIssueTypeCreate  ); state.pickle(b)
          case b: CustomIssueTypeDelete   => state.enc.writeByte(KeyCustomIssueTypeDelete  ); state.pickle(b)
          case b: CustomIssueTypeRestore  => state.enc.writeByte(KeyCustomIssueTypeRestore ); state.pickle(b)
          case b: CustomIssueTypeUpdate   => state.enc.writeByte(KeyCustomIssueTypeUpdate  ); state.pickle(b)
          case b: CustomReqTypeCreate     => state.enc.writeByte(KeyCustomReqTypeCreateV2  ); state.pickle(b)
          case b: CustomReqTypeCreateV1   => state.enc.writeByte(KeyCustomReqTypeCreateV1  ); state.pickle(b)
          case b: CustomReqTypeDelete     => state.enc.writeByte(KeyCustomReqTypeDelete    ); state.pickle(b)
          case b: CustomReqTypeDeleteHard => state.enc.writeByte(KeyCustomReqTypeDeleteHard); state.pickle(b)
          case b: CustomReqTypeDeleteSoft => state.enc.writeByte(KeyCustomReqTypeDeleteSoft); state.pickle(b)
          case b: CustomReqTypeRestore    => state.enc.writeByte(KeyCustomReqTypeRestore   ); state.pickle(b)
          case b: CustomReqTypeUpdate     => state.enc.writeByte(KeyCustomReqTypeUpdateV2  ); state.pickle(b)
          case b: CustomReqTypeUpdateV1   => state.enc.writeByte(KeyCustomReqTypeUpdateV1  ); state.pickle(b)
          case b: FieldCustomDelete       => state.enc.writeByte(KeyFieldCustomDelete      ); state.pickle(b)
          case b: FieldCustomImpCreate    => state.enc.writeByte(KeyFieldCustomImpCreateV2 ); state.pickle(b)
          case b: FieldCustomImpCreateV1  => state.enc.writeByte(KeyFieldCustomImpCreateV1 ); state.pickle(b)
          case b: FieldCustomImpUpdate    => state.enc.writeByte(KeyFieldCustomImpUpdateV2 ); state.pickle(b)
          case b: FieldCustomImpUpdateV1  => state.enc.writeByte(KeyFieldCustomImpUpdateV1 ); state.pickle(b)
          case b: FieldCustomNumberCreate => state.enc.writeByte(KeyFieldCustomNumberCreate); state.pickle(b)
          case b: FieldCustomNumberUpdate => state.enc.writeByte(KeyFieldCustomNumberUpdate); state.pickle(b)
          case b: FieldCustomRestore      => state.enc.writeByte(KeyFieldCustomRestore     ); state.pickle(b)
          case b: FieldCustomTagCreate    => state.enc.writeByte(KeyFieldCustomTagCreateV2 ); state.pickle(b)
          case b: FieldCustomTagCreateV1  => state.enc.writeByte(KeyFieldCustomTagCreateV1 ); state.pickle(b)
          case b: FieldCustomTagUpdate    => state.enc.writeByte(KeyFieldCustomTagUpdateV2 ); state.pickle(b)
          case b: FieldCustomTagUpdateV1  => state.enc.writeByte(KeyFieldCustomTagUpdateV1 ); state.pickle(b)
          case b: FieldCustomTextCreate   => state.enc.writeByte(KeyFieldCustomTextCreateV2); state.pickle(b)
          case b: FieldCustomTextCreateV1 => state.enc.writeByte(KeyFieldCustomTextCreateV1); state.pickle(b)
          case b: FieldCustomTextUpdate   => state.enc.writeByte(KeyFieldCustomTextUpdateV2); state.pickle(b)
          case b: FieldCustomTextUpdateV1 => state.enc.writeByte(KeyFieldCustomTextUpdateV1); state.pickle(b)
          case b: FieldReposition         => state.enc.writeByte(KeyFieldReposition        ); state.pickle(b)
          case b: FieldStaticAdd          => state.enc.writeByte(KeyFieldStaticAdd         ); state.pickle(b)
          case b: FieldStaticRemove       => state.enc.writeByte(KeyFieldStaticRemove      ); state.pickle(b)
          case b: GenericReqCreate        => state.enc.writeByte(KeyGenericReqCreate       ); state.pickle(b)
          case b: GenericReqTitleSet      => state.enc.writeByte(KeyGenericReqTitleSet     ); state.pickle(b)
          case b: GenericReqTypeSet       => state.enc.writeByte(KeyGenericReqTypeSet      ); state.pickle(b)
          case b: ManualIssueCreate       => state.enc.writeByte(KeyManualIssueCreate      ); state.pickle(b)
          case b: ManualIssueDelete       => state.enc.writeByte(KeyManualIssueDelete      ); state.pickle(b)
          case b: ManualIssueUpdate       => state.enc.writeByte(KeyManualIssueUpdate      ); state.pickle(b)
          case b: ProjectDelete           => state.enc.writeByte(KeyProjectDelete          ); state.pickle(b)
          case b: ProjectNameSet          => state.enc.writeByte(KeyProjectNameSet         ); state.pickle(b)
          case b: ProjectRestore.type     => state.enc.writeByte(KeyProjectRestore         ); state.pickle(b)
          case b: ProjectTemplateApply    => state.enc.writeByte(KeyProjectTemplateApply   ); state.pickle(b)
          case b: ReqCodesPatch           => state.enc.writeByte(KeyReqCodesPatch          ); state.pickle(b)
          case b: ReqFieldCustomNumberSet => state.enc.writeByte(KeyReqFieldCustomNumberSet); state.pickle(b)
          case b: ReqFieldCustomTextSet   => state.enc.writeByte(KeyReqFieldCustomTextSet  ); state.pickle(b)
          case b: ReqImplicationsPatch    => state.enc.writeByte(KeyReqImplicationsPatch   ); state.pickle(b)
          case b: ReqsDelete              => state.enc.writeByte(KeyReqsDelete             ); state.pickle(b)
          case b: ReqTagsPatch            => state.enc.writeByte(KeyReqTagsPatch           ); state.pickle(b)
          case b: SavedViewCreate         => state.enc.writeByte(KeySavedViewCreateV2      ); state.pickle(b)
          case b: SavedViewCreateV1       => state.enc.writeByte(KeySavedViewCreateV1      ); state.pickle(b)
          case b: SavedViewDefaultSet     => state.enc.writeByte(KeySavedViewDefaultSet    ); state.pickle(b)
          case b: SavedViewDelete         => state.enc.writeByte(KeySavedViewDelete        ); state.pickle(b)
          case b: SavedViewUpdate         => state.enc.writeByte(KeySavedViewUpdateV2      ); state.pickle(b)
          case b: SavedViewUpdateV1       => state.enc.writeByte(KeySavedViewUpdateV1      ); state.pickle(b)
          case b: TagDelete               => state.enc.writeByte(KeyTagDelete              ); state.pickle(b)
          case b: TagGroupCreate          => state.enc.writeByte(KeyTagGroupCreate         ); state.pickle(b)
          case b: TagGroupUpdate          => state.enc.writeByte(KeyTagGroupUpdate         ); state.pickle(b)
          case b: TagRestore              => state.enc.writeByte(KeyTagRestore             ); state.pickle(b)
          case b: UseCaseCreate           => state.enc.writeByte(KeyUseCaseCreate          ); state.pickle(b)
          case b: UseCaseStepCreate       => state.enc.writeByte(KeyUseCaseStepCreate      ); state.pickle(b)
          case b: UseCaseStepDelete       => state.enc.writeByte(KeyUseCaseStepDelete      ); state.pickle(b)
          case b: UseCaseStepRestore      => state.enc.writeByte(KeyUseCaseStepRestore     ); state.pickle(b)
          case b: UseCaseStepShiftLeft    => state.enc.writeByte(KeyUseCaseStepShiftLeft   ); state.pickle(b)
          case b: UseCaseStepShiftRight   => state.enc.writeByte(KeyUseCaseStepShiftRight  ); state.pickle(b)
          case b: UseCaseStepUpdate       => state.enc.writeByte(KeyUseCaseStepUpdate      ); state.pickle(b)
          case b: UseCaseTitleSet         => state.enc.writeByte(KeyUseCaseTitleSet        ); state.pickle(b)
        }

      override def unpickle(implicit state: UnpickleState): Event =
        state.dec.readByte match {
          case KeyAccessUpdate            => state.unpickle[AccessUpdate]
          case KeyApplicableTagCreateV1   => state.unpickle[ApplicableTagCreateV1]
          case KeyApplicableTagCreateV2   => state.unpickle[ApplicableTagCreate]
          case KeyApplicableTagUpdateV1   => state.unpickle[ApplicableTagUpdateV1]
          case KeyApplicableTagUpdateV2   => state.unpickle[ApplicableTagUpdate]
          case KeyCodeGroupCreate         => state.unpickle[CodeGroupCreate]
          case KeyCodeGroupsDelete        => state.unpickle[CodeGroupsDelete]
          case KeyCodeGroupUpdate         => state.unpickle[CodeGroupUpdate]
          case KeyContentRestore          => state.unpickle[ContentRestore]
          case KeyCustomIssueTypeCreate   => state.unpickle[CustomIssueTypeCreate]
          case KeyCustomIssueTypeDelete   => state.unpickle[CustomIssueTypeDelete]
          case KeyCustomIssueTypeRestore  => state.unpickle[CustomIssueTypeRestore]
          case KeyCustomIssueTypeUpdate   => state.unpickle[CustomIssueTypeUpdate]
          case KeyCustomReqTypeCreateV1   => state.unpickle[CustomReqTypeCreateV1]
          case KeyCustomReqTypeCreateV2   => state.unpickle[CustomReqTypeCreate]
          case KeyCustomReqTypeDelete     => state.unpickle[CustomReqTypeDelete]
          case KeyCustomReqTypeDeleteHard => state.unpickle[CustomReqTypeDeleteHard]
          case KeyCustomReqTypeDeleteSoft => state.unpickle[CustomReqTypeDeleteSoft]
          case KeyCustomReqTypeRestore    => state.unpickle[CustomReqTypeRestore]
          case KeyCustomReqTypeUpdateV1   => state.unpickle[CustomReqTypeUpdateV1]
          case KeyCustomReqTypeUpdateV2   => state.unpickle[CustomReqTypeUpdate]
          case KeyFieldCustomDelete       => state.unpickle[FieldCustomDelete]
          case KeyFieldCustomImpCreateV1  => state.unpickle[FieldCustomImpCreateV1]
          case KeyFieldCustomImpCreateV2  => state.unpickle[FieldCustomImpCreate]
          case KeyFieldCustomImpUpdateV1  => state.unpickle[FieldCustomImpUpdateV1]
          case KeyFieldCustomImpUpdateV2  => state.unpickle[FieldCustomImpUpdate]
          case KeyFieldCustomNumberCreate => state.unpickle[FieldCustomNumberCreate]
          case KeyFieldCustomNumberUpdate => state.unpickle[FieldCustomNumberUpdate]
          case KeyFieldCustomRestore      => state.unpickle[FieldCustomRestore]
          case KeyFieldCustomTagCreateV1  => state.unpickle[FieldCustomTagCreateV1]
          case KeyFieldCustomTagCreateV2  => state.unpickle[FieldCustomTagCreate]
          case KeyFieldCustomTagUpdateV1  => state.unpickle[FieldCustomTagUpdateV1]
          case KeyFieldCustomTagUpdateV2  => state.unpickle[FieldCustomTagUpdate]
          case KeyFieldCustomTextCreateV1 => state.unpickle[FieldCustomTextCreateV1]
          case KeyFieldCustomTextCreateV2 => state.unpickle[FieldCustomTextCreate]
          case KeyFieldCustomTextUpdateV1 => state.unpickle[FieldCustomTextUpdateV1]
          case KeyFieldCustomTextUpdateV2 => state.unpickle[FieldCustomTextUpdate]
          case KeyFieldReposition         => state.unpickle[FieldReposition]
          case KeyFieldStaticAdd          => state.unpickle[FieldStaticAdd]
          case KeyFieldStaticRemove       => state.unpickle[FieldStaticRemove]
          case KeyGenericReqCreate        => state.unpickle[GenericReqCreate]
          case KeyGenericReqTitleSet      => state.unpickle[GenericReqTitleSet]
          case KeyGenericReqTypeSet       => state.unpickle[GenericReqTypeSet]
          case KeyManualIssueCreate       => state.unpickle[ManualIssueCreate]
          case KeyManualIssueDelete       => state.unpickle[ManualIssueDelete]
          case KeyManualIssueUpdate       => state.unpickle[ManualIssueUpdate]
          case KeyProjectDelete           => state.unpickle[ProjectDelete]
          case KeyProjectNameSet          => state.unpickle[ProjectNameSet]
          case KeyProjectRestore          => state.unpickle[ProjectRestore.type]
          case KeyProjectTemplateApply    => state.unpickle[ProjectTemplateApply]
          case KeyReqCodesPatch           => state.unpickle[ReqCodesPatch]
          case KeyReqFieldCustomNumberSet => state.unpickle[ReqFieldCustomNumberSet]
          case KeyReqFieldCustomTextSet   => state.unpickle[ReqFieldCustomTextSet]
          case KeyReqImplicationsPatch    => state.unpickle[ReqImplicationsPatch]
          case KeyReqsDelete              => state.unpickle[ReqsDelete]
          case KeyReqTagsPatch            => state.unpickle[ReqTagsPatch]
          case KeySavedViewCreateV1       => state.unpickle[SavedViewCreateV1]
          case KeySavedViewCreateV2       => state.unpickle[SavedViewCreate]
          case KeySavedViewDefaultSet     => state.unpickle[SavedViewDefaultSet]
          case KeySavedViewDelete         => state.unpickle[SavedViewDelete]
          case KeySavedViewUpdateV1       => state.unpickle[SavedViewUpdateV1]
          case KeySavedViewUpdateV2       => state.unpickle[SavedViewUpdate]
          case KeyTagDelete               => state.unpickle[TagDelete]
          case KeyTagGroupCreate          => state.unpickle[TagGroupCreate]
          case KeyTagGroupUpdate          => state.unpickle[TagGroupUpdate]
          case KeyTagRestore              => state.unpickle[TagRestore]
          case KeyUseCaseCreate           => state.unpickle[UseCaseCreate]
          case KeyUseCaseStepCreate       => state.unpickle[UseCaseStepCreate]
          case KeyUseCaseStepDelete       => state.unpickle[UseCaseStepDelete]
          case KeyUseCaseStepRestore      => state.unpickle[UseCaseStepRestore]
          case KeyUseCaseStepShiftLeft    => state.unpickle[UseCaseStepShiftLeft]
          case KeyUseCaseStepShiftRight   => state.unpickle[UseCaseStepShiftRight]
          case KeyUseCaseStepUpdate       => state.unpickle[UseCaseStepUpdate]
          case KeyUseCaseTitleSet         => state.unpickle[UseCaseTitleSet]
        }
    }

  implicit lazy val picklerActiveEvent: Pickler[ActiveEvent] =
    picklerEvent.narrow

  implicit lazy val picklerVerifiedEvent: Pickler[VerifiedEvent] =
    new Pickler[VerifiedEvent] {
      override def pickle(a: VerifiedEvent)(implicit state: PickleState): Unit = {
        state.pickle(a.ord)
        state.pickle(a.event)
        state.pickle(a.author)
        state.pickle(a.createdAt)
      }
      override def unpickle(implicit state: UnpickleState): VerifiedEvent = {
        val ord       = state.unpickle[EventOrd]
        val event     = state.unpickle[Event]
        val author    = state.unpickle[UserId]
        val createdAt = state.unpickle[Instant]
        VerifiedEvent(ord, event, author, createdAt)
      }
    }

  implicit lazy val picklerVerifiedEventSeq: Pickler[VerifiedEvent.Seq] =
    iterablePickler

  implicit lazy val picklerVerifiedEventNonEmptySeq: Pickler[VerifiedEvent.NonEmptySeq] =
    new Pickler[VerifiedEvent.NonEmptySeq] {
      override def pickle(a: VerifiedEvent.NonEmptySeq)(implicit state: PickleState): Unit = {
        state.pickle(a.head)
        state.pickle(a.tail)
      }
      override def unpickle(implicit state: UnpickleState): VerifiedEvent.NonEmptySeq = {
        val head = state.unpickle[VerifiedEvent]
        val tail = state.unpickle[VerifiedEvent.Seq]
        VerifiedEvent.NonEmptySeq(head, tail)
      }
    }

  implicit lazy val picklerErrorMsgOrVerifiedEventSeq: Pickler[ErrorMsg \/ VerifiedEvent.Seq] =
    pickleDisj

  implicit lazy val picklerProjectEvents: Pickler[ProjectEvents] =
    transformPickler(ProjectEvents.apply)(_.events)

  implicit lazy val picklerProjectOrEvents: Pickler[Project \/ VerifiedEvent.Seq] =
    pickleDisj

  // ===================================================================================================================

  implicit lazy val picklerProjectMetaData: Pickler[ProjectMetaData] =
    new Pickler[ProjectMetaData] {
      override def pickle(a: ProjectMetaData)(implicit state: PickleState): Unit = {
        writeVersion(1)
        state.pickle(a.id)
        state.pickle(a.role)
        state.pickle(a.name)
        state.pickle(a.eventsInit)
        state.pickle(a.eventsTotal)
        state.pickle(a.reqsLive)
        state.pickle(a.reqsTotal)
        state.pickle(a.createdAt)
        state.pickle(a.accessedAt)
        state.pickle(a.lastUpdatedAt)
        state.pickle(a.live)
      }
      override def unpickle(implicit state: UnpickleState): ProjectMetaData =
        readByVersion(1) {

          // v1.0
          case 0 =>
            val id            = state.unpickle[ProjectId.Public]
            val role          = state.unpickle[Option[ProjectRole]]
            val name          = state.unpickle[Project.Name]
            val eventsInit    = state.unpickle[Int]
            val eventsTotal   = state.unpickle[Int]
            val reqsLive      = state.unpickle[Int]
            val reqsTotal     = state.unpickle[Int]
            val createdAt     = state.unpickle[Instant]
            val accessedAt    = state.unpickle[Instant]
            val lastUpdatedAt = state.unpickle[Option[Instant]]
            ProjectMetaData(
              id            = id,
              role          = role,
              name          = name,
              eventsInit    = eventsInit,
              eventsTotal   = eventsTotal,
              reqsLive      = reqsLive,
              reqsTotal     = reqsTotal,
              createdAt     = createdAt,
              accessedAt    = accessedAt,
              lastUpdatedAt = lastUpdatedAt,
              live          = Live)

          // v1.1
          case 1 =>
            val id            = state.unpickle[ProjectId.Public]
            val role          = state.unpickle[Option[ProjectRole]]
            val name          = state.unpickle[Project.Name]
            val eventsInit    = state.unpickle[Int]
            val eventsTotal   = state.unpickle[Int]
            val reqsLive      = state.unpickle[Int]
            val reqsTotal     = state.unpickle[Int]
            val createdAt     = state.unpickle[Instant]
            val accessedAt    = state.unpickle[Instant]
            val lastUpdatedAt = state.unpickle[Option[Instant]]
            val live          = state.unpickle[Live]
            ProjectMetaData(
              id            = id,
              role          = role,
              name          = name,
              eventsInit    = eventsInit,
              eventsTotal   = eventsTotal,
              reqsLive      = reqsLive,
              reqsTotal     = reqsTotal,
              createdAt     = createdAt,
              accessedAt    = accessedAt,
              lastUpdatedAt = lastUpdatedAt,
              live          = live)
        }
    }
}
