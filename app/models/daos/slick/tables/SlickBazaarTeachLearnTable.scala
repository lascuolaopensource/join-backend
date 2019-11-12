package models.daos.slick.tables

import java.sql.Timestamp

import models._
import slick.collection.heterogeneous.HNil
import slick.lifted.ProvenShape

import scala.language.implicitConversions


private[slick] object SlickBazaarTeachLearnTable {
  case class DBBazaarTeachLearn(
    id: Long,
    title: String,
    creatorId: Long,
    valueDetails: String,
    motivation: String,
    location: String,
    costs: Option[String],
    ideaType: Int,
    activityType: Option[Int],
    level: Option[Int],
    meetingDetails: Option[String],
    outputDetails: Option[String],
    bazaarAbstractIdeaId: Option[Long],
    disabled: Boolean,
    createdAt: Timestamp,
    updatedAt: Timestamp,
    deletedAt: Option[Timestamp])
  {
    def updatableFields = (title, creatorId, valueDetails, motivation, location, costs, ideaType,
      activityType, level, meetingDetails, outputDetails, bazaarAbstractIdeaId, updatedAt, deletedAt)
  }

  case class DBBazaarTeachLearnS(
     id: Long,
     title: String,
     creatorId: Long,
     valueDetails: String,
     motivation: String,
     location: String,
     costs: Option[String],
     ideaType: Int,
     activityType: Option[Int],
     level: Option[Int],
     meetingDetails: Option[String],
     outputDetails: Option[String],
     bazaarAbstractIdeaId: Option[Long],
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

  val dbBazaarTeachLearnS = (idea: DBBazaarTeachLearn) =>
    DBBazaarTeachLearnS(
      id = idea.id,
      title = idea.title,
      creatorId = idea.creatorId,
      valueDetails = idea.valueDetails,
      motivation = idea.motivation,
      location = idea.location,
      costs = idea.costs,
      ideaType = idea.ideaType,
      activityType = idea.activityType,
      level = idea.level,
      meetingDetails = idea.meetingDetails,
      outputDetails = idea.outputDetails,
      bazaarAbstractIdeaId = idea.bazaarAbstractIdeaId,
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

private[slick] trait SlickBazaarTeachLearnTable extends SlickTable {

  import SlickBazaarTeachLearnTable._
  import profile.api._

  protected abstract class BazaarTeachLearnTableA[A](tag: Tag, tableName: String)
    extends Table[A](tag, tableName)
  {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def title: Rep[String] = column("title")
    def creatorId: Rep[Long] = column("creator_id")
    def valueDetails: Rep[String] = column("value_details")
    def motivation: Rep[String] = column("motivation")
    def location: Rep[String] = column("location")
    def costs: Rep[Option[String]] = column("costs")
    def ideaType: Rep[Int] = column("type")
    def activityType: Rep[Option[Int]] = column("activity_type")
    def level: Rep[Option[Int]] = column("level")
    def meetingDetails: Rep[Option[String]] = column("meeting_details")
    def outputDetails: Rep[Option[String]] = column("output_details")
    def bazaarAbstractIdeaId: Rep[Option[Long]] = column("bazaar_abstract_idea_id")
    def disabled: Rep[Boolean] = column("disabled")
    def createdAt: Rep[Timestamp] = column("created_at")
    def updatedAt: Rep[Timestamp] = column("updated_at")
    def deletedAt: Rep[Option[Timestamp]] = column("deleted_at")
  }

  protected class BazaarTeachLearnTable(tag: Tag)
    extends BazaarTeachLearnTableA[DBBazaarTeachLearn](tag, "bazaar_teach_learn")
  {
    def updatableFields = (title, creatorId, valueDetails, motivation, location, costs, ideaType,
      activityType, level, meetingDetails, outputDetails, bazaarAbstractIdeaId, updatedAt, deletedAt)
    override def * : ProvenShape[DBBazaarTeachLearn] =
      (id :: title :: creatorId :: valueDetails :: motivation :: location :: costs :: ideaType ::
        activityType :: level :: meetingDetails :: outputDetails :: bazaarAbstractIdeaId ::
        disabled :: createdAt :: updatedAt :: deletedAt :: HNil
      ).mapTo[DBBazaarTeachLearn]
  }

  protected class BazaarTeachLearnTableS(tag: Tag)
    extends BazaarTeachLearnTableA[DBBazaarTeachLearnS](tag, "bazaar_teach_learn_s")
  {
    def score: Rep[Double] = column("score")
    def views: Rep[Int] = column("views")
    def agrees: Rep[Int] = column("agrees")
    def wishes: Rep[Int] = column("wishes")
    def favorites: Rep[Int] = column("favorites")
    def comments: Rep[Int] = column("comments")
    override def * : ProvenShape[DBBazaarTeachLearnS] =
      (id :: title :: creatorId :: valueDetails :: motivation :: location :: costs :: ideaType ::
        activityType :: level :: meetingDetails :: outputDetails :: bazaarAbstractIdeaId ::
        disabled :: createdAt :: updatedAt :: deletedAt :: score.asColumnOf[Option[Double]] ::
        views.asColumnOf[Option[Int]] ::
        agrees.asColumnOf[Option[Int]] :: wishes.asColumnOf[Option[Int]] ::
        favorites.asColumnOf[Option[Int]] :: comments.asColumnOf[Option[Int]] :: HNil
      ).mapTo[DBBazaarTeachLearnS]
  }

  protected val BazaarTeachLearnQuery: TableQuery[BazaarTeachLearnTable] = TableQuery[BazaarTeachLearnTable]
  protected val BazaarTeachLearnQueryS: TableQuery[BazaarTeachLearnTableS] = TableQuery[BazaarTeachLearnTableS]

  type BazaarTeachLearnQuery = Query[BazaarTeachLearnTable, DBBazaarTeachLearn, Seq]
  type BazaarTeachLearnQueryS = Query[BazaarTeachLearnTableS, DBBazaarTeachLearnS, Seq]

  val BazaarLearnQueryAll: BazaarTeachLearnQuery = BazaarTeachLearnQuery.filter(_.ideaType === 0)
  val BazaarTeachQueryAll: BazaarTeachLearnQuery = BazaarTeachLearnQuery.filter(_.ideaType === 1)

  val BazaarLearnQueryAllS: BazaarTeachLearnQueryS = BazaarTeachLearnQueryS.filter(_.ideaType === 0)
  val BazaarTeachQueryAllS: BazaarTeachLearnQueryS = BazaarTeachLearnQueryS.filter(_.ideaType === 1)

  val BazaarLearnQuery: BazaarTeachLearnQuery = BazaarLearnQueryAll.filter(_.deletedAt.isEmpty)
  val BazaarTeachQuery: BazaarTeachLearnQuery = BazaarTeachQueryAll.filter(_.deletedAt.isEmpty)
  val BazaarLearnQueryS: BazaarTeachLearnQueryS = BazaarLearnQueryAllS.filter(_.deletedAt.isEmpty)
  val BazaarTeachQueryS: BazaarTeachLearnQueryS = BazaarTeachQueryAllS.filter(_.deletedAt.isEmpty)

  implicit def bazaarLearnToDB(bazaarLearn: BazaarLearn): DBBazaarTeachLearn = DBBazaarTeachLearn(
    id = bazaarLearn.id,
    title = bazaarLearn.title,
    creatorId = bazaarLearn.creator.id,
    valueDetails = bazaarLearn.valueDetails,
    motivation = bazaarLearn.motivation,
    location = bazaarLearn.location match {
      case AtSOS => "SOS"
      case CustomLocation(location) => location
    },
    costs = bazaarLearn.costs,
    ideaType = 0,
    activityType = None,
    level = None,
    meetingDetails = None,
    outputDetails = None,
    bazaarAbstractIdeaId = None,
    disabled = false,
    createdAt = bazaarLearn.createdAt,
    updatedAt = bazaarLearn.updatedAt,
    deletedAt = None
  )

  implicit def bazaarTeachToDB(bazaarTeach: BazaarTeach): DBBazaarTeachLearn = DBBazaarTeachLearn(
    id = bazaarTeach.id,
    title = bazaarTeach.title,
    creatorId = bazaarTeach.creator.id,
    valueDetails = bazaarTeach.valueDetails,
    motivation = bazaarTeach.motivation,
    location = bazaarTeach.location match {
      case AtSOS => "SOS"
      case CustomLocation(location) => location
    },
    costs = bazaarTeach.costs,
    ideaType = 1,
    activityType = Some(bazaarTeach.activityType),
    level = Some(bazaarTeach.level),
    meetingDetails = Some(bazaarTeach.meetingDetails),
    outputDetails = Some(bazaarTeach.outputDetails),
    bazaarAbstractIdeaId = None,
    disabled = false,
    createdAt = bazaarTeach.createdAt,
    updatedAt = bazaarTeach.updatedAt,
    deletedAt = None
  )

}
