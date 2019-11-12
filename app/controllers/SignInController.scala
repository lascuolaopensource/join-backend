package controllers

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{Clock, Credentials, PasswordHasher}
import com.mohiva.play.silhouette.api.{LoginEvent, LogoutEvent}
import com.mohiva.play.silhouette.impl.exceptions.{IdentityNotFoundException, InvalidPasswordException}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import controllers.AppController.AppControllerComponents
import javax.inject.{Inject, Singleton}
import models.forms.SignInForm
import net.ceedubs.ficus.Ficus._
import play.api.Configuration
import play.api.mvc.{Action, AnyContent}
import services.UserService

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps


@Singleton
class SignInController @Inject() (
  override val controllerComponents: AppControllerComponents,
  userService: UserService,
  passwordHasher: PasswordHasher,
  authInfoRepository: AuthInfoRepository,
  credentialsProvider: CredentialsProvider,
  configuration: Configuration,
  clock: Clock)
  extends AppController {

  def index: Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    redirectIfLoggedIn(request, Future.successful(Ok(views.html.signIn(SignInForm.form))))
  }

  def create: Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    redirectIfLoggedIn(request, {
      SignInForm.form.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.signIn(formWithErrors))),
        loginData => {
          val credentials = Credentials(loginData.email, loginData.password)
          credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
            userService.retrieve(loginInfo).flatMap {
              case Some(user) =>
                val c = configuration.underlying
                silhouette.env.authenticatorService.create(loginInfo).map {
                  case authenticator if loginData.rememberMe =>
                    authenticator.copy(
                      expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                      idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
                      cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
                    )
                  case authenticator => authenticator
                }.flatMap { authenticator =>
                  silhouette.env.eventBus.publish(LoginEvent(user, request))
                  silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
                    silhouette.env.authenticatorService.embed(v, getRedirectFromSession(user))
                  }
                }
              case None =>
                Future.failed(new IdentityNotFoundException("Couldn't find user"))
            }
          }.recover {
            case _ @ (_:IdentityNotFoundException | _:InvalidPasswordException) =>
              val form = SignInForm.form.bindFromRequest().withGlobalError("signIn.error.wrongCredential")
              BadRequest(views.html.signIn(form))
          }
        }
      )
    })
  }

  def signOut: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val result = Redirect(routes.SignInController.index())
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }

}




