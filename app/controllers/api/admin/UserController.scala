package controllers.api.admin

import controllers.api.{ApiController, StatsController}
import controllers.api.ApiController.ApiControllerComponents
import javax.inject.{Inject, Singleton}
import models.Implicits._
import models.{AdminRole, NormalRole}
import services.{UserService, UserStatService}

import scala.concurrent.Future


@Singleton
class UserController @Inject()(
  override val controllerComponents: ApiControllerComponents,
  userService: UserService,
  userStatService: UserStatService
) extends ApiController with StatsController {

  def delete(id: Long) = AdminScope { request =>
    if (request.authInfo.user.id == id) Future.successful(BadRequest)
    else userService.delete(id) map deleteResult
  }

  def updateRole(id: Long) = AdminScope { request =>
    if (request.authInfo.user.id == id) Future.successful(BadRequest)
    else {
      val role = if (request.queryString.contains("admin")) AdminRole else NormalRole
      userService.updateRole(id, role).map(resultJsonFromServiceRep(_))
    }
  }

  def topTeaching = AdminScope { implicit request =>
    processStatsRequest("from", "to", userStatService.topTeaching(_, _))
  }

  def topResearch = AdminScope { implicit request =>
    processStatsRequest("from", "to", userStatService.topResearch(_, _))
  }

  def topIdeas = AdminScope { implicit request =>
    processStatsRequest("from", "to", userStatService.topIdeas(_, _))
  }

  def topSkills = AdminScope { implicit request =>
    processStatsRequest("from", "to", userStatService.topSkills(_, _))
  }

  def topMaker = AdminScope { implicit request =>
    processStatsRequest("from", "to", userStatService.topMaker(_, _))
  }

  def topFavored = AdminScope { implicit request =>
    processStatsRequest("from", "to", userStatService.topFavored(_, _))
  }

  def topFavorites = AdminScope { implicit request =>
    processStatsRequest("from", "to", userStatService.topFavorites(_, _))
  }

}
