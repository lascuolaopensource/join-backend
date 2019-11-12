package controllers

import auth.DefaultEnv
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import com.mohiva.play.silhouette.api.{LoginInfo, Silhouette}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import controllers.AppController.AppControllerComponents
import javax.inject.{Inject, Singleton}
import org.webjars.play.WebJarsUtil
import play.api.http.FileMimeTypes
import play.api.i18n.{I18nSupport, Lang, Langs, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions


object AppController {

  @Singleton
  case class AppControllerComponents @Inject()(
    actionBuilder: DefaultActionBuilder,
    parsers: PlayBodyParsers,
    messagesApi: MessagesApi,
    langs: Langs,
    fileMimeTypes: FileMimeTypes,
    executionContext: ExecutionContext,
    webJarsUtil: WebJarsUtil,
    silhouette: Silhouette[DefaultEnv]
  ) extends ControllerComponents

}


abstract class AppController extends BaseController with I18nSupport {

  override protected def controllerComponents: AppControllerComponents

  implicit protected val webJarsUtil = controllerComponents.webJarsUtil
  implicit protected val ec = controllerComponents.executionContext
  protected val silhouette = controllerComponents.silhouette

  implicit protected def key2LoginInfo(key: String) = LoginInfo(CredentialsProvider.ID, key)

  protected def notFound(implicit requestHeader: RequestHeader) =
    NotFound(views.html.errors.generic(NOT_FOUND))

  protected def redirectIfLoggedIn(request: UserAwareRequest[DefaultEnv, AnyContent], action: Future[Result]): Future[Result] =
    request.identity map (_ => Future.successful(Redirect(routes.HomeController.index()))) getOrElse action

  protected def getSessionLang(implicit request: UserAwareRequest[DefaultEnv, AnyContent]): Option[Lang] =
    request.session.get("preferred_lang") map (Lang(_))

  protected def replyWithLang(action: Future[Result])
    (implicit ec: ExecutionContext, request: UserAwareRequest[DefaultEnv, AnyContent]): Future[Result] =
    request.identity match {
      case Some(user) => action map (ac => messagesApi.setLang(ac, user.preferredLang.lang))
      case None => getSessionLang map { lang =>
        action map (ac => messagesApi.setLang(ac, lang))
      } getOrElse action
    }

  protected def getRedirectFromSession(user: DefaultEnv#I, status: Int = 303)(implicit request: RequestHeader) = {
    val url = routes.OAuth2Controller.accessToken().url
    def getParamsMap(clientId: String) = Map("grant_type" -> Seq("implicit"), "client_id" -> Seq(clientId))
    request.session.get("client_id") match {
      case Some(clientId) =>
        Redirect(url, getParamsMap(clientId), status)
          .removingFromSession("client_id")
          .addingToSession("auth_page" -> "1")
      case None =>
        val clientId = if (user.isAdmin) "admin" else "user"
        Redirect(url, getParamsMap(clientId), status)
    }
  }

}
