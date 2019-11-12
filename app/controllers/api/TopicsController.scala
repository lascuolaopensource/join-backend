package controllers.api

import controllers.api.ApiController.ApiControllerComponents
import javax.inject.{Inject, Singleton}
import models.Implicits._
import play.api.libs.json._
import services.TopicService


@Singleton
class TopicsController @Inject()(
  override val controllerComponents: ApiControllerComponents,
  topicService: TopicService
) extends ApiController {

  def index = AllScope { request =>
    topicService.search(request.queryString.get("topic").map(_.head)).map { topics =>
      Ok(Json.toJson(Map("topics" -> topics)))
    }
  }

}
