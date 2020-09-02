package shipreq.webapp.client.public

import japgolly.scalajs.react.Callback
import shipreq.webapp.base.AssetManifest
import shipreq.webapp.base.util.ResourceHintJs._

object Prefetch {

  // prefetch links cannot include integrity attributes
  // https://github.com/w3c/webappsec-subresource-integrity/issues/26

  private var doMemberHome = true

  /** Run this to prefetch resources for MembersHome */
  def memberHome(am: AssetManifest): Unit =
    if (doMemberHome) {
      val res: List[ResourceHint] =
        // ResourceHint.Prefetch.script(AssetManifest.reactDomServerJs) ::
        ResourceHint.Prefetch.script(am.memberLibBundleJs) ::
        // ResourceHint.Prefetch.script(AssetManifest.webappClientHomeJs) :: // actually, better security to not even reveal the URL
        // ResourceHint.Prefetch.script(AssetManifest.katexCss) ::
        // ResourceHint.Prefetch.script(AssetManifest.katexJs) ::
        Nil
      Callback(res.foreach(_.install())).delayMs(200).toCallback.runNow()
      doMemberHome = false
    }

}
