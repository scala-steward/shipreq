package com.beardedlogic.usecase.util

import com.beardedlogic.usecase.lib.Message

/** Listens and reacts to messages. */
trait MessageListener {
  def messageHandler(reactor: Reactor): PartialFunction[Message, Unit]
}

/**
 * A hub for synchronous message traffic between decoupled components, with dynamic yet specialised reactions
 * (ie. results).
 *
 * This is initially disabled, during which messages are discarded rather than broadcast.
 *
 * Also baked-in, is a special feature that routes all CometMsgs to a provided CometActor. This could have easily be
 * done with existing functionality but the volume of CometMsgs and the assurance that only one listener will be
 * interested warranted the minor performance enhancement.
 *
 * @since 11/05/2013
 */
class MessageCentre {

  @volatile private[this] var listeners: List[MessageListener] = Nil
  @volatile var enabled: Boolean = false

  /**
   * Registers an actor so that it receives a copy of all messages that pass through.
   */
  def register(listener: MessageListener) {
    listeners :+= listener
  }

  /**
   * Removes a subscribed actor so that it no longer receives messages from here.
   */
  def unregister(listener: MessageListener) {
    listeners = listeners.filter(_ ne listener)
  }

  def send[R](msg: Message, rb: ReactionBuilder[R]): R = {
    sendMessage(msg, rb.reactor)
    rb.result
  }

  @inline final def sendMessage(msg: Message, reactor: Reactor): Unit = this.!(msg)(reactor)

  def !(msg: Message)(implicit reactor: Reactor): Unit = {
    if (enabled && reactor != NoReactionOrNewMessages) {
      // TODO Benchmark and determine penalty for reconstructing PF on every call (bc 'react' is a param)
      // TODO Benchmark and determine which is faster: isDefinedAt() + apply() or catching MatchError
      listeners foreach { l =>
        val pf = l.messageHandler(reactor)
        if (pf.isDefinedAt(msg)) pf.apply(msg)
      }
    }
  }
}