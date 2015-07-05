package shipreq.webapp.base.text

import shipreq.base.util.NonEmptyVector

trait AtomTC[TC[_]] {

  def lazily[A](a: => TC[A]): TC[A]

  def vec[A](implicit a: TC[A]): TC[Vector[A]]
  def nev[A](as: TC[Vector[A]])(implicit a: TC[A]): TC[NonEmptyVector[A]]

  def sum[T <: Atom.Base](t: T)(f: t.Atom => TC[t.Atom], all: Vector[TC[t.Atom]]): TC[t.Atom]

  def blankLine    [T <: Atom.NewLine        ](t: T): TC[t.BlankLine   ]
  def literal      [T <: Atom.Literal        ](t: T): TC[t.Literal     ]
  def webAddress   [T <: Atom.PlainTextMarkup](t: T): TC[t.WebAddress  ]
  def emailAddress [T <: Atom.PlainTextMarkup](t: T): TC[t.EmailAddress]
  def mathTeX      [T <: Atom.PlainTextMarkup](t: T): TC[t.MathTeX     ]
  def reqRef       [T <: Atom.ReqRef         ](t: T): TC[t.ReqRef      ]
  def codeRef      [T <: Atom.ReqRef         ](t: T): TC[t.CodeRef     ]
  def tagRef       [T <: Atom.TagRef         ](t: T): TC[t.TagRef      ]

  def issue        [T <: Atom.Issue     ](t: T)(implicit x: TC[Text.InlineIssueDesc.OptionalText]): TC[t.Issue]
  def unorderedList[T <: Atom.ListMarkup](t: T)(implicit x: TC[NonEmptyVector[t.ListItem]])       : TC[t.UnorderedList]

  final val instances = TextTC[TC](this)
}

object TextTC {
  def apply[TC[_]](a: AtomTC[TC]): TextTC[TC] =
    new TextTC(a)

  private[TextTC] trait Instance[F[_], G[_], A] {
    type T <: A
    val f: F[T]
    val g: G[T]
    def genF = f.asInstanceOf[F[A]]
  }
}

class TextTC[TC[_]](a: AtomTC[TC]) {

  private def triple[T <: Text.Generic](t: T): (TC[t.Atom], TC[t.OptionalText], TC[t.NonEmptyText]) = {
    import scala.reflect.ClassTag
    import TextTC.Instance

    type TA = t.Atom
    type Unapply[A] = TA => Option[A]

    type R  = Instance[TC, Unapply, TA]
    type R0 = Vector[R]
    type R1 = NonEmptyVector[R]

    def instance[A <: TA](tc: TC[A])(pf: PartialFunction[TA, A]): Instance[TC, Unapply, TA] =
      new Instance[TC, Unapply, TA] {
        override type T = A
        override val  f = tc
        override val  g = pf.lift
      }

    def cast[A <: Atom.Base](ta: A): t.type with A =
      ta.asInstanceOf[t.type with A]

    def slice[A <: Atom.Base: ClassTag](f: t.type with A =>  Unit): Unit =
      t match {
        case x: A => f(cast[A](x))
        case _ => ()
      }

    var as: R1 = NonEmptyVector(instance(a literal t) { case r: t.Literal => r })

    def add(r: R): Unit = as :+= r

    slice[Atom.PlainTextMarkup] { x =>
      add(instance(a webAddress x) { case r: x.WebAddress => r })
      add(instance(a emailAddress x) { case r: x.EmailAddress => r })
      add(instance(a mathTeX x) { case r: x.MathTeX => r })
    }

    slice[Atom.ReqRef] { x =>
      add(instance(a reqRef x) { case r: x.ReqRef => r })
      add(instance(a codeRef x) { case r: x.CodeRef => r })
    }

    slice[Atom.TagRef] { x =>
      add(instance(a tagRef x) { case r: x.TagRef => r })
    }

    slice[Atom.NewLine] { x =>
      add(instance(a blankLine x) { case r: x.BlankLine => r })
    }

    lazy val atom: TC[TA] = {

      slice[Atom.ListMarkup] { x =>
        implicit val li: TC[NonEmptyVector[x.ListItem]] = a lazily {
          val va = vec.asInstanceOf[TC[Vector[x.Atom]]]
           a.nev(a vec va)(va)
        }
        add(instance(a unorderedList x){case r: x.UnorderedList => r})
      }

      slice[Atom.Issue] { x =>
        add(instance(a.issue(x)(issue3._2)) { case r: x.Issue => r })
      }

      val av = as.whole

      def resolve: TA => TC[TA] = a => {
        var result: AnyRef = null
        val it = av.iterator
        while ((result eq null) && it.hasNext) {
          val i = it.next()
          if ((i g a).isDefined)
            result = i.f.asInstanceOf[AnyRef]
        }
        assert(result ne null, s"Failed to supply typeclass for $a. Should only happen in TextTC is out of sync with the Atom structures.")
        result.asInstanceOf[TC[TA]]
      }

      a.sum(t)(resolve, av.map(_.genF))
    }

    lazy val vec = a lazily (a vec atom)

    (atom, vec, a.nev(vec)(atom))
  }

  private lazy val issue3 = triple(Text.InlineIssueDesc)

  implicit val (inlineIssueDescA,   inlineIssueDescO,   inlineIssueDescN)   = issue3
  implicit val (genericReqTitleA,   genericReqTitleO,   genericReqTitleN)   = triple(Text.GenericReqTitle)
  implicit val (customTextFieldA,   customTextFieldO,   customTextFieldN)   = triple(Text.CustomTextField)
  implicit val (reqCodeGroupTitleA, reqCodeGroupTitleO, reqCodeGroupTitleN) = triple(Text.ReqCodeGroupTitle)
}
