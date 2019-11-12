package controllers

import controllers.AppController.AppControllerComponents
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import services.{MailTokenService, UserService}

import scala.Function.const
import scala.concurrent.Future


@Singleton
class MailConfirmController @Inject() (
  override val controllerComponents: AppControllerComponents,
  configuration: Configuration,
  mailTokenService: MailTokenService,
  userService: UserService)
  extends AppController {

  def confirmChangeEmail(token: String) = silhouette.UserAwareAction.async { implicit request =>
    mailTokenService.find(token) flatMap {
      case services.Ok(mailToken) if mailToken.isChangeEmail =>
        if (mailToken.isExpired)
          mailTokenService.consume(token).map(const(notFound))
        else
          for {
            optUser <- userService.find(mailToken.userId)
            user = optUser getOrElse (throw new RuntimeException(s"user ${mailToken.userId} not found for token $token"))
            _ <- userService.save(user.copy(email = mailToken.email))
            _ <- mailTokenService.consume(token)
            optAuthenticator <- silhouette.env.authenticatorService.retrieve
            redirect = Redirect(configuration.underlying.getString("api.clients.sos-ui.redirect_uri"))
            result <- optAuthenticator match {
              case Some(authenticator) =>
                val loginInfo = authenticator.loginInfo.copy(providerKey = mailToken.email)
                val updatedAuth = authenticator.copy(loginInfo = loginInfo)
                silhouette.env.authenticatorService.update(updatedAuth, redirect)
              case None =>
                Future.successful(redirect)
            }
          } yield result
      case _ =>
        Future.successful(notFound)
    }
  }

}
