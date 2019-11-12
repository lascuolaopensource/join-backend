package controllers

import controllers.AppController.AppControllerComponents
import javax.inject._
import models.daos.RulesDAO
import play.api.Configuration
import play.api.i18n.Lang
import services.UserService

import scala.concurrent.Future


@Singleton
class HomeController @Inject() (
  override val controllerComponents: AppControllerComponents,
  userService: UserService,
  configuration: Configuration,
  rulesDAO: RulesDAO)
  extends AppController {

  private val uiUrl = configuration.get[String]("api.clients.sos-ui.redirect_uri")

  def index = silhouette.UserAwareAction.async { implicit request =>
    replyWithLang {
      if (request.identity.isDefined)
        Future.successful(Redirect(uiUrl))
      else
        Future.successful(Redirect(routes.SignInController.index()))
    }
  }

  def changeLang(lang: String) = silhouette.UserAwareAction.async { implicit request =>
    val formBody = request.request.body.asFormUrlEncoded.get
    val result = Redirect(formBody.getOrElse("redirect_url", Seq(routes.HomeController.index().url)).head)

    request.identity match {
      case Some(user) =>
        userService.save(user.copy(preferredLang = lang)).map { u =>
          messagesApi.setLang(result, u.preferredLang.lang)
        }
      case None =>
        Future.successful {
          messagesApi.setLang(result, Lang(lang)).withSession(request.session + ("preferred_lang" -> lang))
        }
    }
  }

  def rules = silhouette.UserAwareAction.async { implicit request =>
    rulesDAO.get.map(rulesOpt => Ok(views.html.rules(request.identity, rulesOpt.getOrElse(""))))
  }

}
