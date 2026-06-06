package shipreq.webapp.member.protocol.entrypoint

import cats.Eq
import japgolly.microlibs.cats_ext.CatsMacros
import shipreq.webapp.base.protocol.Version
import shipreq.webapp.base.protocol.binary.SafePickler
import shipreq.webapp.base.test.BinaryTestUtil._
import shipreq.webapp.member.test.project.RandomData
import utest._

object ProjectSpaEntryPointTest extends TestSuite {

  private implicit val equalProjectSpaInitPageData: Eq[ProjectSpaEntryPoint.InitData] =
    CatsMacros.deriveEq

  override def tests = Tests {
    "roundTrip" - {
      val protocol = SafePickler.of(Version.v1(0), _ => ProjectSpaEntryPoint.picklerInitData)
      propTestRoundTrip(protocol)(RandomData.routines.projectSpaInitPageData)
    }
  }
}
