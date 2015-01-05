package shipreq.webapp.base.protocol

import scalaz.{\&/, NonEmptyList}
import scalaz.Isomorphism.<=>

import upickle._
import upickle.Fns._
import upickle.TupleCodecs._
import upickle.StdlibCodecs.{SeqishR, SeqishW}
import upickle.StdlibCodecs.{MapR, MapW}

import shipreq.prop.util._
import shipreq.base.util._
import shipreq.base.util.TaggedTypes._
import shipreq.webapp.base.data._
import DataImplicits._

private[protocol] object CodecBase {

  //  private def tagS[T <: TaggedString](implicit C: TaggedTypeCtor[T]) =
  //    ReadWriter[T](i => Js.Str(i.value), { case Js.Str(i) => C(i)})
  //  private def tagL[T <: TaggedLong](implicit C: TaggedTypeCtor[T]) =
  //    ReadWriter[T](i => Js.Str(i.value.toString), { case Js.Str(i) => C(i.toLong)})

  def tagS[T <: TaggedString](C: String => T) =
    ReadWriter[T](i => Js.Str(i.value), { case Js.Str(i) => C(i)})

  def tagL[T <: TaggedLong](C: Long => T) =
//    import java.lang.{Long => L}
//    import java.lang.Character.{MAX_RADIX => R} // TODO patch scala.js
//    ReadWriter[T](i => Js.Str(L.toString(i, R)), { case Js.Str(i) => C(L.parseLong(i, R)) })
    ReadWriter[T](i => Js.Str(i.value.toString), { case Js.Str(i) => C(i.toLong)})

  def boolCase[T](iso: Boolean <=> T) = {
    val T = "t"
    val F = "f"
    ReadWriter[T](t => if (iso from t) Js.Str(T) else Js.Str(F), {
      case Js.Str(T) => iso to true
      case Js.Str(F) => iso to false
    })
  }

  // UNSAFE. Make sure tests using exhaustive pattern matching to cover this hierarchy
  def enum[T](ts: NonEmptyList[T]) = {
    val table = BiMap(ts.list.zipWithIndex.map(p => p._1 -> ('0' + p._2).toChar.toString).toMap)
    ReadWriter[T](t => Js.Str(table.ab(t)), {
      case Js.Str(k) if table.ba.contains(k) => table.ba(k)
    })
  }

  def xmap[A, B](f: A => B)(g: B => A)(implicit RB: Reader[B], WB: Writer[B]) =
    ReadWriter[A](WB.write compose f, RB.read andThen g)

  implicit class ReaderExt[T](val r: Reader[T]) extends AnyVal {
    @inline def readSet[TT >: T](s: Seq[Js.Value]): Set[TT] =
      foldlSeq[Set[TT]](s, Set.empty)(_ + _)

    @inline def readList(s: Seq[Js.Value]): List[T] =
      foldlSeq[List[T]](s, Nil)((a, b) => b :: a)
      // foldrSeq[List[T]](s, Nil)(_ :: _)

    @inline def foldlSeq[B](s: Seq[Js.Value], z: B)(f: (B, T) => B): B =
      s.foldLeft(z)((b, j) => f(b, r read j))

    @inline def foldrSeq[B](s: Seq[Js.Value], z: B)(f: (T, B) => B): B =
      s.foldRight(z)((j, b) => f(r read j, b))
  }

  def writeIterable[T](ts: Iterable[T])(implicit W: Writer[T]) =
    Js.Arr(ts.foldLeft(List.empty[Js.Value])((q,i) => W.write(i) :: q): _*)

  def caseclass1[A, Z](y: A => Z, u: Z => Option[A])(implicit RA: Reader[A], WA: Writer[A]) =
    ReadWriter[Z](z => WA write u(z).get, RA.read andThen y)

  def caseclass2[A: Reader : Writer, B: Reader : Writer, Z]
  (y: (A, B) => Z, u: Z => Option[(A, B)]): ReadWriter[Z] = {
    val r = Tuple2R[A, B].read
    val w = Tuple2W[A, B].write
    ReadWriter[Z](z => w(u(z).get), r andThen y.tupled)
  }

  def caseclass3[A: Reader : Writer, B: Reader : Writer, C: Reader : Writer, Z]
  (y: (A, B, C) => Z, u: Z => Option[(A, B, C)]): ReadWriter[Z] = {
    val r = Tuple3R[A, B, C].read
    val w = Tuple3W[A, B, C].write
    ReadWriter[Z](z => w(u(z).get), r andThen y.tupled)
  }

  def caseclass4[A: Reader : Writer, B: Reader : Writer, C: Reader : Writer, D: Reader : Writer, Z]
  (y: (A, B, C, D) => Z, u: Z => Option[(A, B, C, D)]): ReadWriter[Z] = {
    val r = Tuple4R[A, B, C, D].read
    val w = Tuple4W[A, B, C, D].write
    ReadWriter[Z](z => w(u(z).get), r andThen y.tupled)
  }
  
  def caseclass5[A: Reader : Writer, B: Reader : Writer, C: Reader : Writer, D: Reader : Writer, E: Reader : Writer, Z]
  (y: (A, B, C, D, E) => Z, u: Z => Option[(A, B, C, D, E)]): ReadWriter[Z] = {
    val r = Tuple5R[A, B, C, D, E].read
    val w = Tuple5W[A, B, C, D, E].write
    ReadWriter[Z](z => w(u(z).get), r andThen y.tupled)
  }

  def caseclass6[A: Reader : Writer, B: Reader : Writer, C: Reader : Writer, D: Reader : Writer, E: Reader : Writer, F: Reader : Writer, Z]
  (y: (A, B, C, D, E, F) => Z, u: Z => Option[(A, B, C, D, E, F)]): ReadWriter[Z] = {
    val r = Tuple6R[A, B, C, D, E, F].read
    val w = Tuple6W[A, B, C, D, E, F].write
    ReadWriter[Z](z => w(u(z).get), r andThen y.tupled)
  }

  def intkeyW[T](k: Int, t: T)(implicit T: Writer[T]) =
    Js.Arr(Js.Num(k), T write t)

  def iMap[K: Reader : Writer, V: Reader : Writer](key: V => K): ReadWriter[IMap[K, V]] =
    xmap((_: IMap[K, V]).underlyingMap)(m => IMap.empty(key).replaceUnderlying(m))

  /*
  Something like this for ADTs maybe?
  val vv = Vector[ReadWriter[_ <: Tag]](tagGroup, applicableTag)
  def w: Tag => ReadWriter[_ <: Tag] = {
    case t: TagGroup      => vv(0)
    case t: ApplicableTag => vv(1)
  }
  implicit def tag2 = ReadWriter[Tag](
  t => w(t).asInstanceOf[Writer[Tag]] write0 t, {
    case Js.Arr(Js.Num(n), v) => vv(n.toInt) read0 v
  })
  */
}
import CodecBase._

// =====================================================================================================================
object DataCodecs {

  @inline implicit def string = BaseCodecs.StringRW
  @inline implicit def unit   = BaseCodecs.UnitRW

  implicit def option[A: Reader: Writer]: ReadWriter[Option[A]] =
    ReadWriter[Option[A]](
    _.fold(Js.Arr())(a => Js.Arr(writeJs(a))), {
      case Js.Arr()  => None
      case Js.Arr(a) => Some(readJs[A](a))
    })

  implicit def these[A: Reader: Writer, B: Reader: Writer]: ReadWriter[A \&/ B] = {
    import \&/._
    ReadWriter[A \&/ B]({
      case This(a)    => intkeyW(1, a)
      case That(b)    => intkeyW(2, b)
      case Both(a, b) => Js.Arr(Js.Num(3), writeJs(a), writeJs(b))
    }, {
      case Js.Arr(Js.Num(n), v) => n.toInt match {
        case 1 => This(readJs[A](v))
        case 2 => That(readJs[B](v))
      }
      case Js.Arr(Js.Num(n), a, b) if n.toInt == 3 =>
        Both(readJs[A](a), readJs[B](b))
    })
  }

  @inline implicit def iMapAuto[K: Reader : Writer, V: Reader : Writer](implicit d: DataIdAux[V, K]): ReadWriter[IMap[K, V]] =
    iMap(d.id)

  implicit def rev        = tagL(Rev.apply)
  implicit def alive      = boolCase(Alive)
  implicit def impReq     = boolCase(ImplicationRequired)
  implicit def isEnumLike = boolCase(MutexChildren)
  implicit def refkey     = tagS(RefKey.apply)

  implicit def customIssueTypeId = tagL(CustomIssueType.Id.apply)
  implicit def customIssueType   = caseclass4(CustomIssueType.apply, CustomIssueType.unapply)

  implicit def reqTypeMnemonic = tagS(ReqType.Mnemonic.apply)
  implicit def customReqTypeId = tagL(CustomReqType.Id.apply)
  implicit def customReqType   = caseclass6(CustomReqType.apply, CustomReqType.unapply)

  implicit def tagId         = tagL(Tag.Id.apply)
  implicit def tagGroup      = caseclass5(TagGroup.apply, TagGroup.unapply)
  implicit def applicableTag = caseclass5(ApplicableTag.apply, ApplicableTag.unapply)
  implicit def tag           = ReadWriter[Tag]({
      case t: TagGroup      => intkeyW(0, t)
      case t: ApplicableTag => intkeyW(1, t)
    }, {
      case Js.Arr(Js.Num(n), v) => n.toInt match {
        case 0 => readJs[TagGroup](v)
        case 1 => readJs[ApplicableTag](v)
      }
    })

  implicit def tagInTree           = caseclass2(TagInTree.apply, TagInTree.unapply)
  implicit def tagTree             = iMap[Tag.Id, TagInTree](_.tag.id)
  implicit def tagPovRelations     = caseclass2(TagProtocol.PovRelations.apply, TagProtocol.PovRelations.unapply)
  implicit def tagPov              = caseclass2(TagProtocol.PovTag.apply, TagProtocol.PovTag.unapply)
  implicit def tagGroupValues      = caseclass3(TagProtocol.TagGroupValues.apply, TagProtocol.TagGroupValues.unapply)
  implicit def applicableTagValues = caseclass3(TagProtocol.ApplicableTagValues.apply, TagProtocol.ApplicableTagValues.unapply)
  implicit def tagValues           = ReadWriter[TagProtocol.Values]({
      case t: TagProtocol.TagGroupValues      => intkeyW(0, t)
      case t: TagProtocol.ApplicableTagValues => intkeyW(1, t)
    }, {
      case Js.Arr(Js.Num(n), v) => n.toInt match {
        case 0 => readJs[TagProtocol.TagGroupValues](v)
        case 1 => readJs[TagProtocol.ApplicableTagValues](v)
      }
    })

  implicit def revAnd[T](implicit WT: Writer[T], RT: Reader[T]) =
    caseclass2(RevAnd.apply[T], RevAnd.unapply[T])

  implicit def project = caseclass3(Project.apply, Project.unapply)
}

// =====================================================================================================================
object RoutineDataCodecs {

  implicit def deletionAction = enum(DeletionAction.values)

  def crudAction[I, V](implicit WI: Writer[I], RI: Reader[I], WV: Writer[V], RV: Reader[V]): ReadWriter[CrudAction[I, V]] =
    ReadWriter[CrudAction[I, V]]({
      case CrudAction.Create(v)    => Js.Arr(WV write v)
      case CrudAction.Update(i, v) => Js.Arr(WI write i, WV write v)
      case CrudAction.Delete(i, a) => Js.Arr(WI write i, deletionAction write a, Js.Obj())
    }, {
      case Js.Arr(v)       => CrudAction.Create(RV read v)
      case Js.Arr(i, v)    => CrudAction.Update(RI read i, RV read v)
      case Js.Arr(i, a, _) => CrudAction.Delete(RI read i, deletionAction read a)
    })
}

// =====================================================================================================================
object RoutineRemoteCodecs {
  import Routines._

  def remoteRoutine[R <: Routine.Desc](d: R): ReadWriter[d.Remote] =
    ReadWriter[d.Remote](r => Js.Str(r.n), { case Js.Str(n) => Routine.Remote(n, d) })

  implicit def projectInit   = remoteRoutine(ProjectInit)
  implicit def issueTypeCrud = remoteRoutine(CustomIssueTypeCrud)
  implicit def reqTypeCrud   = remoteRoutine(CustomReqTypeCrud)
  implicit def reqTypeImpMod = remoteRoutine(CustomReqTypeImplicationMod)
  implicit def tagCrud       = remoteRoutine(TagCrud)

  implicit def projectSPA = caseclass5(ProjectSPA.apply, ProjectSPA.unapply)
}

// =====================================================================================================================
object DeltaCodecs {
  import shipreq.webapp.base.delta._
  import DataCodecs.rev

  implicit def partitions = enum(Partition.values)

  implicit def remoteDeltaGW = Writer[RemoteDeltaG](r => {
    import r.p.{wi, wd}
    val dp = r.forceDeltaP[r.p.type](r.p)
    val a = partitions write r.p
    val b = rev write r.from
    val c = rev write r.to
    val d = writeIterable(dp.del)(wi)
    val e = writeIterable(dp.upd)(wd)
    Js.Arr(a, b, c, d, e)
  })

  implicit def remoteDeltaGR = Reader[RemoteDeltaG]({
    case Js.Arr(a, b, c, Js.Arr(d@_*), Js.Arr(e@_*)) =>
      val p = partitions read a
      val f = rev read b
      val t = rev read c
      val x = p.ri.readSet(d)
      val y = p.rd.readList(e)
      RemoteDeltaG(p, f, t)(x, y)
  })

//  implicit def remoteDelta: ReadWriter[RemoteDelta] = implicitly[ReadWriter[List[RemoteDeltaG]]]
  implicit def remoteDelta = ReadWriter[RemoteDelta](
    SeqishW[RemoteDeltaG, List].write,
    SeqishR[RemoteDeltaG, List].read)

}