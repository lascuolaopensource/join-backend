package controllers.api

import controllers.api.ApiController.ApiControllerComponents
import javax.inject.{Inject, Singleton}
import models.Implicits._
import models._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import services.BazaarPreferenceService


@Singleton
class BazaarPreferencesController @Inject()(
  override val controllerComponents: ApiControllerComponents,
  bazaarPreferenceService: BazaarPreferenceService
) extends ApiController {

  private def find(id: Long, ideaType: BazaarIdeaType) = AllScope { request =>
    bazaarPreferenceService.find(request.authInfo.user.id, id, ideaType).map {
      case Some(preference) => Ok(Json.toJson(preference))
      case None => NotFound
    }
  }

  def findLearn(id: Long) = find(id, BazaarLearnType)
  def findTeach(id: Long) = find(id, BazaarTeachType)
  def findEvent(id: Long) = find(id, BazaarEventType)
  def findResearch(id: Long) = find(id, BazaarResearchType)


  private case class BazaarPreferenceRequest(agree: Boolean, favorite: Boolean)

  private implicit val bazaarPreferenceRequestReads: Reads[BazaarPreferenceRequest] = (
    (JsPath \ "agree").read[Boolean] and
    (JsPath \ "favorite").read[Boolean]
  )(BazaarPreferenceRequest.apply _)

  private def upsertFlags(ideaType: BazaarIdeaType, ideaId: Long) = AllScope { implicit request =>
    jsonOrBadRequest { json =>
      Json.fromJson[BazaarPreferenceRequest](json).fold(
        defaultErrorHandler,
        prefReq => {
          val user = request.authInfo.user
          val bazaarPreference = BazaarPreference(0, user.id, ideaId, ideaType, prefReq.agree, None, prefReq.favorite, viewed = true)
          bazaarPreferenceService.upsertFlags(bazaarPreference).map(p => Ok(Json.toJson(p)))
        }
      )
    }
  }

  def upsertFlagsLearn(id: Long) = upsertFlags(BazaarLearnType, id)
  def upsertFlagsTeach(id: Long) = upsertFlags(BazaarTeachType, id)
  def upsertFlagsEvent(id: Long) = upsertFlags(BazaarEventType, id)
  def upsertFlagsResearch(id: Long) = upsertFlags(BazaarResearchType, id)


  private case class BazaarWishRequest(comment: String)

  private implicit val bazaarWishRequestReads: Reads[BazaarWishRequest] =
    (JsPath \ "comment").read[String](minLength[String](1) keepAnd maxLength[String](2048)).map(BazaarWishRequest)

  private def upsertWish(ideaType: BazaarIdeaType, ideaId: Long) = AllScope { implicit request =>
    jsonOrBadRequest { json =>
      Json.fromJson[BazaarWishRequest](json).fold(
        defaultErrorHandler,
        wishReq => {
          val user = request.authInfo.user
          val comment = BazaarComment(0, user.id, None, None, wishReq.comment, None)
          val bazaarPreference = BazaarPreference(0, user.id, ideaId, ideaType, agree = false, Some(comment), favorite = false, viewed = false)
          bazaarPreferenceService.upsertWish(bazaarPreference).map(p => Ok(Json.toJson(p)))
        }
      )
    }
  }

  def upsertWishLearn(id: Long) = upsertWish(BazaarLearnType, id)
  def upsertWishTeach(id: Long) = upsertWish(BazaarTeachType, id)
  def upsertWishEvent(id: Long) = upsertWish(BazaarEventType, id)
  def upsertWishResearch(id: Long) = upsertWish(BazaarResearchType, id)


  def deleteWish(id: Long) = AllScope { request =>
    bazaarPreferenceService.deleteWish(BazaarPreference(id, request.authInfo.user.id)).map {
      case Some(preference) => Ok(Json.toJson(preference))
      case None => NotFound
    }
  }

}
