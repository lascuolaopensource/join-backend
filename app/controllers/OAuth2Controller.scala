package controllers

import java.security.SecureRandom

import auth.OAuthDataHandlerImpl
import controllers.AppController.AppControllerComponents
import javax.inject.{Inject, Singleton}
import models.daos.AccessTokenDAO
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import scalaoauth2.provider.{AuthorizationHandler, OAuth2Provider, OAuthGrantType, TokenEndpoint}

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class OAuth2Controller @Inject() (
  override val tokenEndpoint: TokenEndpoint,
  override val controllerComponents: AppControllerComponents,
  config: Configuration,
  secureRandom: SecureRandom,
  accessTokenDAO: AccessTokenDAO)
  extends AppController with OAuth2Provider {

  def accessToken: Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) =>
        issueAccessToken(new OAuthDataHandlerImpl(config, secureRandom, accessTokenDAO, Some(user)))(request, ec)
      case _ =>
        val session = request.getQueryString("client_id") match {
          case Some(clientId) => request.session + ("client_id" -> clientId)
          case None =>           request.session
        }
        Future.successful(Redirect(routes.SignInController.index()).withSession(session))
    }
  }

  override def issueAccessToken[A, U](handler: AuthorizationHandler[U])(implicit request: Request[A], ctx: ExecutionContext): Future[Result] = {
    tokenEndpoint.handleRequest(request, handler)(ctx).map {
      case Left(e) => new Status(e.statusCode)(responseOAuthErrorJson(e)).withHeaders(responseOAuthErrorHeader(e))
      case Right(r) =>
        r.authInfo.redirectUri match {
          case Some(uri) if request.grantType == OAuthGrantType.IMPLICIT =>
            val page = request.getQueryString("redirect_uri")
              .getOrElse(if (request.session.get("auth_page").isDefined) "auth.html" else "")
            Redirect(s"$uri$page#token=${r.accessToken}").removingFromSession("auth_page")
          case _ =>
            Ok(Json.toJson(responseAccessToken(r)))
              .withHeaders("Cache-Control" -> "no-store", "Pragma" -> "no-cache")
              .removingFromSession("client_id")
        }
    }(ctx)
  }

}
