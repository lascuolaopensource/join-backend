package models.daos

import models.{BazaarComment, BazaarIdeaType}

import scala.concurrent.Future


trait BazaarCommentDAO {

  def count(ideaId: Long, ideaType: BazaarIdeaType): Future[Int]
  def findSingle(id: Long, ideaType: BazaarIdeaType): Future[Option[BazaarComment]]
  def find(ideaId: Long, ideaType: BazaarIdeaType): Future[Seq[BazaarComment]]
  def create(ideaId: Long, ideaType: BazaarIdeaType, bazaarComment: BazaarComment): Future[BazaarComment]
  def delete(bazaarComment: BazaarComment, admin: Boolean): Future[Boolean]

}
