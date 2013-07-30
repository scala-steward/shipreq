package com.beardedlogic.usecase.util

import scalaz.{LensFamily, Lens}
import Lens.{lensg, lensFamilyg}

object LensFns {

  /**
   * A lens that requires a key be provided to extract B from A.
   *
   * @tparam A The record type.
   * @tparam K The field key type.
   * @tparam B The field type.
   */
  def KeyedLens[A, K, B](set: A => K => B => A, get: A => K => B) =
    lensFamilyg[(A, K), A, B, B](
      ak => b => set(ak._1)(ak._2)(b),
      ak => get(ak._1)(ak._2)
    )

  private def composeKK[A, K1, B, K2, C](g: LensFamily[(A, K1), A, B, B], f: LensFamily[(B, K2), B, C, C]) =
    lensFamilyg[(A, K1, K2), A, C, C](
      akl => c => {
        val (a, k1, k2) = akl
        val b1 = g.get(a, k1)
        val b2 = f.set((b1, k2), c)
        g.set((a, k1), b2)
      },
      akl => {
        val (a, k1, k2) = akl
        val b = g.get(a, k1)
        f.get(b, k2)
      }
    )

  private def composeLK[A, B, K, C](g: Lens[A, B], f: LensFamily[(B, K), B, C, C]) =
    lensFamilyg[(A, K), A, C, C](
      ak => c => {
        val (a, k) = ak
        val b1 = g.get(a)
        val b2 = f.set((b1, k), c)
        g.set(a, b2)
      },
      ak => {
        val (a, k) = ak
        val b = g.get(a)
        f.get(b, k)
      }
    )

  private def composeKL[A, K, B, C](g: LensFamily[(A, K), A, B, B], f: Lens[B, C]) =
    lensFamilyg[(A, K), A, C, C](
      ak => c => {
        val b1 = g.get(ak)
        val b2 = f.set(b1, c)
        g.set(ak, b2)
      },
      ak => f.get(g.get(ak))
    )

  implicit class KeyedLensExt[A, K, B](val l: LensFamily[(A, K), A, B, B]) extends AnyVal {
    def >@=@>[K2, C](f: LensFamily[(B, K2), B, C, C]) = composeKK(l, f)
    def >@==>[C](f: Lens[B, C]) = composeKL(l, f)
    def <==@<[Z](g: Lens[Z, A]) = composeLK(g, l)
    def <@=@<[Z, K0](g: LensFamily[(Z, K0), Z, A, A]) = composeKK(g, l)
  }

  implicit class PlainLensExt[A, B](val l: Lens[A, B]) extends AnyVal {
    def >=@>[K, C](f: LensFamily[(B, K), B, C, C]) = composeLK(l, f)
    def <@=<[Z, K](g: LensFamily[(Z, K), Z, A, A]) = composeKL(g, l)
  }

  implicit class AllLensExt[A1, A2, B](val l: LensFamily[A1, A2, B, B]) extends AnyVal {
    def <@(key: A1) = AppliedLens(l, key)
  }
}

/**
 * Composition of a lens and record instance.
 *
 * Allows stuff to be put in and taken out without specifying the source/target.
 */
class AppliedLens[A, B](val get: B, val set: B => A) {
  def mod(f: B => B) = set(f(get))
}

object AppliedLens {
  def apply[A1, A2, B](lens: LensFamily[A1, A2, B, B], key: A1): AppliedLens[A2, B] =
    new AppliedLens(lens.get(key), lens.set(key, _))
}
