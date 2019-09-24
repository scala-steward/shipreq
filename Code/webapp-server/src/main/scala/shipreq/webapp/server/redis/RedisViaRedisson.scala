package shipreq.webapp.server.redis

import com.typesafe.scalalogging.StrictLogging
import java.lang.{Boolean => JBool}
import java.util.{ArrayList, Collections, List => JList}
import org.redisson.api.RScript.Mode
import org.redisson.api.listener.MessageListener
import org.redisson.api.{RScript, RedissonClient}
import org.redisson.client.codec.{ByteArrayCodec, StringCodec}
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.control.NonFatal
import scalaz.\/-
import scalaz.std.option._
import scalaz.syntax.traverse._
import shipreq.base.util.FxModule._
import shipreq.webapp.base.data.ProjectId
import shipreq.webapp.base.event.{EventOrd, VerifiedEvent}
import shipreq.webapp.base.protocol.binary.SafePickler
import shipreq.webapp.server.logic.Redis
import shipreq.webapp.server.logic.RedisProtocol._
import shipreq.webapp.server.logic.Redis.ProjectSnapshot

object RedisViaRedisson {

  private[RedisViaRedisson] object Internals {

    def newArgs() = new Args(new mutable.ArrayBuffer[Array[Byte]])

    final class Args(val args: mutable.ArrayBuffer[Array[Byte]]) extends AnyVal {
      @inline def +=(s: String         ): Unit = args += s.getBytes
      @inline def +=(i: Int            ): Unit = args += i.toString.getBytes
      @inline def +=(a: Array[Byte]    ): Unit = args += a
      @inline def +=(e: VerifiedEvent  ): Unit = args += picklerEvent.encode(e).unsafeArray
      @inline def +=(s: ProjectSnapshot): Unit = args += picklerProjectSnapshot.encode(s).unsafeArray
      @inline def ++=(es: VerifiedEvent.Seq): Unit = es.foreach(+=(_))
    }
  }
}

final class RedisViaRedisson(client: RedissonClient, schema: RedisSchema) extends Redis.ProjectAlgebra[Fx] with StrictLogging {
  import Redis._
  import RedisViaRedisson.Internals._

  override protected def F = fxInstance

  private val byteArrayClass = classOf[Array[Byte]]

  private def deployScript(lua: Lua): String =
    client.getScript(StringCodec.INSTANCE).scriptLoad(lua.processed)

  private val scriptBinary =
    client.getScript(ByteArrayCodec.INSTANCE)

  private val noKeys: JList[AnyRef] =
    Collections.unmodifiableList(Collections.emptyList())

  override def subscribe(id: ProjectId, listener: SafePickler.Result[VerifiedEvent] => Fx[Unit]): Fx[Subscription[Fx]] = {
    val topicName = schema.topic(id)

    val msgListener: MessageListener[Array[Byte]] = (_, msg) => {
      val decoded = picklerEvent.decodeBytes(msg)
      val fx      = listener(decoded)
      fx.unsafeRun()
    }

    Fx[Subscription[Fx]] {
      val topicRef = client.getTopic(topicName, ByteArrayCodec.INSTANCE)
      val listenerId = topicRef.addListener(byteArrayClass, msgListener)
      val unsub = Fx[Unit] {
        topicRef.removeListener(listenerId)
      }
      Subscription[Fx](unsub)
    }
  }

//  private def logResult(a: Any) =
//    a match {
//      case l: JList[_] =>
//        println("-"*120)
//        println(s"RESULT: [${a.getClass}] ${l.size()} elements:")
//        l.asScala.map {
//          case r: Array[Byte] => s"${BinaryData.unsafeFromArray(r)} -- ${r.map(_.toChar).mkString}"
//          case a => s"[${if (a == null) "null" else a.getClass.getCanonicalName}] $a"
//        }.map("* " + _).foreach(println)
//        println("-"*120)
//      case r: Array[Byte] => println("RESULT: " + BinaryData.unsafeFromArray(r))
//      case _ => println(s"RESULT: [${if (a == null) "null" else a.getClass.getCanonicalName}] ${a}")
//    }

  private def fxWithFallback[@specialized(Boolean) A](name: String, default: A)(body: => A): Fx[A] =
    Fx {
      try
        body
      catch {
        case NonFatal(t) =>
          logger.warn(s"Exception during $name: $t", t)
          default
      }
    }

  private[this] val emptyEventResult = SafePickler.success(VerifiedEvent.Seq.empty)

  private def decodeEvents(raw: JList[Array[Byte]]): SafePickler.Result[VerifiedEvent.Seq] =
    raw.asScala
      .iterator
      .map(picklerEvent.decodeBytes)
      .foldLeft(emptyEventResult)((q, r) => q.flatMap(e1 => r.map(e2 => e1 + e2)))

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  private val shaRead = deployScript(Lua.read)

  private[this] val emptyCacheResult = SafePickler.success(ProjectCache.empty)

  override protected def _read(id: ProjectId) =
    fxWithFallback("read", emptyCacheResult) {

      // Call Redis
      val keys = new ArrayList[AnyRef](2)
      keys.add(schema.snapshot(id))
      keys.add(schema.events(id))
      val result = scriptBinary.evalSha[JList[_]](Mode.READ_ONLY, shaRead, RScript.ReturnType.MULTI, keys)

      // Parse results
      val binSS = Option(result.get(0).asInstanceOf[Array[Byte]])
      val binES = result.get(1).asInstanceOf[JList[Array[Byte]]]
      val decSS = binSS.traverse(picklerProjectSnapshot.decodeBytes)
      val decES = decodeEvents(binES)

      // Done
      for {
        ss <- decSS
        es <- decES
      } yield ProjectCache(ss, es)
    }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  private val shaReadEvents = deployScript(Lua.readEvents)

  override def readEvents(id: ProjectId, beyond: Option[EventOrd.Latest]) =
    fxWithFallback("readEvents", emptyEventResult) {

      val keys = new ArrayList[AnyRef](1)
      keys.add(schema.events(id))

      val args = newArgs()
      args += beyond.fold(0)(_.value)

      val result = scriptBinary.evalSha[JList[Array[Byte]]](Mode.READ_ONLY, shaReadEvents, RScript.ReturnType.MULTI, keys, args.args: _*)

      decodeEvents(result)
    }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  private val shaWriteSnapshot = deployScript(Lua.writeSnapshot)

  override def writeSnapshot(id: ProjectId, snapshot: ProjectSnapshot, publishOnly: VerifiedEvent.Seq) =
    fxWithFallback("writeSnapshot", true) {

      val keys = new ArrayList[AnyRef](2)
      keys.add(schema.snapshot(id))
      keys.add(schema.events(id))

      val args = newArgs()
      args += schema.topic(id)
      args += snapshot.ord.value
      args += snapshot
      args ++= publishOnly

      scriptBinary.evalSha[JBool](Mode.READ_WRITE, shaWriteSnapshot, RScript.ReturnType.BOOLEAN, keys, args.args: _*)
    }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  private val shaWriteEvents = deployScript(Lua.writeEvents)

  override def writeEvents(id: ProjectId, cacheOnly: VerifiedEvent.Seq, cacheAndPublish: VerifiedEvent.Seq) =
    if (cacheOnly.isEmpty && cacheAndPublish.isEmpty)
      Fx.pure(false)
    else fxWithFallback("writeEvents", true) {

      val shouldPublish: VerifiedEvent => Boolean =
        if (cacheOnly.isEmpty)
          _ => true
        else if (cacheAndPublish.isEmpty)
          _ => false
        else {
          val pubOrds = cacheAndPublish.iterator.map(_.ord).toSet
          e => pubOrds.contains(e.ord)
        }

      val keys = new ArrayList[AnyRef](2)
      keys.add(schema.snapshot(id))
      keys.add(schema.events(id))

      val args = newArgs()
      args += schema.topic(id)
      for (e <- cacheOnly ++ cacheAndPublish) {
        val key = if (shouldPublish(e)) -e.ord.value else e.ord.value
        args += key
        args += e
      }

      scriptBinary.evalSha[JBool](Mode.READ_WRITE, shaWriteEvents, RScript.ReturnType.BOOLEAN, keys, args.args: _*)
    }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  private val shaPublishEvents = deployScript(Lua.publishEvents)

  override def publishEvents(id: ProjectId, events: VerifiedEvent.NonEmptySeq) =
    fxWithFallback("publishEvents", ()) {
      // logger.info(s"Publishing project #${id.value} events ${events.describeEvents}")

      val args = newArgs()
      args += schema.topic(id)
      args ++= events

      scriptBinary.evalSha(Mode.READ_WRITE, shaPublishEvents, RScript.ReturnType.STATUS, noKeys, args.args: _*)
    }
}
