package shipreq.webapp.client.public.protocol

import shipreq.webapp.base.protocol._
import BoopickleMacros._

/**
  * Protocols for the Public SPA / webapp-client-public module.
  */
object PublicSpaProtocols {

  final case class InitData(landingPage: LandingPageProtocol.Fn.Instance)

  import LandingPageProtocol.Fn.{pickleInstance => _i1}
  implicit val picklerInitData = pickleCaseClass[InitData]

  final val EntryPointName = "A"
  val EntryPoint = ClientSideProc[InitData](EntryPointName)
}
