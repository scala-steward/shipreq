package shipreq.webapp.base.validation2

import scalaz.{Equal, NonEmptyList}
import scalaz.syntax.equal._

final class Uniqueness[_S, D, K, V](data:    _S => Stream[D],
                                   key:      (_S, V) => K,
                                   ignore:   K => D => Boolean,
                                   cmp:      V => D => Boolean,
                                   vfailure: VFailure) {

  type S = _S

  def contramap[T](f: T => S): Uniqueness[T, D, K, V] =
    new Uniqueness(f andThen data, (t, v) => key(f(t), v), ignore, cmp, vfailure)

  def addData(f: S => Stream[D]): Uniqueness[S, D, K, V] =
    new Uniqueness(s => data(s) #::: f(s), key, ignore, cmp, vfailure)

  def isValid(s: S, v: V): Boolean = {
    val k = key(s, v)
    val ignoreD = ignore(k)
    val cmpD = cmp(v)
    data(s).forall(d =>
      !cmpD(d) || ignoreD(d))
  }

  def vp: ValidationPart[S, V, V] =
    ValidationPart.test(isValid, vfailure)
}

object Uniqueness {

  def vfailure(fieldName: String): VFailure =
    VFailure.forField(fieldName, NonEmptyList("must be unique."))

  def over[P] = new BP[P]
  final class BP[P] {
    def apply[K: Equal, V: Equal](pk: P => K, pv: P => V) =
      new BF[(Stream[P], K), P, K, V](f =>
        new Uniqueness(
          _._1,
          (s, _) => s._2,
          k => k ≟ pk(_),
          v => v ≟ pv(_),
          f))
  }

  final class BF[S, D, K, V](build: VFailure => Uniqueness[S, D, K, V]) {
    def forField(fieldName: String) = build(vfailure(fieldName))
  }
}


/*

k⁻
v⁻
Stream shit
shit -> k⁻ -> boolean?
shit -> v
compare v to v⁻
field name


    def nameUniqueness(names: Map[Long, String], id: Long, name: String): Option[VFailure] =
      (names - id).forall(_._2 != name) match {
        case true  => None
        case false => Some(uniquenessFailure("Name"))
      }
    def uniquenessFailure(fieldName: String): VFailure =
      VFailure.forField(fieldName, NonEmptyList("must be unique."))
    def tovps[S,A](f: (S,InputCorrected[A]) => Option[VFailure]): ValidationPart[S, A, A] =
      new ValidationPart((s,a) => f(s,a) match {
        case None    => Success(a.value)
        case Some(r) => Failure(r)
      })


  private def mnemonicUniqueness =
    TableConstraint.uniquenessE[prespec.S, prespec.R, Mnemonic](
      (s, r) => {
        val custom: Stream[ReqType] =
          s._1.toStream
            .filterNot(dpi => r.fold(false)(_ == dpi._1)) // ignore own row
            .map(_._2.p)
        val static: Stream[ReqType] = ReqType.static.toStream
        (static #::: custom).flatMap(p => p.mnemonic #:: p.oldMnemonics.toStream)
      }).fieldName("Mnemonic")

  def uniquenessFailure(fieldName: String) =
    VFailure.forField(fieldName, NonEmptyList("must be unique."))

  final class UniquenessB[S, R, A](b: VFailure => ValidatePlusR[S, R, A]) {
    def apply(fail: VFailure) = b(fail)
    def fieldName(fieldName: String) = apply(uniquenessFailure(fieldName))
    def failureMsg(msg: String) = apply(VFailure looseMsg msg)
  }

  def uniqueness[S, R, A, B](extract: (S, R) => Stream[A], cmp: (A, B) => Boolean) =
    new UniquenessB[S, R, B](
      fail => r => (s, b) =>
        if (extract(s, r).exists(cmp(_, b))) Some(fail) else None
    )

  def uniquenessE[S, R, A: Equal](extract: (S, R) => Stream[A]) =
    uniqueness[S, R, A, A](extract, implicitly[Equal[A]].equal)

  def uniquenessT[D, P, II, A: Equal](pa: P => A) =
    uniqueness[SavedUnsaved[D, P, II], Option[D], (D, SavedRow[P, II]), A](
      // D is used to key maps so Scala equality must hold, no need for Equal[D]
      (s, od) => s._1.toStream.filter(r => od.forall(_ != r._1)),
      (r, a)  => a ≟ pa(r._2.p))

 */