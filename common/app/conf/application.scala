package conf

import common.GuardianConfiguration
import common.Assets.{Assets, CssMap}

object Configuration extends GuardianConfiguration
object Static extends Assets(Configuration.assets.path, "assets/assets.map")
object Atomise extends CssMap("assets/atomic-class-map.json")
object DiscussionAsset {
  def apply(assetName: String): String = {
    assets.DiscussionAssetsMap.getURL(assetName)
  }
}
