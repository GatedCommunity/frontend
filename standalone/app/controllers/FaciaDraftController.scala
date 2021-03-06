package controllers

import com.gu.contentapi.client.model.v1.ItemResponse
import contentapi.{ContentApiClient, SectionsLookUp}
import controllers.front.FrontJsonFapiDraft
import play.api.Environment
import play.api.mvc.{RequestHeader, Result}
import services.ConfigAgent

import scala.concurrent.Future

class FaciaDraftController(val frontJsonFapi: FrontJsonFapiDraft, contentApiClient: ContentApiClient, sectionsLookUp: SectionsLookUp)(implicit val env: Environment) extends FaciaController with RendersItemResponse {

  private val indexController = new IndexController(contentApiClient, sectionsLookUp)

  override def renderItem(path: String)(implicit request: RequestHeader): Future[Result] = {
    log.info(s"Serving Path: $path")

    if (!ConfigAgent.getPathIds.contains(path))
      indexController.renderItem(path)
    else
      renderFrontPressResult(path)
  }

  override def canRender(path: String): Boolean = ConfigAgent.getPathIds.contains(path)

  override def canRender(item: ItemResponse): Boolean = indexController.canRender(item)
}
