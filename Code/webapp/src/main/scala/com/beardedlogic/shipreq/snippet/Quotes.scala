package com.beardedlogic.shipreq.snippet

import com.beardedlogic.shipreq.util.{DisableCache, NeverExpire, ExpireAfter, CacheFn, NonEmptyTemplate}
import net.liftweb.util.Helpers._
import xml.{NodeSeq, Elem, Node}
import scala.util.Random
import org.joda.time.Period
import net.liftweb.common.Logger

object Quotes extends Logger {

  val quotes = {
    val template = NonEmptyTemplate.load("templates-hidden/quotes").get
    val process = ".off" #> "" & "blockquote [class+]" #> "rndquote"
    def collect(t: NodeSeq) = t.head.child.toList.collect {case e: Elem => e}
    val countAll = collect(template).size
    val used = collect(process(template))
    info(s"Quotes available: ${used.size}/$countAll")
    used
  }

  private val rng = new Random
  private var nextQuotes: List[Node] = Nil

  def nextQuote(): Node = rng.synchronized {
    if (nextQuotes.isEmpty)
      nextQuotes = rng.shuffle(quotes)
    val h :: t = nextQuotes
    nextQuotes = t
    h
  }

  def renderFn = {
    val q = nextQuote
    "*" #> q
  }

  val rcache = CacheFn(renderFn)(ExpireAfter(Period minutes 30))

  def render = rcache.value
}
