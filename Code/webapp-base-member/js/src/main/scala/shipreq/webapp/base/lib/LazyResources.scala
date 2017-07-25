package shipreq.webapp.base.lib

import scala.scalajs.js
import shipreq.webapp.base.AssetManifest
import shipreq.webapp.base.jsfacade.KaTeX

object LazyResources {

  lazy val reactDomServer: LazyLoader[Unit] =
    LazyLoader.js(AssetManifest.reactDomServerJs)

  lazy val katex: LazyLoader[KaTeX] =
    LazyLoader.merge(
      reactDomServer,
      LazyLoader.js(AssetManifest.katexJs),
      LazyLoader.css(AssetManifest.katexCss)
    ).map(_ => js.Dynamic.global.katex.asInstanceOf[KaTeX])

}
