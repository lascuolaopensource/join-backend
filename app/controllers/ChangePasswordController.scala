package controllers

import com.mohiva.play.silhouette.api.LoginEvent
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{Credentials, PasswordHasherRegistry}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import controllers.AppController.AppControllerComponents
import javax.inject.{Inject, Singleton}
import models.MailToken
import models.forms.ChangePasswordForm
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.RequestHeader
import services.{MailService, MailTokenService, UserService}

import scala.Function.const
import scala.concurrent.Future


@Singleton
class ChangePasswordController @Inject()(
  override val controllerComponents: AppControllerComponents,
  userService: UserService,
  mailTokenService: MailTokenService,
  mailService: MailService,
  authInfoRepository: AuthInfoRepository,
  passwordHasherRegistry: PasswordHasherRegistry,
  credentialsProvider: CredentialsProvider
) extends AppController {

  val emailForm = Form(single("email" -> email))

  def forgotPassword = silhouette.UserAwareAction.async { implicit request =>
    redirectIfLoggedIn(request, Future.successful(Ok(views.html.forgotPassword(emailForm))))
  }

  def handleForgotPassword = silhouette.UnsecuredAction.async { implicit request =>
    emailForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.forgotPassword(formWithErrors))),
      email => userService.retrieve(email).flatMap {
        case Some(user) =>
          mailTokenService.create(MailToken.resetPassword(user)) map { mt =>
            mailService.resetPassword(user, routes.ChangePasswordController.resetPassword(mt.token).absoluteURL())
            Ok(views.html.forgotPassword(emailForm, Some(user.email)))
          }
        case None =>
          val formWithErrors = emailForm.withError("email", "forgotPassword.error.emailNotFound")
          Future.successful(BadRequest(views.html.forgotPassword(formWithErrors)))
      }
    )
  }

  val resetPasswordForm = Form(tuple(
    "password1" -> models.forms.passwordValidation,
    "password2" -> nonEmptyText
  ) verifying ("resetPassword.passwords.notEqual", passwords => passwords._2 == passwords._1))

  def consumeIfExpired(mailToken: MailToken)(implicit requestHeader: RequestHeader) =
    if (mailToken.isExpired) Some(mailTokenService consume mailToken.token map const(notFound)) else None

  def resetPassword(token: String) = silhouette.UnsecuredAction.async { implicit request =>
    mailTokenService.find(token) flatMap {
      case services.Ok(mailToken) if mailToken.isResetPassword =>
        consumeIfExpired(mailToken) getOrElse {
          Future.successful(Ok(views.html.resetPassword(token, resetPasswordForm)))
        }
      case _ =>
        Future.successful(notFound)
    }
  }

  def handleResetPassword(token: String) = silhouette.UnsecuredAction.async { implicit request =>
    resetPasswordForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.resetPassword(token, formWithErrors))),
      passwords => mailTokenService.find(token) flatMap {
        case services.Ok(mailToken) if mailToken.isResetPassword =>
          consumeIfExpired(mailToken) getOrElse userService.find(mailToken.userId).flatMap {
            case Some(user) =>
              for {
                _ <- mailTokenService.consume(token)
                _ <- authInfoRepository.update(user.email, passwordHasherRegistry.current.hash(passwords._1))
                authenticator <- silhouette.env.authenticatorService.create(user.email)
                authResult <- silhouette.env.authenticatorService.renew(authenticator, getRedirectFromSession(user))
              } yield {
                silhouette.env.eventBus.publish(LoginEvent(user, request))
                authResult
              }
            case None =>
              val msg = s"could not find user ${mailToken.userId} from token ${mailToken.token}"
              Future.failed(new IdentityNotFoundException(msg))
          }
        case _ =>
          Future.successful(notFound)
      }
    )
  }


  def changePassword = silhouette.UserAwareAction { implicit request =>
    if (request.identity.isDefined)
      Ok(views.html.changePassword(ChangePasswordForm.form))
    else
      Redirect(routes.SignInController.index())
  }

  def handleChangePassword = silhouette.UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) =>
        ChangePasswordForm.form.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(views.html.changePassword(formWithErrors))),
          formData => credentialsProvider.authenticate(Credentials(user.email, formData.current)).flatMap { loginInfo =>
            for {
              _ <- authInfoRepository.update(loginInfo, passwordHasherRegistry.current.hash(formData.password1))
              authenticator <- silhouette.env.authenticatorService.create(loginInfo)
              result <- silhouette.env.authenticatorService.renew(authenticator, getRedirectFromSession(user))
            } yield result
          }.recover {
            case _: ProviderException =>
              val form = ChangePasswordForm.form.withError("current", "changePassword.current.mismatch")
              BadRequest(views.html.changePassword(form))
          }
        )
      case None =>
        Future.successful(Redirect(routes.SignInController.index()))
    }
  }

}
