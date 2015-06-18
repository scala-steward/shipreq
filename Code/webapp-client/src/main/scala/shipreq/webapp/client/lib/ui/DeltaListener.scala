package shipreq.webapp.client.lib.ui

import japgolly.scalajs.react.TopNode
import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react.extra.{Listenable, OnUnmount}
import shipreq.base.util.NonEmptySet
import scalaz.effect.IO
import shipreq.webapp.base.delta._
import shipreq.webapp.client.ClientData
import shipreq.webapp.client.delta._

// TODO Replace DeltaListener with accurately-targeted Px & Reusability.

class DeltaListener[S](val h: LocalDelta => S => S) extends AnyVal {

  /**
   * Install as a React component mixin.
   */
  def install[P, B <: OnUnmount, N <: TopNode](cd: P => ClientData) =
    Listenable.install[P, S, B, N, LocalDelta](cd, $ => ld => $.modState(h(ld)))
}

object DeltaListener {

  def partition[S](p: Partition)(f: LocalDeltaP.Aux[p.type] => S => S): DeltaListener[S] =
    new DeltaListener(_.get(p).fold((s: S) => s)(f))

  // -------------------------------------------------------------------------------------------------------------------

  /**
   * DSL for building a DeltaListener using functions that process each change one-by-one.
   */
  class OneByOne[S, I, D](val remove: (S, I) => S,
                          val put: (S, I, D) => S) {

    def partialContramap[J, B](ji: J => Option[I], bd: B => Option[D]): OneByOne[S, J, B] =
      new OneByOne(
        (s, j)    => ji(j).fold(s)(i => remove(s, i)),
        (s, j, b) => ji(j).flatMap(i => bd(b).map(d => put(s, i, d))) getOrElse s)

    def apply(p: Partition.Aux[D, I]): DeltaListener[S] =
      DeltaListener.partition[S](p)(d => s1 => {
        val s2 = (s1 /: d.delete)((s, id)   => remove(s, id))
        val s3 = (s2 /: d.update)((s, data) => put(s, p.di.id(data), data))
        s3
      })

    def partial[J, B](p: Partition.Aux[B, J])(ji: J => Option[I], bd: B => Option[D]): DeltaListener[S] =
      partialContramap(ji, bd)(p)
  }

  def store[S, I, D](store: SavedRowStore[S, I, D, _]): OneByOne[S, I, D] =
    new OneByOne[S, I, D](
      (s, i)    => store.remove(i)(s),
      (s, i, d) => store.set(i, d)(s))

  // -------------------------------------------------------------------------------------------------------------------

  def refreshOnChange[P, S, B <: OnUnmount, N <: TopNode](cd: P => ClientData, ps: NonEmptySet[Partition]) =
    Listenable.installIO[P, S, B, N, LocalDelta](cd, ($, ld) =>
      if (ps.exists(ld.get(_).isDefined))
        $.forceUpdateIO
      else
        IO(())
    )
}