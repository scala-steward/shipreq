package shipreq.base.util.diff

final case class DiffEqual[A](eql: (A, A) => Boolean) extends AnyVal

object DiffEqual {

  def byUnivEq[@specialized A: UnivEq]: DiffEqual[A] =
    DiffEqual(_ == _)
}