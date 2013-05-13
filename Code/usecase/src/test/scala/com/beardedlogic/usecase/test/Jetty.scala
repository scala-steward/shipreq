package com.beardedlogic.usecase.test

import net.liftweb.util.TimeHelpers._
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.webapp.WebAppContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * Starts up an instance of Jetty than runs the webapp.
 *
 * @since 29/04/2013
 */
object Jetty {

  private val jetty = new JettyInstance

  def start() {
    jetty.start
  }

  def stop() {
    jetty.stop
  }

  val PORT = 8090
  val MAX_IDLE = 10 seconds
  val URL = "http://localhost:" + PORT

  def newServer = {
    val svr = new Server

    val connector = new SelectChannelConnector
    connector.setPort(PORT)
    connector.setMaxIdleTime(MAX_IDLE.millis.toInt)
    connector.setServer(svr)
    svr.setConnectors(Array(connector));

    val context = new WebAppContext
    context.setContextPath("/")
    context.setWar("src/main/webapp")
    // context.setClassLoader(Thread.currentThread().getContextClassLoader());
    // context.setDescriptor("src/main/webapp/WEB-INF/web.xml")
    // context.setResourceBase("src/main/webapp")
    svr.setHandler(context)

    context.setServer(svr)
    svr
  }
}

private class JettyInstance {

  import Jetty._

  private val refCount = new AtomicInteger(0)
  private val serverLock = new Object()
  private var server: Server = null

  def start() {
    if (refCount.getAndIncrement == 0) {
      serverLock.synchronized {
        // println("Starting Jetty...")
        server = newServer
        server.start
      }
    }
  }

  def stop() {
    new Thread(new Runnable {
      def run() {
        Thread.sleep(1000)
        if (refCount.decrementAndGet == 0) {
          serverLock.synchronized {
            // println("Stopping Jetty...")
            server.stop
            server.join
            server = null
          }
        }
      }
    }).start()
  }
}