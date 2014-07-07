package golly

import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom.{document, console, window}

import japgolly.scalajs.react._
import vdom.ReactVDom._
import all._

import scala.collection.immutable.SortedSet
import scalaz.{LensFamily, Lens}

object ReactExamples {

  implicit final class ComponentScope_SS_Ext2[State](val u: ComponentScope_SS[State]) extends AnyVal {
    @inline def setStateL[V](l: LensFamily[State, State, _, V])(v: V) = u.modState(l.set(_, v))
  }

  def textChangeRecv(f: String => Unit): SyntheticEvent[dom.HTMLInputElement] => Unit = e => f(e.target.value)
  def textChangeRecvL[State](t: ComponentScopeB[_, State], l: Lens[State, String]) = textChangeRecv(t setStateL l)

  object Sample4 {

    case class State(people: SortedSet[String], text: String, focusPerson: Option[String])
    val stateTextL = Lens.lensg[State, String](a => b => a.copy(text = b, focusPerson = None), _.text)

    class PeopleListBackend(t: ComponentScopeB[Unit, State]) {
      def delete(name: String): Unit = {
        val p = t.state.people
        if (p.contains(name))
          t.setState(State(p - name, name, None))
      }

      val onChange = textChangeRecvL(t, stateTextL)

      val onKP: SyntheticEvent[dom.HTMLInputElement] => Unit =
        e => if (e.keyboardEvent.keyCode == 13) {
            e.preventDefault()
            add()
          }

      def add(): Unit = t.setState(State(t.state.people + t.state.text, "", Some(t.state.text)))
    }

    case class PeopleListProps(people: SortedSet[String], latest: Option[String], deleteFn: String => Unit)

    val PeopleList = {
      val focusNext = Ref[dom.HTMLInputElement]("latest")

      ReactComponentB[PeopleListProps]("PeopleList")
        .render(P =>
          if (P.people.isEmpty)
            div(color := "#800")("No people in your list!!").render
          else
            ol(P.people.toList.map(p =>
              li(
                input(value := p, (P.latest contains p) && (ref := focusNext))(),
                button(marginLeft := 1.em, onclick runs P.deleteFn(p))("Delete"))
            )).render
          )
          .componentDidUpdate((t,_,_) => focusNext(t).tryFocus())
          .componentDidMount(t => focusNext(t).tryFocus())
          .create
    }

    val PeopleEditor = ReactComponentB[Unit]("PeopleEditor")
      .getInitialState(_ => State(SortedSet("First","Second", "x"), "Middle", Some("Second")))
      .backend(new PeopleListBackend(_))
      .render((_,S,B) =>
          div(
            h3("People List")
            ,div(PeopleList(PeopleListProps(S.people, S.focusPerson, B.delete)))
            ,h3("Add")
            ,input(onchange ==> B.onChange, onkeypress ==> B.onKP, value := S.text)()
            ,button(onclick runs B.add())("+")
          ).render
      )
      .create

    def apply(): Unit =
      React.renderComponent(PeopleEditor(()), document getElementById "target2")
  }
}
