package controllers.api

import controllers.api.ApiController.ApiControllerComponents
import javax.inject.{Inject, Singleton}
import models.Implicits._
import models._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import services.BazaarCommentService


@Singleton
class BazaarCommentsController @Inject()(
  override val controllerComponents: ApiControllerComponents,
  bazaarCommentService: BazaarCommentService
) extends ApiController {

  private def find(ideaType: BazaarIdeaType, ideaId: Long) = AllScope { _ =>
    bazaarCommentService.find(ideaId, ideaType).map(comments => Ok(Json.toJson(Map("comments" -> comments))))
  }

  def findLearn(id: Long) = find(BazaarLearnType, id)
  def findTeach(id: Long) = find(BazaarTeachType, id)
  def findEvent(id: Long) = find(BazaarEventType, id)
  def findResearch(id: Long) = find(BazaarResearchType, id)

  private case class BazaarCommentRequest(comment: String)

  implicit private val bazaarCommentRequestReads: Reads[BazaarCommentRequest] =
    (JsPath \ "comment").read[String](minLength[String](1) keepAnd maxLength[String](2048)).map(BazaarCommentRequest)

  private def create(ideaType: BazaarIdeaType, ideaId: Long) = AllScope { implicit request =>
    jsonOrBadRequest { json =>
      Json.fromJson[BazaarCommentRequest](json).fold(
        defaultErrorHandler,
        commentRequest => {
          val user = request.authInfo.user
          val bazaarComment = BazaarComment(0, user.id, Some(user.firstName), Some(user.lastName), commentRequest.comment, None)
          bazaarCommentService.create(ideaId, ideaType, bazaarComment).map(comment => Ok(Json.toJson(comment)))
        }
      )
    }
  }

  def createLearn(id: Long) = create(BazaarLearnType, id)
  def createTeach(id: Long) = create(BazaarTeachType, id)
  def createEvent(id: Long) = create(BazaarEventType, id)
  def createResearch(id: Long) = create(BazaarResearchType, id)


  def delete(id: Long) = AllScope { request =>
    val user = request.authInfo.user
    bazaarCommentService.delete(BazaarComment(id, user.id), user.isAdmin) map deleteResult
  }

}
