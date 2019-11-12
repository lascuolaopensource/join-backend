package controllers

import java.time.Clock

import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.api.{LoginEvent, LoginInfo, SignUpEvent}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import controllers.AppController.AppControllerComponents
import javax.inject.{Inject, Singleton}
import models.forms.SignUpForm
import models.{MailToken, User}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent}
import services._

import scala.Function.const
import scala.concurrent.Future


@Singleton
class SignUpController @Inject() (
  override val controllerComponents: AppControllerComponents,
  userService: UserService,
  skillService: SkillService,
  passwordHasher: PasswordHasher,
  authInfoRepository: AuthInfoRepository,
  credentialsProvider: CredentialsProvider,
  configuration: Configuration,
  clock: Clock,
  mailService: MailService,
  mailTokenService: MailTokenService)
  extends AppController {

  def index: Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    redirectIfLoggedIn(request, Future.successful(Ok(views.html.signUp(SignUpForm.form))))
  }

  def startCreate: Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    redirectIfLoggedIn(request, {
      SignUpForm.form.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.signUp(formWithErrors))),
        userData => {
          val loginInfo = LoginInfo(CredentialsProvider.ID, userData.email)
          userService.retrieve(loginInfo) flatMap {
            case Some(_) =>
              val form = SignUpForm.form.bindFromRequest().withError("email", "signUp.error.emailExists")
              Future.successful(BadRequest(views.html.signUp(form)))
            case None =>
              val authInfo = passwordHasher.hash(userData.password)
              val user = User(
                id = 0,
                firstName = userData.firstName,
                lastName = userData.lastName,
                email = userData.email,
                emailConfirmed = false,
                loginInfo = Seq(),
                preferredLang = getSessionLang.getOrElse[Lang](request.acceptLanguages.headOption.getOrElse(Lang("it"))),
                agreement = userData.agreement,
                title = userData.title
              )

              for {
                user <- userService.save(user)
                _ <- authInfoRepository.add(loginInfo, authInfo)
                token <- mailTokenService.create(MailToken.signUp(user))
              } yield {
                mailService.signUp(user, routes.SignUpController.create(token.token).absoluteURL())
                Ok(views.html.signUp(SignUpForm.form, Some(user.email)))
              }
          }
        }
      )
    })
  }

  def create(token: String): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    redirectIfLoggedIn(request, {
      mailTokenService.find(token).flatMap {
        case services.Ok(mailToken) if mailToken.isSignUp =>
          if (mailToken.isExpired)
            mailTokenService.consume(token).map(const(notFound))
          else {
            val loginInfo = LoginInfo(CredentialsProvider.ID, mailToken.email)
            userService.retrieve(loginInfo).flatMap {
              case Some(user) =>
                for {
                  newUser <- {
                    if (!user.emailConfirmed)
                      userService.save(user.copy(emailConfirmed = true))
                    else Future.successful(user)
                  }
                  authenticator <- silhouette.env.authenticatorService.create(loginInfo)
                  cookie <- silhouette.env.authenticatorService.init(authenticator)
                  redirect = Redirect(configuration.underlying.getString("api.clients.sos-ui.first_login"))
                  result <- silhouette.env.authenticatorService.embed(cookie, redirect)
                  _ <- mailTokenService.consume(token)
                } yield {
                  silhouette.env.eventBus.publish(SignUpEvent(newUser, request))
                  silhouette.env.eventBus.publish(LoginEvent(newUser, request))
                  result
                }
              case None =>
                Future.failed(new IdentityNotFoundException("Couldn't find user"))
            }
          }
        case _ =>
          Future.successful(notFound)
      }
    })
  }

}



