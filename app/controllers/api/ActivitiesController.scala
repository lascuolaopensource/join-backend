package controllers.api

import controllers.api.ActivitiesController._
import controllers.api.ApiController.{ApiControllerComponents, RequestType}
import javax.inject.{Inject, Singleton}
import models.Implicits._
import models._
import play.api.libs.json.{Json, _}
import play.api.mvc.Result
import services.ActivityService.{ApplicationsFilterAdmin, ApplicationsFilterUser}
import services.{ActivityService, ServiceReply}

import scala.concurrent.Future


private object ActivitiesController {

  case class PaymentInfoRequest(
    paymentMethod: PaymentMethod,
    referenceId: String,
    amount: Double)

  implicit val paymentInfoRequestReads = Json.reads[PaymentInfoRequest]

  case class ResearchAppRequest(
    apply: Boolean,
    motivation: Option[String])
  {
    def toModel(userId: Long) = ActivityResearchApp(
      userId = userId,
      motivation = motivation,
      createdAt = dummyTimestamp)
  }

  implicit val researchAppRequestReads = Json.reads[ResearchAppRequest]

}


@Singleton
class ActivitiesController @Inject() (
  override val controllerComponents: ApiControllerComponents,
  activityService: ActivityService)
  extends ApiController {

  def index = AllScope { implicit request =>
    val userId = request.authInfo.user.id
    val fromAdmin = request.authInfo.clientId.getOrElse("user") == "admin"
    val search = request.getQueryString("search")
    val skills = request.queryString.get("skillId").map(_.map(_.toLong)).getOrElse(Seq.empty)
    val matchAll = request.queryString.contains("matchAll")
    val future = request.queryString.contains("future")
    activityService.all(getLanguage, userId, fromAdmin, search, skills, matchAll, future).map { activities =>
      Ok(Json.obj("activities" -> activities))
    }
  }

  private def allByType[A: Writes](f: (Language, Long, Boolean) => Future[Seq[A]])(implicit request: RequestType) = {
    val userId = request.authInfo.user.id
    val future = request.queryString.contains("future")
    f(getLanguage, userId, future).map { activities =>
      Ok(Json.obj("activities" -> activities))
    }
  }

  def allTeach = AllScope { implicit request =>
    allByType(activityService.allTeach)
  }

  def allEvent = AllScope { implicit request =>
    allByType(activityService.allEvent)
  }

  def allResearch = AllScope { implicit request =>
    allByType(activityService.allResearch)
  }


  def findEvent(id: Long) = AllScope { implicit request =>
    activityService.findEvent(id, getLanguage, request.authInfo.user.id, future = false)
      .map(resultJsonFromServiceReply(_))
  }

  def findTeach(id: Long) = AllScope { implicit request =>
    activityService.findTeach(id, getLanguage, request.authInfo.user.id, future = false)
      .map(resultJsonFromServiceReply(_))
  }

  def findResearch(id: Long) = AllScope { implicit request =>
    activityService.findResearch(id, getLanguage, request.authInfo.user.id, future = false)
      .map(resultJsonFromServiceReply(_))
  }


  private def favoritesForUser(userId: Long, language: Language) =
    activityService.favorites(userId, language)
      .map(activities => Ok(Json.obj("activities" -> activities)))

  def favorites(userId: Long) = AdminScope { implicit request =>
    favoritesForUser(userId, getLanguage)
  }

  def myFavorites = AllScope { implicit request =>
    favoritesForUser(request.authInfo.user.id, getLanguage)
  }

  private def favorite(
    activityId: Long,
    f: (Long, Long, Boolean) => Future[ServiceReply[Unit, services.NotFound]]
  ) = AllScope { implicit request =>
    jsonOrBadRequest { json =>
      json.validate((JsPath \ "favorite").read[Boolean]).fold(
        defaultErrorHandler,
        favorite => f(activityId, request.authInfo.user.id, favorite).map(resultJsonFromServiceReply(_))
      )
    }
  }

  def favoriteEvent(activityId: Long) = favorite(activityId, activityService.favoriteTeachEvent)
  def favoriteTeach(activityId: Long) = favorite(activityId, activityService.favoriteTeachEvent)
  def favoriteResearch(activityId: Long) = favorite(activityId, activityService.favoriteResearch)


  def subscribe(activityId: Long) = AllScope { implicit request =>
    val storeSubscription: Option[PaymentInfo] => Future[Result] =
      activityService.subscribe(activityId, request.authInfo.user.id, getLanguage, _)
        .map(resultJsonFromServiceReply(_))

    request.body.asJson match {
      case Some(json) =>
        json.validate[PaymentInfoRequest].fold(
          defaultErrorHandler,
          paymentInfoRequest => {
            val (nonce, cro) = paymentInfoRequest.paymentMethod match {
              case PayPal | CreditCard => (Some(paymentInfoRequest.referenceId), None)
              case _ => (None, Some(paymentInfoRequest.referenceId))
            }

            val paymentInfo = PaymentInfo(
              id = 0,
              paymentMethod = paymentInfoRequest.paymentMethod,
              transactionId = None,
              cro = cro,
              verified = None,
              amount = paymentInfoRequest.amount,
              paymentNonce = nonce)

            storeSubscription(Some(paymentInfo))
          }
        )
      case None =>
        storeSubscription(None)
    }
  }


  def changeApplication(roleId: Long) = AllScope { implicit request =>
    jsonOrBadRequest { json =>
      json.validate[ResearchAppRequest].fold(
        defaultErrorHandler,
        appRequest => {
          val appModel = appRequest.toModel(request.authInfo.user.id)
          activityService.changeApplication(roleId, appRequest.apply, appModel).map {
            case services.Ok(Some(a)) => Ok(Json.toJson(a))
            case services.Ok(None) => NoContent
            case services.Error(e) => e
          }
        }
      )
    }
  }


  def applications(activityId: Long) = AllScope { request =>
    val userFilter = if (request.authInfo.user.isAdmin)
      ApplicationsFilterAdmin
    else ApplicationsFilterUser(request.authInfo.user.id)

    activityService.applications(activityId, userFilter).map(resultJsonFromServiceReply(_, Some("applications")))
  }

}
