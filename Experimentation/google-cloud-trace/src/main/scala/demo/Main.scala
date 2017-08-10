package demo

import com.google.cloud.trace.Trace
import com.google.cloud.trace.core.{ConstantTraceOptionsFactory, EndSpanOptions, Labels, StartSpanOptions}
import com.google.cloud.trace.service.TraceGrpcApiService
import scala.annotation.tailrec

object Main {

  def main(args: Array[String]): Unit = {
    println("Starting...")

    val traceService = TraceGrpcApiService.builder()
      .setProjectId("shipreq-dev")
      .setScheduledDelay(1)
      // FINALLY FOUND IT! Default rate limits to 1 sample/sec
      .setTraceOptionsFactory(new ConstantTraceOptionsFactory(true, true))
      .build()
    println(s"traceService = $traceService")

    Trace.init(traceService)
    println("Trace.init(traceService) OK")

    /*
    {
      val tracer = Trace.getTracer()
      println(s"tracer = $tracer")
      println("SpanContextHandler = " + Trace.getSpanContextHandler.current())

      val span1 = tracer.startSpan("span-1")
      println("SpanContextHandler = " + Trace.getSpanContextHandler.current())
      Thread.sleep(100)
      val span2 = tracer.startSpan("span-2"); Thread.sleep(100); tracer.endSpan(span2)
      val span3 = tracer.startSpan("span-3"); Thread.sleep(60); tracer.endSpan(span3)
      tracer.endSpan(span1)
    }

    Thread.sleep(20)

    {
      println("SpanContextHandler = " + Trace.getSpanContextHandler.current())
      val tracer = Trace.getTracer()
      println(s"tracer = $tracer")
      println("SpanContextHandler = " + Trace.getSpanContextHandler.current())

      val span4o = new StartSpanOptions().setEnableTrace(true)
      val span4 = tracer.startSpan("span-4", span4o)
      println("SpanContextHandler = " + Trace.getSpanContextHandler.current())
      Thread.sleep(600)
      tracer.endSpan(span4)
    }

    println("Waiting...")
    Thread.sleep(2000)

    {
      println("SpanContextHandler = " + Trace.getSpanContextHandler.current())
      val tracer = Trace.getTracer()
      println(s"tracer = $tracer")
      println("SpanContextHandler = " + Trace.getSpanContextHandler.current())

      val span5 = tracer.startSpan("span-5")
      println("SpanContextHandler = " + Trace.getSpanContextHandler.current())
      Thread.sleep(80)
      tracer.endSpan(span5)
    }
    */

    val tracer = Trace.getTracer()
    @tailrec def go(start: Long, i: Int): Unit =
      if (System.currentTimeMillis() - start <= 500) {
        val name = "loop-a-" + i
        println(name)
        val ctx = tracer.startSpan(name)
        Thread.sleep(300)
        tracer.endSpan(ctx)
        tracer.annotateSpan(ctx, Labels.builder().add("http/status_code", "200").add("http/method", "GET").build())
        Thread.sleep(10)
        go(start, i + 1)
      }
    go(System.currentTimeMillis(), 1)
/*
    for (i <- 1 to 4) {

      val ctx = tracer.startSpan("example-" + i)
      println(Trace.getSpanContextHandler.current.getTraceId)
      tracer.endSpan(ctx)
    }

    println("Waiting...")
    Thread.sleep(2000)

    for (i <- 5 to 8) {
      val tracer = Trace.getTracer()
      val ctx = tracer.startSpan("example-" + i)
      println(Trace.getSpanContextHandler.current.getTraceId)
      tracer.endSpan(ctx)
      tracer.annotateSpan(ctx, Labels.builder().add("http/status_code", "200").build())
    }

    println("Waiting...")
    Thread.sleep(8000)
    */

    println("Done.")
  }
}