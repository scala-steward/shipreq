package shipreq.webapp.base.protocol

import BinCodecGeneric._

/**
  * Protocols for the Public SPA / webapp-client-public module.
  */
object PublicSpaProtocols {

  final val EntryPointName = "A"
  val EntryPoint = ClientSideProc[Unit](EntryPointName)
}
