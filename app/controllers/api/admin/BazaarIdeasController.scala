package controllers.api.admin

import controllers.api.ApiController.ApiControllerComponents
import controllers.api.{ApiController, StatsController}
import javax.inject.{Inject, Singleton}
import models.Implicits._
import services.{BazaarIdeaService, BazaarIdeaStatService}


@Singleton
class BazaarIdeasController @Inject()(
  override val controllerComponents: ApiControllerComponents,
  bazaarIdeaService: BazaarIdeaService,
  bazaarIdeaStatService: BazaarIdeaStatService
) extends ApiController with StatsController {

  def deleteLearn(id: Long) = AdminScope { request =>
    bazaarIdeaService.deleteLearn(id, request.authInfo.user, disabled = true) map resultFromServiceReply(NoContent)
  }

  def deleteTeach(id: Long) = AdminScope { request =>
    bazaarIdeaService.deleteTeach(id, request.authInfo.user, disabled = true) map resultFromServiceReply(NoContent)
  }

  def deleteEvent(id: Long) = AdminScope { request =>
    bazaarIdeaService.deleteEvent(id, request.authInfo.user, disabled = true) map resultFromServiceReply(NoContent)
  }

  def deleteResearch(id: Long) = AdminScope { request =>
    bazaarIdeaService.deleteResearch(id, request.authInfo.user, disabled = true) map resultFromServiceReply(NoContent)
  }


  def byUser(userId: Long) = AdminScope { _ =>
    bazaarIdeaService.byUser(userId) map toOk("ideas")
  }


  def latestIdeas = AdminScope { implicit request =>
    processStatsRequest("to", bazaarIdeaStatService.latestIdeas(_))
  }

  def count = AdminScope { implicit request =>
    processStatsRequest("to", bazaarIdeaStatService.count)
  }

  def topScoredEvent = AdminScope { implicit request =>
    processStatsRequest("from", "to", bazaarIdeaStatService.topScoredEvent(_, _))
  }

  def topScoredTeach = AdminScope { implicit request =>
    processStatsRequest("from", "to", bazaarIdeaStatService.topScoredTeach(_, _))
  }

  def topScoredResearch = AdminScope { implicit request =>
    processStatsRequest("from", "to", bazaarIdeaStatService.topScoredResearch(_, _))
  }

  def topCreators = AdminScope { implicit request =>
    processStatsRequest("from", "to", bazaarIdeaStatService.topCreators(_, _))
  }

  def topEver = AdminScope { _ =>
    bazaarIdeaStatService.topEver().map(okStats(_))
  }

}
