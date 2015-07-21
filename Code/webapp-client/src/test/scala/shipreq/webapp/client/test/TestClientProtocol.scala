package shipreq.webapp.client.test

import scalaz.{-\/, Equal}
import scalaz.std.AllInstances._
import scalaz.effect.IO
import shipreq.webapp.base.protocol.RemoteFn
import shipreq.webapp.client.lib.{SuccessIO, FailureIO}
import shipreq.webapp.client.protocol.ClientProtocol
import shipreq.webapp.client.protocol.ClientProtocol.Failed
import shipreq.webapp.client.test.TestUtil._

object TestClientProtocol {
  trait Req {
    val r      : RemoteFn.Instance
    val input  : r.fn.Input
    val success: r.fn.Output => SuccessIO
    val failure: Failed[r.fn.Failure] => FailureIO

    def force(r2: RemoteFn.Instance) =
      this.asInstanceOf[Req {val r: r2.type}]
  }
}

import TestClientProtocol.Req

class TestClientProtocol extends ClientProtocol {
  var reqs = Vector.empty[Req]

  def reset(): Unit =
    reqs = Vector.empty

  def call(i: RemoteFn.Instance)(_input  : i.fn.Input,
                                 _success: i.fn.Output => SuccessIO,
                                 _failure: Failed[i.fn.Failure] => FailureIO): IO[Unit] = {
    //println(s"RPC: ${_r.d}(${_r.n}) ← ${_i}")
    IO {
      reqs :+= new Req {
        override val r: i.type = i
        override val input   = _input
        override val success = _success
        override val failure = _failure
      }
    }
  }

  def assertReqsSent(count: Int): Unit =
    assertEq("AJAX requests", reqs.size, count)

  def last = reqs.last

  def respondToLast(r: RemoteFn.Instance)(o: r.fn.Output): Unit =
    last.force(r).success(o).io.unsafePerformIO()

  def failLast(): Unit =
    last.failure(-\/(new Throwable("dummy error"))).io.unsafePerformIO()

  def lastTwo(r: RemoteFn.Instance) = {
    val Vector(a, b) = reqs.takeRight(2).map(_.force(r))
    (a, b)
  }

  def assertLastTwoRequestsEqual(r: RemoteFn.Instance)(implicit e: Equal[r.fn.Input]): Unit = {
    val (a, b) = lastTwo(r)
    assertEq("Last two requests", a.input, b.input)
  }
}
