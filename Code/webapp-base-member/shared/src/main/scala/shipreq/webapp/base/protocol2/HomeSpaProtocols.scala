package shipreq.webapp.base.protocol2

import shipreq.webapp.base.data._
import shipreq.webapp.base.protocol.BoopickleMacros._
import shipreq.webapp.base.user._
import shipreq.webapp.base.protocol.{ClientSideProc => _, _}
import BinCodecGeneric._
import BinCodecUser._
import BinCodecMemberData._

/**
  * Protocols for the Home SPA / webapp-client-home module.
  */
object HomeSpaProtocols {

  final case class InitData(username: Username,
                            projects: List[ProjectMetaData])

  implicit val picklerInitData = pickleCaseClass[InitData]

  final val EntryPointName = "H"
  val EntryPoint = ClientSideProc[InitData](EntryPointName)

  val createProject = MemberProtocols.createProject
}