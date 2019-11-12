package controllers.api

import controllers.api.ApiController.ApiControllerComponents
import javax.inject.{Inject, Singleton}
import models.Implicits._
import models.MembershipType
import play.api.data.Form
import play.api.data.Forms._
import services.MembershipService


//noinspection MutatorLikeMethodIsParameterless
@Singleton
class MembershipsController @Inject()(
  override val controllerComponents: ApiControllerComponents,
  membershipService: MembershipService
) extends ApiController with StatsController {

  def allRequests = AdminScope { implicit request =>
    membershipService.allRequests(getLanguage).map(toOk("requests"))
  }

  val requestNewForm = Form(single("type" -> number))

  def requestNew = AllScope { implicit request =>
    validateOrBadRequest(requestNewForm) { typeId =>
      membershipService.requestNew(request.authInfo.user.id, typeId, getLanguage) map (resultJsonFromServiceRep(_))
    }
  }

  def requestRenewal = AllScope { implicit request =>
    membershipService.requestRenewal(request.authInfo.user.id, getLanguage) map (resultJsonFromServiceRep(_))
  }

  def acceptRequest(userId: Long) = AdminScope { implicit request =>
    membershipService.acceptRequest(userId, getLanguage) map (resultJsonFromServiceRep(_))
  }

  def deleteRequest = AllScope { request =>
    membershipService.deleteRequest(request.authInfo.user.id) map (resultJsonFromServiceRep(_))
  }

  def deleteActive = AllScope { request =>
    membershipService.deleteActive(request.authInfo.user.id) map (resultJsonFromServiceRep(_))
  }

  def deleteRenewal = AllScope { request =>
    membershipService.deleteRenewal(request.authInfo.user.id) map (resultJsonFromServiceRep(_))
  }

  def allTypes = AllScope { implicit request =>
    membershipService.allTypes(getLanguage) map toOk("types")
  }

  def createType = AdminScope { implicit request =>
    validateOrBadRequest[MembershipType]() { membershipType =>
      membershipService.createType(membershipType) map toOk("type")
    }
  }

  def updateType(id: Long) = AdminScope { implicit request =>
    validateOrBadRequest[MembershipType]() { membershipType =>
      membershipService.updateType(membershipType.copy(id = id)).map(resultJsonFromServiceRep(_, Some("type")))
    }
  }

  def deleteType(id: Long) = AdminScope { _ =>
    membershipService.deleteType(id) map (resultJsonFromServiceRep(_))
  }

  def countActive = AdminScope { implicit request =>
    processStatsRequest("to", membershipService.countActiveByType(_, getLanguage))
  }

}
