package services

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import models.User
import play.api.Configuration
import play.api.i18n.Messages
import play.api.libs.mailer.{Email, MailerClient}
import play.twirl.api.Html
import views.html.mails

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}


@Singleton
class MailService @Inject() (
  mailerClient: MailerClient,
  system: ActorSystem,
  conf: Configuration
)(implicit ec: ExecutionContext) {

  private lazy val from = conf.underlying.getString("play.mailer.from")

  private implicit def html2String(html: Html): String = html toString

  private def sendEmailAsync(recipients: String*)(subject: String, bodyHtml: String, bodyText: String) = {
    system.scheduler.scheduleOnce(100 milliseconds) {
      sendEmail(recipients: _*)(subject, bodyHtml, bodyText)
    }
  }

  private def sendEmail(recipients: String*)(subject: String, bodyHtml: String, bodyText: String) =
    mailerClient.send(Email(subject, from, recipients, Some(bodyText), Some(bodyHtml)))


  def signUp(user: User, link: String)(implicit messages: Messages) =
    sendEmailAsync(user.email)(
      subject = Messages("mail.signUp.subject"),
      bodyHtml = mails.signUp(user.firstName, link),
      bodyText = mails.signUpTxt(user.firstName, link))

  def changeEmail(user: User, email: String, link: String)(implicit messages: Messages) =
    sendEmailAsync(email)(
      subject = Messages("mail.changeEmail.subject"),
      bodyHtml = mails.changeEmail(user.firstName, link),
      bodyText = mails.changeEmailTxt(user.firstName, link))

  def resetPassword(user: User, link: String)(implicit messages: Messages) =
    sendEmailAsync(user.email)(
      subject = Messages("mail.resetPassword.subject"),
      bodyHtml = mails.resetPassword(user.firstName, link),
      bodyText = mails.resetPasswordTxt(user.firstName, link))

}
