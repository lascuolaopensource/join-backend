package controllers.api.admin

import controllers.api.{ApiController, StatsController}
import controllers.api.ApiController.ApiControllerComponents
import javax.inject.{Inject, Singleton}
import models.Implicits._
import models.{Skill, SkillWithLinked}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import services.SkillService

import scala.concurrent.Future


private object SkillsController {

  case class SkillRequest(name: String, parentId: Option[Long], request: Option[Boolean])

  implicit val skillRequestReads: Reads[SkillRequest] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "parentId").readNullable[Long] and
      (JsPath \ "request").readNullable[Boolean]
    )(SkillRequest)

}


@Singleton
class SkillsController @Inject() (
  override val controllerComponents: ApiControllerComponents,
  skillService: SkillService
) extends ApiController with StatsController {

  import SkillsController._

  def search = AdminScope { request =>
    val name = request.getQueryString("name")
    val user = request.getQueryString("user")
    skillService.searchAdmin(name, user).map(toOk("skills"))
  }

  def requests = AdminScope { _ =>
    skillService.requests map { skillsRequests =>
      val requestsForJson = skillsRequests.map {
        case (skill, users) => SkillWithLinked(skill, users)
      }
      Ok(Json.toJson(Map("requests" -> requestsForJson)))
    }
  }

  def confirmRequest(skillId: Long) = AdminScope { request =>
    skillService.confirmRequest(skillId, request.queryString.contains("confirm"))
      .map(resultJsonFromServiceReply(_))
  }

  private def createUpdate(skillId: Option[Long], f: Skill => Future[Skill]) = AdminScope { implicit request =>
    validateOrBadRequest[SkillRequest]() { skillRequest =>
      val SkillRequest(name, parentId, request) = skillRequest
      val skill = Skill(skillId.getOrElse(0), name, parentId, request.getOrElse(false))
      f(skill).map(s => Ok(Json.toJson(Map("skill" -> s))))
    }
  }

  def create = createUpdate(None, skillService.create)
  def update(skillId: Long) = createUpdate(Some(skillId), skillService.update)


  def find(skillId: Long) = AdminScope { _ =>
    skillService.find(skillId) map {
      case Some(skillUsers) => Ok(Json.toJson(SkillWithLinked.tupled(skillUsers)))
      case None => NotFound
    }
  }

  def moveSkills(fromId: Long, toId: Long) = AdminScope { _ =>
    skillService.moveSkills(fromId, toId).map(resultJsonFromServiceReply(_))
  }

  def delete(skillId: Long) = AdminScope { _ =>
    skillService.deleteSkill(skillId).map(resultJsonFromServiceReply(_))
  }

  def deleteForUser(skillId: Long, userId: Long) = AdminScope { _=>
    skillService.deleteForUser(skillId, userId).map(resultJsonFromServiceReply(_))
  }

  def latest = AdminScope { _ =>
    skillService.latest().map(toOk("skills"))
  }

  def byUserCount = AdminScope { implicit request =>
    processStatsRequest("from", "to", skillService.byUserCount(_, _))
  }

}
