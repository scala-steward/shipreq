package shipreq.webapp.client.base.feature

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.Reusability
import japgolly.scalajs.react.MonocleReact._
import monocle.Lens
import scala.annotation.elidable
import shipreq.base.util.Intersection
import shipreq.base.util.univeq._
import shipreq.webapp.client.base.data.TCB

/**
  * Provides the following functionality around async actions:
  *
  * - A status to use between initiation and successful completion of an async action. Statuses are:
  *   - Locked: the async action is in progress, awaiting a response (or timeout).
  *   - Failed: the async action failed.
  * - When an async action has failed, this provides:
  *   - Retry functionality.
  *   - Cancel/abort functionality.
  *
  * This is a lower-level building block. You'll usually lift this into an [[EditorStatus]].
  */
object AsyncActionFeature {
  // TODO Names in this file are terrible: apply{,D1}; statusD[12]; setD1{,s}

  /**
    * @tparam F The type of async failure.
    */
  sealed abstract class Status[+F]

  case object Locked extends Status[Nothing]

  final case class Failed[F](failure: F, retry: Callback, resumeEdit: Callback) extends Status[F]

  implicit def reusabilityStatus[F]: Reusability[Status[F]] =
    Reusability.byRef

  /** Provides two callbacks for you to use in your async logic:
    * one for you to call on async success
    * one for you to call on async failure
    */
  type AsyncCall[+F] = (TCB.Success, F => TCB.Failure) => Callback

  // ===================================================================================================================

  /**
   * Provides the feature for a single value.
   */
  object D0 {
    type State[+F] = Option[Status[F]]

    def initState: State[Nothing] =
      None

    type Feature[-F] = AsyncCall[F] => Callback

    object Feature {
      def apply[F]($: StateAccessPure[State[F]]): Feature[F] =
        fn($.setState(_))

      def fn[F](setStatus: Option[Status[F]] => Callback): Feature[F] =
        call => {
          val clearStatus = setStatus(None)

          def onSuccess: TCB.Success =
            TCB Success clearStatus

          def onFailure: F => TCB.Failure =
            f => TCB Failure setStatus(Some(Failed(f, Callback byName doIt, clearStatus)))

          lazy val doIt: Callback =
            // Switching this around breaks tests' MockServer's order of events.
            // i.e. it will call onSuccess which clears the status, and then set it to locked.
            setStatus(Some(Locked)) >> call(onSuccess, onFailure)

          doIt
        }

      def Nop: Feature[Any] =
        _ => Callback.empty
    }
  }

  // ===================================================================================================================

  object D1 {

    /** @tparam SK State key
      * @tparam K  Practical/usage key
      */
    final class State[SK, K, +F](private[State] val values: Map[SK, Status[F]],
                                 intr: Intersection[SK, K]) extends State.ReadOnly[K, F] {

      @elidable(elidable.FINE)
      override def toString = s"D1.State($values)"

      override def isEmpty: Boolean =
        values.isEmpty

      override def apply(key: K): D0.State[F] =
        intr.reverse.getOption(key).flatMap(values.get)

      def set[FF >: F](key: K, o: D0.State[FF]): State[SK, K, FF] = {
        val m = Dimensions.set1(intr)(values, key, o)
        new State(m, intr)
      }

      override def mapKey[C](j: Intersection[K, C]): State[SK, C, F] =
        new State(values, intr <=> j)

      def mergeInto[FF >: F](parent: State[SK, SK, FF]): State[SK, SK, FF] = {
        val m = Dimensions.merge(intr.getOption)(parent.values, values)
        new State(m, Intersection.id[SK])
      }
    }

    object State {
      type Simple[K, +F] = State[K, K, F]

      sealed abstract class ReadOnly[K, +F] {
        def isEmpty: Boolean
        def apply(key: K): D0.State[F]
        def mapKey[C](q: Intersection[K, C]): ReadOnly[C, F]
      }

      implicit def reusabilityState1[K, F]: Reusability[ReadOnly[K, F]] =
        Reusability.byRef || Reusability.when(_.isEmpty)

      private[AsyncActionFeature] def empty[A, B](p: Intersection[A, B]): State[A, B, Nothing] =
        new State(Map.empty, p)

      private[AsyncActionFeature] def emptyA[A]: State[A, A, Nothing] =
        empty(Intersection.id[A])

      def init[A: UnivEq]: State[A, A, Nothing] =
        emptyA

      def at[A, B, F](b: B): Lens[State[A, B, F], D0.State[F]] =
        Lens((_: State[A, B, F])(b))(o => _.set(b, o))
    }

    abstract class Feature[K, -F] {
      def apply(k: K): D0.Feature[F]
      def mapKey[C](j: Intersection[K, C]): Feature[C, F]
    }

    object Feature {
      private final class Impl[S, A, B, -F]($: StateAccessPure[State[S, A, F]],
                                            intr: Intersection[A, B]) extends Feature[B, F] {
        override def apply(b: B) =
          intr.reverse.fold(b, a => D0.Feature($ zoomStateL State.at(a)))(D0.Feature.Nop)

        override def mapKey[C](j: Intersection[B, C]) =
          new Impl($, intr <=> j)
      }

      @inline def apply[S, A, F]($: StateAccessPure[State[S, A, F]]): Feature[A, F] =
        apply($, Intersection.id[A])

      def apply[S, A, B, F]($: StateAccessPure[State[S, A, F]], i: Intersection[A, B]): Feature[B, F] =
        new Impl($, i)

      def nop[K]: Feature[K, Any] =
        new Feature[K, Any] {
          override def apply(k: K)                      = D0.Feature.Nop
          override def mapKey[C](j: Intersection[K, C]) = nop
        }
      }
  }

  // ===================================================================================================================

  object D2 {

    /** @tparam SK2 State key #2
      * @tparam K2  Practical/usage key #2
      * @tparam SK1 State key #1
      * @tparam K1  Practical/usage key #1
      */
    final class State[SK2, K2, SK1, K1, F](values: Map[SK2, D1.State.Simple[SK1, F]],
                                           intr2: Intersection[SK2, K2],
                                           intr1: Intersection[SK1, K1]) extends State.ReadOnly[K2, K1, F] {
      @elidable(elidable.FINE)
      override def toString = s"D2.State($values)"

      override def isEmpty: Boolean =
        values.isEmpty

      override def apply(key: K2): D1.State[SK1, K1, F] =
        intr2.reverse.fold(key, values.get)(None) match {
          case Some(s) => s mapKey intr1
          case None    => D1.State.empty(intr1)
        }

      def set(k: K2, v: D1.State[SK1, K1, F]): State[SK2, K2, SK1, K1, F] = {
        val m = Dimensions.set2(intr2)(values)(k, v mergeInto _.getOrElse(D1.State.emptyA), _.isEmpty)
        new State(m, intr2, intr1)
      }

      def mod(k: K2, f: D1.State[SK1, K1, F] => D1.State[SK1, K1, F]): State[SK2, K2, SK1, K1, F] =
        set(k, f(apply(k)))

      override def mapKey2[C2](j: Intersection[K2, C2]): State[SK2, C2, SK1, K1, F] =
        new State(values, intr2 <=> j, intr1)

      override def mapKey1[C1](j: Intersection[K1, C1]): State[SK2, K2, SK1, C1, F] =
        new State(values, intr2, intr1 <=> j)

      override def iterator: Iterator[(K2, D1.State[SK1, K1, F])] =
        Dimensions.iterator(intr2.getOption, values)(_ mapKey intr1)
    }

    object State {
      type Simple[K2, K1, F] = State[K2, K2, K1, K1, F]

      def init[K2: UnivEq, K1: UnivEq, F]: State[K2, K2, K1, K1, F] =
        new State(UnivEq.emptyMap, Intersection.id[K2], Intersection.id[K1])

      sealed abstract class ReadOnly[K2, K1, +F] {
        def isEmpty: Boolean
        def apply(key: K2): D1.State.ReadOnly[K1, F]
        def mapKey2[K](f: Intersection[K2, K]): ReadOnly[K, K1, F]
        def mapKey1[K](q: Intersection[K1, K]): ReadOnly[K2, K, F]
        def iterator: Iterator[(K2, D1.State.ReadOnly[K1, F])]
      }

      implicit def reusabilityState2[K2, K1, F]: Reusability[ReadOnly[K2, K1, F]] =
        Reusability.byRef || Reusability.when(_.isEmpty)

      def at[A2, B2, A1, B1, F](k: B2): Lens[State[A2, B2, A1, B1, F], D1.State[A1, B1, F]] =
        Lens((_: State[A2, B2, A1, B1, F])(k))(o => _.set(k, o))
    }

    abstract class Feature[K2, K1, -F] {
      def apply(k2: K2): D1.Feature[K1, F]
      def mapKey1[C](j: Intersection[K1, C]): Feature[K2, C, F]
      def mapKey2[C](j: Intersection[K2, C]): Feature[C, K1, F]
      def setBulk(k2s: Iterable[K2], k1: K1, value: => D0.State[F]): Callback
    }

    object Feature {
      private final class Impl[S2, T2, K2, S1, T1, K1, -F]($: StateAccessPure[State[S2, T2, S1, T1, F]],
                                                           i2: Intersection[T2, K2],
                                                           i1: Intersection[T1, K1]) extends Feature[K2, K1, F] {
        override def apply(b: K2) =
          i2.reverse.getOption(b) match {
            case Some(a) => D1.Feature($ zoomStateL State.at(a), i1)
            case None    => D1.Feature.nop
          }

        override def mapKey1[C](j: Intersection[K1, C]) =
          new Impl($, i2, i1 <=> j)

        override def mapKey2[C](j: Intersection[K2, C]) =
          new Impl($, i2 <=> j, i1)

        override def setBulk(k2s: Iterable[K2], k1: K1, value: => D0.State[F]): Callback =
          Callback.unless(k2s.isEmpty)(
            i1.reverse.getOption(k1) match {
              case Some(t1) =>
                $.modState { s0 =>
                  val v = value
                  k2s.foldLeft(s0)((s, k2) => i2.reverse.getOption(k2) match {
                    case Some(t2) => s.mod(t2, _.set(t1, value))
                    case None     => Dimensions.warnDiscard(k2); s
                  })
                }
              case None =>
                Dimensions.warnDiscard(k1); Callback.empty
            }
          )
      }

      def apply[S2, A2, S1, A1, F]($: StateAccessPure[State[S2, A2, S1, A1, F]]): Feature[A2, A1, F] =
        new Impl($, Intersection.id[A2], Intersection.id[A1])

      implicit def reusabilityFeature[A, B, C]: Reusability[Feature[A, B, C]] =
        Reusability.byRef
    }
  }
}
