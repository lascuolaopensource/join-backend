package models.daos.slick

import javax.inject.Inject
import models.daos.BazaarCommentDAO
import models.{BazaarComment, _}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext


class SlickBazaarCommentDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends BazaarCommentDAO with SlickDAO {

  import profile.api._

  private def fk(ideaType: BazaarIdeaType, t: BazaarCommentTable) = ideaType match {
    case BazaarLearnType | BazaarTeachType  => t.bazaarTeachLearnId
    case BazaarEventType                    => t.bazaarEventId
    case BazaarResearchType                 => t.bazaarResearchId
  }

  override def findSingle(id: Long, ideaType: BazaarIdeaType) = {
    val query = BazaarCommentQuery.filter(_.id === id).join(UsersQuery).on(_.userId === _.id)
    db run query.result.headOption map { option =>
      option.map { commentUser =>
        val (comment, user) = commentUser
        dbCommentToComment(comment).copy(firstName = Some(user.firstName), lastName = Some(user.lastName))
      }
    }
  }

  override def find(ideaId: Long, ideaType: BazaarIdeaType) = {
    val query = BazaarCommentQuery.filter(fk(ideaType, _) === ideaId).join(UsersQuery).on(_.userId === _.id)
    db run query.result map { comments =>
      comments.map { commentUser =>
        val (comment, user) = commentUser
        dbCommentToComment(comment).copy(firstName = Some(user.firstName), lastName = Some(user.lastName))
      }
    }
  }

  override def create(ideaId: Long, ideaType: BazaarIdeaType, bazaarComment: BazaarComment) = {
    val BazaarComment(_, userId, _, _, comment, _) = bazaarComment
    val now = currentTimestamp()
    val dbComment = DBBazaarComment(0, None, None, None, userId, comment, now)
    val dbCommentWithIdea = ideaType match {
      case BazaarLearnType | BazaarTeachType =>
        dbComment.copy(bazaarTeachLearnId = Some(ideaId))
      case BazaarEventType =>
        dbComment.copy(bazaarEventId = Some(ideaId))
      case BazaarResearchType =>
        dbComment.copy(bazaarResearchId = Some(ideaId))
    }

    val action = for {
      user <- UsersQuery.filter(_.id === userId).result.head
      id <- (BazaarCommentQuery returning BazaarCommentQuery.map(_.id)) += dbCommentWithIdea
    } yield (id, user.firstName, user.lastName)

    db run action map {
      case (id, firstName, lastName) =>
        bazaarComment.copy(id = id, firstName = Some(firstName), lastName = Some(lastName), createdAt = Some(now))
    }
  }

  override def delete(bazaarComment: BazaarComment, admin: Boolean) = {
    val preferenceQuery = {
      val q = BazaarPreferenceQuery.filter(_.wishId === bazaarComment.id)
      if (admin) q else q.filter(_.userId === bazaarComment.userId)
    }

    val commentQuery = {
      val q = BazaarCommentQuery.filter(_.id === bazaarComment.id)
      if (admin) q else q.filter(_.userId === bazaarComment.userId)
    }

    val action = for {
      _ <- preferenceQuery.map(_.wishId).update(None)
      n <- commentQuery.delete
    } yield n == 1

    db run action
  }

  override def count(ideaId: Long, ideaType: BazaarIdeaType) =
    db run BazaarCommentQuery.filter(fk(ideaType, _) === ideaId).length.result
}
