package shipreq.webapp.client.test

import scalaz.Equal
import scalaz.std.AllInstances._
import scalaz.effect.IO
import shipreq.webapp.base.protocol.Routine
import shipreq.webapp.client.lib.FailureIO
import shipreq.webapp.client.protocol.ClientProtocol
import shipreq.webapp.client.test.TestUtil._

object TestClientProtocol {
  trait Req {
    type D <: Routine.Desc
    val r: Routine.Remote[D]
    val i: r.d.I
    val s: r.d.O => IO[Unit]
    val f: FailureIO

    def force[D2 <: Routine.Desc](r2: Routine.Remote[D2]) =
      this.asInstanceOf[Req {type D = D2; val r: r2.type}]
  }
}

import TestClientProtocol.Req

class TestClientProtocol extends ClientProtocol {
  var reqs = Vector.empty[Req]

  def reset(): Unit =
    reqs = Vector.empty

  override def call[_D <: Routine.Desc](_r: Routine.Remote[_D])(_i: _r.d.I, _s: _r.d.O => IO[Unit], _f: FailureIO): IO[Unit] = {
    //println(s"RPC: ${_r.d}(${_r.n}) ← ${_i}")
    IO {
      reqs :+= new Req {
        override type D = _D
        override val r: _r.type = _r
        override val i = _i
        override val s = _s
        override val f = _f
      }
    }
  }

  def assertReqsSent(count: Int): Unit =
    assertEq("AJAX requests", reqs.size, count)

  def last = reqs.last

  def respondToLast[D <: Routine.Desc](r: Routine.Remote[D])(o: r.d.O): Unit =
    last.force(r).s(o).unsafePerformIO()

  def failLast(): Unit =
    last.f.io.unsafePerformIO()

  def lastTwo[D <: Routine.Desc](r: Routine.Remote[D]) = {
    val Vector(a, b) = reqs.takeRight(2).map(_.force(r))
    (a, b)
  }

  def assertLastTwoRequestsEqual[D <: Routine.Desc](r: Routine.Remote[D])(implicit e: Equal[r.d.I]): Unit = {
    val (a, b) = lastTwo(r)
    assertEq("Last two requests", a.i, b.i)
  }
}
