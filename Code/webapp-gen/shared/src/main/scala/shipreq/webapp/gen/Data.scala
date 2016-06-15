package shipreq.webapp.gen

import java.time.Instant
import shipreq.webapp.base.data._

object Data {

  def username: MainAndTest[Username] =
    MainAndTest(Username("USERNAME"), Vector(Username("YXXusernameXYX")))

  def projectCatalogueItems: Vector[ProjectCatalogue.Item] =
    Vector(
      ProjectCatalogue.Item(
        ExternalId("QWE123ZXC987"),
        "Proj<ect/ \"nam>e\"!",
        123456789,
        666,
        Instant ofEpochMilli 1462000000000L, // 2016-04-30T07:06:40Z
        Some(Instant ofEpochMilli 1462012345678L))) // 2016-04-30T10:32:25.678Z

  type ProjectSpaLoader = (Username, ProjectCatalogue.Item)

  def projectSpaLoaderData: MainAndTest[ProjectSpaLoader] = {
    val main = (username.main,
      ProjectCatalogue.Item(ExternalId("XXXidXXX"), "XXX projectname XXX", 123456701, 123456702, null, None))

    val tests = for {u <- username.tests; i <- projectCatalogueItems} yield (u, i)

    MainAndTest(main, tests)
  }
}

