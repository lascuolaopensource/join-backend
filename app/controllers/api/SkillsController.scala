package controllers.api

import controllers.api.ApiController.ApiControllerComponents
import javax.inject.{Inject, Singleton}
import models.Implicits._
import models.UserSkill
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import services.SkillService

import scala.concurrent.Future

@Singleton
class SkillsController @Inject() (
  override val controllerComponents: ApiControllerComponents,
  skillService: SkillService
) extends ApiController {


  def index = AllScope { request =>
    val skills = (request.queryString.get("name"), request.queryString.contains("all")) match {
      case (Some(Seq(name)), allNodes) =>
        skillService.search(name, allNodes)
      case _ => skillService.all
    }

    skills map (skills => Ok(Json.toJson(Map("skills" -> skills))))
  }

  private object CreateSkillRequest {

    val form = Form(
      mapping(
        "name" -> nonEmptyText(minLength = 2, maxLength = 255)
      )(Data.apply)(Data.unapply)
    )

    case class Data(name: String)

  }

  def create = AllScope { implicit request =>
    jsonOrBadRequest { json =>
      CreateSkillRequest.form.bind(json).fold(
        defaultJsonWithErrorsAction,
        skillData => {
          val userSkill = UserSkill(0, skillData.name, 0, request.authInfo.user.id, request.authInfo.clientId.get == "user")
          skillService save userSkill map (us => Ok(Json.toJson(us)))
        }
      )
    }
  }

  def delete(userSkillId: Long) = AllScope { request =>
    skillService.all(request.authInfo.user) flatMap { userSkills =>
      if (userSkills.map(_.id).contains(userSkillId))
        skillService.delete(userSkillId) map (affectedRows => deleteResult(affectedRows > 0))
      else
        Future.successful(NotFound)
    }
  }

}
