package services

import javax.inject.{Inject, Singleton}
import models.MailToken
import models.daos.MailTokenDAO

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions


@Singleton
class MailTokenService @Inject() (mailTokenDAO: MailTokenDAO)(implicit ec: ExecutionContext) extends Service {

  import Service.Ops._

  def find(token: String): Future[ServiceReply[MailToken, NotFound]] =
    mailTokenDAO find token map toNotFound

  def create(mailToken: MailToken): Future[MailToken] = mailTokenDAO create mailToken

  def consume(token: String): Future[ServiceReply[Unit, NotFound]] =
    mailTokenDAO consume token map toNotFound

}
