package models.daos

import models.MailToken

import scala.concurrent.Future


trait MailTokenDAO {

  def find(token: String): Future[Option[MailToken]]
  def create(mailToken: MailToken): Future[MailToken]
  def consume(token: String): Future[Boolean]

}
