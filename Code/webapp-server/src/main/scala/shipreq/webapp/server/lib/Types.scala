package shipreq.webapp.server.lib

import net.liftweb.http.js.{JsCmd, JsCmds}
import shipreq.taskman.api.UserId
import scalaz.Monoid
import shipreq.base.util.TaggedTypes._
import shipreq.webapp.server.db._
import shipreq.webapp.server.feature.ExternalId

/**
 * @since 30/05/2013
 */
object Types {

  // -------------------------------------------------------------------------------------------------------------------
  // String tags

  /** Marks a string as being an ISO-8601 representation of a datetime. */
  final case class ISO8601(value: String) extends TaggedString
  implicit object ISO8601 extends TaggedTypeCtor[ISO8601]

  /** Marks a password as being hashed. */
  final case class HashedStr(value: String) extends TaggedString
  implicit object HashedStr extends TaggedTypeCtor[HashedStr]

  final case class ShareUrlToken(value: String) extends TaggedString
  implicit object ShareUrlToken extends TaggedTypeCtor[ShareUrlToken]

  final case class Username(value: String) extends TaggedString
  implicit object Username extends TaggedTypeCtor[Username]

  // -------------------------------------------------------------------------------------------------------------------
  // Long tags

  /** Marks a Long value as corresponding to `usr.id`. */
  @inline final implicit def UserToId1(a: UserDescriptor): UserId = a.id
  @inline final implicit def UserToId2(a: UserRegistrationInfo): UserId = a.id

  // -------------------------------------------------------------------------------------------------------------------
  // Externalisable ID tags

  sealed trait ExteralisableId extends TaggedLong {
    type E <: TaggedString
  }

//  final case class X(value: Long) extends ExteralisableId
//  implicit object X extends TaggedTypeCtor[X]

  /** Marks a Long value as corresponding to `project.id`. */
  final case class ProjectIdE(value: String) extends TaggedString
  final case class ProjectId(value: Long) extends ExteralisableId {
    override type E = ProjectIdE
  }
  implicit object ProjectId extends TaggedTypeCtor[ProjectId]
  implicit object ProjectIdE extends TaggedTypeCtor[ProjectIdE]
  @inline final implicit def p2pid(p: Project): ProjectId = p.id

  object AutoExternaliseIds {
    implicit def aei_P(id: ProjectId): ProjectIdE = ExternalId.Project(id)
  }

  // ===================================================================================================================
  // Type class instances

  implicit object JsCmdMonoid extends Monoid[JsCmd] {

    import JsCmds.{_Noop => Noop}

    override def zero = Noop
    override def append(a: JsCmd, b: => JsCmd) =
      if (a eq Noop) b
      else if (b eq Noop) a
      else a & b
  }
}
