package shipreq.webapp.base.protocol

import shipreq.webapp.base.data._

case class InitDataForHomeSpa(username: Username,
                              projects: ProjectCatalogue)

object InitDataForHomeSpa {
  import boopickle._
  import BoopickleMacros._
  import BinCodecData._
  import BinCodecRemoteFns._

  implicit val pickler = pickleCaseClass[InitDataForHomeSpa]
}
