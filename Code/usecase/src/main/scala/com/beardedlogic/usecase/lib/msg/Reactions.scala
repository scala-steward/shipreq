package com.beardedlogic.usecase.lib.msg

/**
 * Interface through which message handlers provide reactions to messages.
 */
trait Reactor {
  def apply[R](t: ReactionType[R])(f: => R): Unit
}

/**
 * Builds a specific type reaction.
 * @tparam R The reaction data type.
 */
trait ReactionBuilder[R] {
  def reactor: Reactor
  def result: R
}

/**
 * A specific type of reaction. Examples: Javascript, JSON, XML.
 * @tparam R The reaction data type.
 */
trait ReactionType[R] {
  def unpack[A](t: ReactionType[A], f: => A)(x: R => Any): Unit = if (t == this) x(f.asInstanceOf[R])
  def unapply[A](t: ReactionType[A], f: => A): Option[R] = if (t == this) Some(f.asInstanceOf[R]) else None
}
