package views.support

import common.Edition
import model.Article
import play.api.Environment
import play.api.mvc.RequestHeader
import views.support.cleaner._

object MainCleaner {
  def apply(article: Article, html: String, amp: Boolean)(implicit request: RequestHeader, env: Environment) = {
    implicit val edition = Edition(request)
    withJsoup(BulletCleaner(html))(
      if (amp) AmpEmbedCleaner(article) else VideoEmbedCleaner(article),
      PictureCleaner(article, amp),
      MainFigCaptionCleaner,
      AtomsCleaner(article.content.atoms, shouldFence = true, amp)
    )
  }
}
