package models.daos.slick.tables

import java.sql.Timestamp

import models.BazaarComment
import slick.lifted.ProvenShape

import scala.language.implicitConversions


trait SlickBazaarCommentTable extends SlickTable {

  import profile.api._

  case class DBBazaarComment(
    id: Long,
    bazaarTeachLearnId: Option[Long],
    bazaarEventId: Option[Long],
    bazaarResearchId: Option[Long],
    userId: Long,
    comment: String,
    createdAt: Timestamp)

  protected class BazaarCommentTable(tag: Tag) extends Table[DBBazaarComment](tag, "bazaar_comment") {
    def id: Rep[Long] = column("id", O.AutoInc, O.PrimaryKey)
    def bazaarTeachLearnId: Rep[Option[Long]] = column("bazaar_teach_learn_id")
    def bazaarEventId: Rep[Option[Long]] = column("bazaar_event_id")
    def bazaarResearchId: Rep[Option[Long]] = column("bazaar_research_id")
    def userId: Rep[Long] = column("user_id")
    def comment: Rep[String] = column("comment")
    def createdAt: Rep[Timestamp] = column("created_at")
    override def * : ProvenShape[DBBazaarComment] =
      (id, bazaarTeachLearnId, bazaarEventId, bazaarResearchId, userId, comment, createdAt) <>
        (DBBazaarComment.tupled, DBBazaarComment.unapply)
  }

  val BazaarCommentQuery: TableQuery[BazaarCommentTable] = TableQuery[BazaarCommentTable]

  implicit def dbCommentToComment(dbBazaarComment: DBBazaarComment): BazaarComment =
    BazaarComment(dbBazaarComment.id, dbBazaarComment.userId, None, None, dbBazaarComment.comment, Some(dbBazaarComment.createdAt))

}
