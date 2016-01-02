package shipreq.webapp.base.test

import nyaya.gen._
import shipreq.base.util.NonEmptyVector
import shipreq.base.util.ScalaExt._
import shipreq.base.test.BaseUtilGen._
import shipreq.webapp.base.util.GenericData

object WebappBaseGen {

  abstract class GenericDataGen[GD <: GenericData](final val gd: GD) {
    import gd.equalityAttr

    def valueFor(a: gd.Attr): Gen[gd.Value]

    val attr = Gen.chooseNE(gd.attrs)

    lazy val values: Gen[gd.Values] =
      attr.set
        .flatMap(as => Gen sequence as.iterator.map(valueFor).toVector)
        .map(_.foldLeft(gd.emptyValues)(_ + _))

    val nonEmptyValues: Gen[gd.NonEmptyValues] =
      attr.nes
        .flatMap(as => Gen sequence as.iterator.map(valueFor).toVector)
        .map(vs => gd.nev(vs.head, vs.tail: _*))
  }

  abstract class GenericDataOptionGen[GD <: GenericData](final val gd: GD) {
    import gd.equalityAttr

    def valueFor(a: gd.Attr): Option[Gen[gd.Value]]

    val gens: Map[gd.Attr, Gen[gd.Value]] =
      gd.attrs.iterator
        .map(a => (a, valueFor(a)))
        .filterDefined_2
        .toMap

    val attrNE: Option[Gen[gd.Attr]] =
      NonEmptyVector.option(gens.keySet.toVector) map (Gen chooseNE _)

    lazy val values: Gen[gd.Values] =
      attrNE.fold(Gen pure gd.emptyValues)(_
        .set
        .flatMap(as => Gen sequence as.iterator.map(gens).toVector)
        .map(_.foldLeft(gd.emptyValues)(_ + _)))

    val nonEmptyValues: Option[Gen[gd.NonEmptyValues]] =
      attrNE.map(_
        .nes
        .flatMap(as => Gen sequence as.iterator.map(gens).toVector)
        .map(vs => gd.nev(vs.head, vs.tail: _*)))
  }

}
