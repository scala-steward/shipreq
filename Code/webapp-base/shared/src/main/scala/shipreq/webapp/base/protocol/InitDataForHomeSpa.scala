package shipreq.webapp.base.protocol

import shipreq.webapp.base.data._
import BinCodecGeneric._
import BinCodecData._

object CreateProjectFn extends (String =>|=> ProjectCatalogue.Item)

case class InitDataForHomeSpa(username     : Username,
                              projects     : ProjectCatalogue,
                              createProject: CreateProjectFn.Instance)

object InitDataForHomeSpa {
  import boopickle._
  import BoopickleMacros._
  import BinCodecData._
  import BinCodecRemoteFns._

  implicit val pickler = pickleCaseClass[InitDataForHomeSpa]
}
