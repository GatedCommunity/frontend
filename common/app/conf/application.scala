package conf

import common.GuardianConfiguration
import common.Assets.{Assets, CssMap}
import play.api.Environment

trait EnvironmentHelper {
  implicit val env: Environment
}

object Configuration extends GuardianConfiguration
object Static extends Assets(Configuration.assets.path, "assets/assets.map") with EnvironmentHelper
object Atomise extends CssMap("assets/atomic-class-map.json") with EnvironmentHelper
object DiscussionAsset {
  def apply(assetName: String): String = {
    assets.DiscussionAssetsMap.getURL(assetName)
  }
}
