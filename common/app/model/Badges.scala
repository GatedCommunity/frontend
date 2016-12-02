package model

import conf.Static
import layout.FaciaContainer
import java.security.MessageDigest
import java.math.BigInteger

import play.api.Environment

import scala.util.control.NonFatal


trait BaseBadge {
  def maybeThisBadge(tag: String): Option[Badge]
}

case class Badge(seriesTag: String, imageUrl: String, classModifier: Option[String] = None) extends BaseBadge {
  def maybeThisBadge(tag: String) = if (seriesTag == tag) Some(this) else None
}

// for salt use a random unique string - e.g. some string from running in terminal: pwgen -n -y 20
// it's fine to commit that, it just stops people using previously calculated tables to reverse the hash
case class SpecialBadge(salt: String, hashedTag: String) extends BaseBadge {
  def maybeThisBadge(tag: String) =
    if (md5(salt + tag).contains(hashedTag)) {
      Some(Badge(tag, s"https://assets.guim.co.uk/special/$tag/special-badge.svg"))
    } else None

  private val digest = MessageDigest.getInstance("MD5")

  private def md5(input: String): Option[String] = {
    try {
      digest.update(input.getBytes(), 0, input.length)

      Option(new BigInteger(1, digest.digest()).toString(16))
    } catch {
      case NonFatal(_) => None
    }
  }
}

object Badges {

  private def euSvg(implicit env: Environment) = Static("images/badges/eu-ref.svg")

  def usElection(implicit env: Environment) = Badge("us-news/us-elections-2016", Static("images/badges/us-election.png"), Some("us-election"))
  def ausElection(implicit env: Environment) = Badge("australia-news/australian-election-2016", Static("images/badges/aus-election.png"), Some("aus-election"))
  def voicesOfAmerica(implicit env: Environment) = Badge("us-news/series/voices-of-america", Static("images/badges/voices-of-america.svg"), Some("voices-of-america"))

  def euElection(implicit env: Environment) = Badge("politics/eu-referendum", euSvg)
  def euRealityCheck(implicit env: Environment) = Badge("politics/series/eu-referendum-reality-check", euSvg)
  def euBriefing(implicit env: Environment) = Badge("politics/series/eu-referendum-morning-briefing", euSvg)
  def euSparrow(implicit env: Environment) = Badge("politics/series/eu-referendum-live-with-andrew-sparrow", euSvg)

  def rio2016(implicit env: Environment) = Badge("sport/rio-2016", Static("images/badges/rio-2016.svg"))

  def nauru(implicit env: Environment) = Badge("news/series/nauru-files", Static("images/badges/nauru-files.svg"))

  def allBadges(implicit env: Environment) = Seq(usElection, ausElection, voicesOfAmerica, nauru, rio2016, euElection, euRealityCheck, euBriefing, euSparrow)

  def badgeFor(c: ContentType)(implicit env: Environment) = {
    badgeForTags(c.tags.tags.map(_.id))
  }

  def badgeForTags(tags: Traversable[String])(implicit env: Environment) = {

    val badgesForTags =
      for {
        tagId <- tags
        baseBadge <- allBadges
        maybeBadge <- baseBadge.maybeThisBadge(tagId)
      } yield maybeBadge
    badgesForTags.headOption
  }

  def badgeFor(fc: FaciaContainer)(implicit env: Environment) = badgeForTags(fc.href)

}
