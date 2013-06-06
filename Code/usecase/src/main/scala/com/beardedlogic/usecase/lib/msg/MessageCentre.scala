package com.beardedlogic.usecase.lib.msg

/** Listens and reacts to messages. */
trait MessageListener {
  def messageHandler: PartialFunction[(Message, Reactor), Unit]
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

  def send[R](msg: Message, r: ReactionBuilder[R]): R = {
    this ! ((msg, r.reactor))
    r.result
  }

  def !(msgAndReactor: (Message, Reactor)): Unit = {
    if (enabled) {
      listeners foreach { l =>
        val pf = l.messageHandler
        if (pf.isDefinedAt(msgAndReactor)) pf.apply(msgAndReactor)
      }
    }
  }
}