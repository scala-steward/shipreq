package shipreq.webapp.base.protocol

import BinCodecGeneric._

/**
  * Protocols for the Public SPA / webapp-client-public module.
  */
object PublicSpaProtocols {

  type InitData = Unit

  final val EntryPointName = "A"
  val EntryPoint = ClientSideProc[InitData](EntryPointName)
}
