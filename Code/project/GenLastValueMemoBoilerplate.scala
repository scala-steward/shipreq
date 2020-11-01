import sbt._

object GenLastValueMemoBoilerplate {

  def apply(outputDir: File): File = {

    val groups =
      (2 to 22).map { n =>
        def ns(t: String) = (1 to n).map(i => t.replace("1", i.toString)).mkString(", ")
        s"""
           |  final def apply$n[${ns("A1, B1")}, Z]
           |      (${ns("c1: LastValueMemo[A1, B1]")})
           |      (f: (${ns("B1")}) => Z): LastValueMemo[(${ns("A1")}), Z] = {
           |    type A = (${ns("A1")})
           |    implicit val reusability: Reusability[A] =
           |      Reusability.tuple$n(${ns("c1.reusability")})
           |    apply[A, Z](a => f(${ns("c1(a._1)")}))
           |  }
         """.stripMargin.trim.replaceFirst("^", "  ")
      }

    val Name = "LastValueMemoBoilerplate"

    val sep = s"\n  // ${"=" * 115}\n\n"

    val content =
      s"""
         |package shipreq.webapp.base.util
         |
         |import japgolly.scalajs.react.Reusability
         |
         |abstract class $Name private[util]() {
         |
         |  def apply[A, B](f: A => B)(implicit r: Reusability[A]): LastValueMemo[A, B]
         |$sep${groups.mkString("\n" + sep)}
         |}
        """.stripMargin.trim

    val file = (outputDir / "shipreq" / "webapp" / "base" / "util" / s"$Name.scala").asFile
    IO.write(file, content)
    println(s"Generated ${file.getAbsolutePath}")
    file
  }
}