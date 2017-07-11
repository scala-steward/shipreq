package shipreq.webapp.client.public.spa

import monocle.macros.Lenses
import shipreq.webapp.client.public.pages._

@Lenses
final case class State(landingPage: LandingPage.State,
                       login      : Login      .State)

object State {
  def init: State =
    State(
      LandingPage.State.init,
      Login      .State.init)
}