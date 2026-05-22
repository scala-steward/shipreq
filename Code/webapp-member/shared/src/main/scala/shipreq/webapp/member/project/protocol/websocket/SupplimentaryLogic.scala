package shipreq.webapp.member.project.protocol.websocket

import cats.Monad
import cats.syntax.functor._
import shipreq.webapp.base.data.{Rolodex, UserId, Username}
import shipreq.webapp.member.project.event.{Event, VerifiedEvent}
import shipreq.webapp.member.project.protocol.websocket.ProjectSpaProtocols.Supplimentary

/** Logic to obtain necessary supplimentary data required when certain events are received. */
object SupplimentaryLogic {

  type Fn[F[_]] = VerifiedEvent.Seq => F[ProjectSpaProtocols.Supplimentary]

  def apply[F[_]](needUsernamesByUserId: Set[UserId] => F[Map[UserId, Username]])
                 (implicit F: Monad[F]): Fn[F] = {
    val empty = F.pure(Supplimentary.empty)

    events =>
      if (events.isEmpty)
        empty
      else {
        var newUserIds = Set.empty[UserId]

        for (ve <- events)
          ve.event match {

            case e: Event.AccessUpdate =>
              e.newRole match {
                case Some(_) => newUserIds += e.userId
                case _       =>
              }

            case _ =>
          }

        for {
          usernames <- needUsernamesByUserId(newUserIds)
        } yield {
          val rolodex = Rolodex(usernames)
          Supplimentary(rolodex)
        }
      }
  }

}
