package models.daos.slick.tables

import java.sql.Timestamp

import models._
import slick.lifted.ProvenShape

import scala.language.implicitConversions


private[slick] object SlickBazaarEventTable {
  case class DBBazaarEvent(
    id: Long,
    title: String,
    creatorId: Long,
    valueDetails: String,
    motivation: String,
    bazaarAbstractIdeaId: Long,
    activityType: Int,
    isOrganizer: Boolean,
    bookingRequired: Boolean,
    requiredSpaces: Option[String],
    disabled: Boolean,
    createdAt: Timestamp,
    updatedAt: Timestamp,
    deletedAt: Option[Timestamp])
  {
    def updatableFields = (title, creatorId, valueDetails, motivation, bazaarAbstractIdeaId, activityType,
      isOrganizer, bookingRequired, requiredSpaces, updatedAt, deletedAt)
  }

  case class DBBazaarEventS(
    id: Long,
    title: String,
    creatorId: Long,
    valueDetails: String,
    motivation: String,
    bazaarAbstractIdeaId: Long,
    activityType: Int,
    isOrganizer: Boolean,
    bookingRequired: Boolean,
    requiredSpaces: Option[String],
    disabled: Boolean,
    createdAt: Timestamp,
    updatedAt: Timestamp,
    deletedAt: Option[Timestamp],
    score: Option[Double],
    views: Option[Int],
    agrees: Option[Int],
    wishes: Option[Int],
    favorites: Option[Int],
    comments: Option[Int])

  val dbBazaarEventS = (idea: DBBazaarEvent) =>
    DBBazaarEventS(
      id = idea.id,
      title = idea.title,
      creatorId = idea.creatorId,
      valueDetails = idea.valueDetails,
      motivation = idea.motivation,
      bazaarAbstractIdeaId = idea.bazaarAbstractIdeaId,
      activityType = idea.activityType,
      isOrganizer = idea.isOrganizer,
      bookingRequired = idea.bookingRequired,
      requiredSpaces = idea.requiredSpaces,
      disabled = idea.disabled,
      createdAt = idea.createdAt,
      updatedAt = idea.updatedAt,
      deletedAt = idea.deletedAt,
      score = None,
      views = None,
      agrees = None,
      wishes = None,
      favorites = None,
      comments = None
    )
}

private[slick] trait SlickBazaarEventTable extends SlickTable {

  import SlickBazaarEventTable._
  import profile.api._

  protected abstract class BazaarEventTableA[A](tag: Tag, tableName: String)
    extends Table[A](tag, tableName)
  {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def title: Rep[String] = column("title")
    def creatorId: Rep[Long] = column("creator_id")
    def valueDetails: Rep[String] = column("value_details")
    def motivation: Rep[String] = column("motivation")
    def bazaarAbstractIdeaId: Rep[Long] = column("bazaar_abstract_idea_id")
    def activityType: Rep[Int] = column("activity_type")
    def isOrganizer: Rep[Boolean] = column("is_organizer")
    def bookingRequired: Rep[Boolean] = column("booking_required")
    def requiredSpaces: Rep[Option[String]] = column("required_spaces")
    def disabled: Rep[Boolean] = column("disabled")
    def createdAt: Rep[Timestamp] = column("created_at")
    def updatedAt: Rep[Timestamp] = column("updated_at")
    def deletedAt: Rep[Option[Timestamp]] = column("deleted_at")
  }

  protected class BazaarEventTable(tag: Tag)
    extends BazaarEventTableA[DBBazaarEvent](tag, "bazaar_event")
  {
    def updatableFields = (title, creatorId, valueDetails, motivation, bazaarAbstractIdeaId, activityType,
      isOrganizer, bookingRequired, requiredSpaces, updatedAt, deletedAt)
    override def * : ProvenShape[DBBazaarEvent] =
      (id, title, creatorId, valueDetails, motivation, bazaarAbstractIdeaId, activityType,
        isOrganizer, bookingRequired, requiredSpaces, disabled, createdAt, updatedAt, deletedAt
      ).mapTo[DBBazaarEvent]
  }

  protected class BazaarEventTableS(tag: Tag)
    extends BazaarEventTableA[DBBazaarEventS](tag, "bazaar_event_s")
  {
    def score: Rep[Double] = column("score")
    def views: Rep[Int] = column("views")
    def agrees: Rep[Int] = column("agrees")
    def wishes: Rep[Int] = column("wishes")
    def favorites: Rep[Int] = column("favorites")
    def comments: Rep[Int] = column("comments")
    override def * : ProvenShape[DBBazaarEventS] =
      (id, title, creatorId, valueDetails, motivation, bazaarAbstractIdeaId, activityType,
        isOrganizer, bookingRequired, requiredSpaces, disabled, createdAt, updatedAt, deletedAt,
        score.asColumnOf[Option[Double]], views.asColumnOf[Option[Int]],
        agrees.asColumnOf[Option[Int]], wishes.asColumnOf[Option[Int]],
        favorites.asColumnOf[Option[Int]], comments.asColumnOf[Option[Int]]
      ).mapTo[DBBazaarEventS]
  }

  val BazaarEventQueryAll: TableQuery[BazaarEventTable] = TableQuery[BazaarEventTable]
  val BazaarEventQueryAllS: TableQuery[BazaarEventTableS] = TableQuery[BazaarEventTableS]

  val BazaarEventQuery: Query[BazaarEventTable, DBBazaarEvent, Seq] = BazaarEventQueryAll.filter(_.deletedAt.isEmpty)
  val BazaarEventQueryS: Query[BazaarEventTableS, DBBazaarEventS, Seq] = BazaarEventQueryAllS.filter(_.deletedAt.isEmpty)

  implicit def bazaarEventToDB(idea: BazaarEvent): DBBazaarEvent = DBBazaarEvent(
    id = idea.id,
    title = idea.title,
    creatorId = idea.creator.id,
    valueDetails = idea.valueDetails,
    motivation = idea.motivation,
    bazaarAbstractIdeaId = 0,
    activityType = idea.activityType,
    isOrganizer = idea.isOrganizer,
    bookingRequired = idea.bookingRequired,
    requiredSpaces = idea.requiredSpaces,
    disabled = false,
    createdAt = idea.createdAt,
    updatedAt = idea.updatedAt,
    deletedAt = None
  )

}
