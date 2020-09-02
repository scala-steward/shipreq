package shipreq.webapp.base

object AssetManifest {
  type CDN = AbstractAssetManifest.CDN
  val  CDN = AbstractAssetManifest.CDN
}

final class AssetManifest extends AbstractAssetManifest[String] {

  override  protected def modify(urlPath: String): String =
    urlPath

}
