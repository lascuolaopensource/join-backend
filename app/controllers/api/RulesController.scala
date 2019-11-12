package controllers.api

import controllers.api.ApiController.ApiControllerComponents
import javax.inject.{Inject, Singleton}
import models.daos.RulesDAO
import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, single}
import play.api.libs.json.Json


object RulesController {

  val updateRulesForm = Form(single("rules" -> nonEmptyText))

}

@Singleton
class RulesController @Inject() (
  override val controllerComponents: ApiControllerComponents,
  rulesDAO: RulesDAO
) extends ApiController {

  import RulesController._

  def get = AllScope { _ =>
    rulesDAO.get.map(optStr => Ok(Json.obj("rules" -> optStr)))
  }

  def set = AdminScope { implicit request =>
    validateOrBadRequest(updateRulesForm){ rules =>
      rulesDAO.set(rules).map(if (_) NoContent else InternalServerError)
    }
  }

}
