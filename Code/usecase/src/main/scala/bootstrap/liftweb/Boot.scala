package bootstrap.liftweb

import net.liftweb.http.{ Html5Properties, LiftRules, Req }
import net.liftweb.sitemap.{ Menu, SiteMap }
import com.beardedlogic.usecase.lib
import lib.db.DB
import lib.{Misc, Defaults}

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {

    Misc.ensureTestModeDuringTests()

    // App package path
    LiftRules.addToPackages("com.beardedlogic.usecase")

    // Build SiteMap
    def sitemap(): SiteMap = SiteMap(
      Menu.i("Home") / "index"
    )

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) => new Html5Properties(r.userAgent))

    // Force requests to be UTF-8
    //LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    DB.init()
    Defaults.init()
  }
}
