package services

import javax.inject.Inject

import models.daos.BazaarCommentDAO
import models.{BazaarComment, BazaarIdeaType}

import scala.concurrent.Future


trait BazaarCommentService extends Service {

  def count(ideaId: Long, ideaType: BazaarIdeaType): Future[Int]
  def find(ideaId: Long, ideaType: BazaarIdeaType): Future[Seq[BazaarComment]]
  def create(ideaId: Long, ideaType: BazaarIdeaType, bazaarComment: BazaarComment): Future[BazaarComment]
  def delete(bazaarComment: BazaarComment, admin: Boolean): Future[Boolean]

}

class BazaarCommentServiceImpl @Inject()(
  bazaarCommentDAO: BazaarCommentDAO
) extends BazaarCommentService {

  override def count(ideaId: Long, ideaType: BazaarIdeaType) = bazaarCommentDAO.count(ideaId, ideaType)
  override def find(ideaId: Long, ideaType: BazaarIdeaType) = bazaarCommentDAO.find(ideaId, ideaType)
  override def create(ideaId: Long, ideaType: BazaarIdeaType, bazaarComment: BazaarComment) =
    bazaarCommentDAO.create(ideaId, ideaType, bazaarComment)
  override def delete(bazaarComment: BazaarComment, admin: Boolean) = bazaarCommentDAO.delete(bazaarComment, admin)

}
