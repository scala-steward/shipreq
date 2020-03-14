package shipreq.webapp.client.project.app.pages.config.tags

import japgolly.univeq.UnivEq

final case class Buttons[+A](delete : Option[A] = None,
                             restore: Option[A] = None,
                             cancel : Option[A] = None,
                             close  : Option[A] = None,
                             save   : Option[A] = None) {

  override def toString: String = {
    var fs = Vector.empty[String]
    for (a <- delete ) fs :+= s"delete = $a"
    for (a <- restore) fs :+= s"restore = $a"
    for (a <- cancel ) fs :+= s"cancel = $a"
    for (a <- close  ) fs :+= s"close = $a"
    for (a <- save   ) fs :+= s"save = $a"
    fs.mkString("Buttons(", ", ", ")")
  }

  def map[B](f: A => B): Buttons[B] =
    Buttons(
      delete  = delete .map(f),
      restore = restore.map(f),
      cancel  = cancel .map(f),
      close   = close  .map(f),
      save    = save   .map(f),
    )
}

object Buttons {
  val none = apply()

  implicit def univEq[A: UnivEq]: UnivEq[Buttons[A]] = UnivEq.derive
}
