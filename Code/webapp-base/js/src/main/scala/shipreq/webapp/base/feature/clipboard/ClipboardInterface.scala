package shipreq.webapp.base.feature.clipboard

/** A protocol to read from, and write to, the clipboard for a specific data type. */
final class ClipboardInterface[A](val write: A => ClipboardData,
                                  val read: ClipboardData => Option[A]) {

  def xmap[B](r: A => B)(w: B => A): ClipboardInterface[B] =
    ClipboardInterface(read.andThen(_.map(r)))(write compose w)

  def correct(f: A => A): ClipboardInterface[A] =
    xmap(f)(f)

  def readOrUse(cd: Option[ClipboardData], use: => A): A =
    cd.flatMap(read).getOrElse(use)
}

object ClipboardInterface {

  def apply[A](read: ClipboardData => Option[A])
              (write: A => ClipboardData): ClipboardInterface[A] =
    new ClipboardInterface(write, read)

  def total[A](read: ClipboardData => A)
              (write: A => ClipboardData): ClipboardInterface[A] =
    apply(read.andThen(Some(_)))(write)

  lazy val string: ClipboardInterface[String] =
    total(_.text)(ClipboardData.apply)
}