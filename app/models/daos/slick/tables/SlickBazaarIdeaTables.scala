package models.daos.slick.tables

import java.sql.{Date, Time}

import models._
import slick.lifted.{PrimaryKey, ProvenShape}

import scala.language.implicitConversions


trait SlickBazaarIdeaTables extends SlickTable {

  import profile.api._


  case class DBBazaarIdeaAudience(bazaarAbstractIdeaId: Long, audienceType: Int)

  protected class BazaarIdeaAudienceTable(tag: Tag) extends Table[DBBazaarIdeaAudience](tag, "bazaar_idea_audience") {
    def bazaarAbstractIdeaId: Rep[Long] = column("bazaar_abstract_idea_id")
    def audienceType: Rep[Int] = column("audience_type")
    def pk: PrimaryKey = primaryKey("pk", (bazaarAbstractIdeaId, audienceType))
    override def * : ProvenShape[DBBazaarIdeaAudience] =
      (bazaarAbstractIdeaId, audienceType) <> (DBBazaarIdeaAudience.tupled, DBBazaarIdeaAudience.unapply)
  }

  val BazaarIdeaAudienceQuery: TableQuery[BazaarIdeaAudienceTable] = TableQuery[BazaarIdeaAudienceTable]


  case class DBBazaarIdeaFunding(bazaarAbstractIdeaId: Long, fundingType: Int)

  protected class BazaarIdeaFundingTable(tag: Tag) extends Table[DBBazaarIdeaFunding](tag, "bazaar_idea_funding") {
    def bazaarAbstractIdeaId: Rep[Long] = column("bazaar_abstract_idea_id")
    def fundingType: Rep[Int] = column("funding_type")
    def pk: PrimaryKey = primaryKey("pk", (bazaarAbstractIdeaId, fundingType))
    override def * : ProvenShape[DBBazaarIdeaFunding] =
      (bazaarAbstractIdeaId, fundingType) <> (DBBazaarIdeaFunding.tupled, DBBazaarIdeaFunding.unapply)
  }

  val BazaarIdeaFundingQuery: TableQuery[BazaarIdeaFundingTable] = TableQuery[BazaarIdeaFundingTable]


  case class DBBazaarIdeaMeetingDuration(id: Long, bazaarAbstractIdeaId: Long, numberDays: Int, numberHours: Int)

  protected class BazaarIdeaMeetingDurationTable(tag: Tag) extends Table[DBBazaarIdeaMeetingDuration](tag, "bazaar_idea_meeting_duration") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def bazaarAbstractIdeaId: Rep[Long] = column("bazaar_abstract_idea_id")
    def numberDays: Rep[Int] = column("number_days")
    def numberHours: Rep[Int] = column("number_hours")
    override def * : ProvenShape[DBBazaarIdeaMeetingDuration] =
      (id, bazaarAbstractIdeaId, numberDays, numberHours) <> (DBBazaarIdeaMeetingDuration.tupled, DBBazaarIdeaMeetingDuration.unapply)
  }

  val BazaarIdeaMeetingDurationQuery: TableQuery[BazaarIdeaMeetingDurationTable] = TableQuery[BazaarIdeaMeetingDurationTable]


  case class DBBazaarIdeaDate(id: Long, bazaarAbstractIdeaId: Long, date: Date, startTime: Time, endTime: Time)

  protected class BazaarIdeaDateTable(tag: Tag) extends Table[DBBazaarIdeaDate](tag, "bazaar_idea_date") {
    def id: Rep[Long] = column("id", O.AutoInc, O.PrimaryKey)
    def bazaarAbstractIdeaId: Rep[Long] = column("bazaar_abstract_idea_id")
    def date: Rep[Date] = column("date")
    def startTime: Rep[Time] = column("start_time")
    def endTime: Rep[Time] = column("end_time")
    override def * : ProvenShape[DBBazaarIdeaDate] =
      (id, bazaarAbstractIdeaId, date, startTime, endTime) <> (DBBazaarIdeaDate.tupled, DBBazaarIdeaDate.unapply)
  }

  val BazaarIdeaDateQuery: TableQuery[BazaarIdeaDateTable] = TableQuery[BazaarIdeaDateTable]


  case class DBBazaarIdeaGuest(
    id: Long,
    bazaarEventId: Option[Long],
    bazaarTeachLearnId: Option[Long],
    userId: Option[Long],
    firstName: Option[String],
    lastName: Option[String],
    title: Option[String],
    tutor: Boolean)

  protected class BazaarIdeaGuestTable(tag: Tag) extends Table[DBBazaarIdeaGuest](tag, "bazaar_idea_guests") {
    def id: Rep[Long] = column("id", O.AutoInc, O.PrimaryKey)
    def bazaarEventId: Rep[Option[Long]] = column("bazaar_event_id")
    def bazaarTeachLearnId: Rep[Option[Long]] = column("bazaar_teach_learn_id")
    def userId: Rep[Option[Long]] = column("user_id")
    def firstName: Rep[Option[String]] = column("first_name")
    def lastName: Rep[Option[String]] = column("last_name")
    def title: Rep[Option[String]] = column("title")
    def tutor: Rep[Boolean] = column("tutor")
    override def * : ProvenShape[DBBazaarIdeaGuest] =
      (id, bazaarEventId, bazaarTeachLearnId, userId, firstName, lastName, title, tutor) <>
        (DBBazaarIdeaGuest.tupled, DBBazaarIdeaGuest.unapply)
  }

  val BazaarIdeaGuestQuery: TableQuery[BazaarIdeaGuestTable] = TableQuery[BazaarIdeaGuestTable]


  case class DBBazaarIdeaTopic(
    id: Long,
    topicId: Long,
    bazaarEventId: Option[Long],
    bazaarTeachLearnId: Option[Long],
    bazaarResearchId: Option[Long]
  )

  protected class BazaarIdeaTopicTable(tag: Tag) extends Table[DBBazaarIdeaTopic](tag, "bazaar_idea_topic") {
    def id: Rep[Long] = column("id", O.AutoInc, O.PrimaryKey)
    def topicId: Rep[Long] = column("topic_id")
    def bazaarEventId: Rep[Option[Long]] = column("bazaar_event_id")
    def bazaarTeachLearnId: Rep[Option[Long]] = column("bazaar_teach_learn_id")
    def bazaarResearchId: Rep[Option[Long]] = column("bazaar_research_id")
    override def * : ProvenShape[DBBazaarIdeaTopic] =
      (id, topicId, bazaarEventId, bazaarTeachLearnId, bazaarResearchId) <> (DBBazaarIdeaTopic.tupled, DBBazaarIdeaTopic.unapply)
  }

  val BazaarIdeaTopicQuery: TableQuery[BazaarIdeaTopicTable] = TableQuery[BazaarIdeaTopicTable]




  implicit def dbFundingToFunding(dbBazaarIdeaFunding: DBBazaarIdeaFunding): BazaarIdeaFunding =
    dbBazaarIdeaFunding.fundingType match {
      case 0 => TuitionFee
      case 1 => Sponsor
      case 2 => Grant
      case 3 => Crowdfunding
      case 4 => SelfFinanced
    }

  implicit def fundingToDB(funding: BazaarIdeaFunding): DBBazaarIdeaFunding = DBBazaarIdeaFunding(
    bazaarAbstractIdeaId = 0,
    fundingType = funding match {
      case TuitionFee => 0
      case Sponsor => 1
      case Grant => 2
      case Crowdfunding => 3
      case SelfFinanced => 4
    }
  )

  implicit def dbBazaarDateToDate(dbBazaarIdeaDate: DBBazaarIdeaDate): SOSDate =
    SOSDate(dbBazaarIdeaDate.id, dbBazaarIdeaDate.date, dbBazaarIdeaDate.startTime, dbBazaarIdeaDate.endTime)

  implicit def bazaarGuestToDB(bazaarIdeaGuest: BazaarIdeaGuest): DBBazaarIdeaGuest =
    DBBazaarIdeaGuest(
      id = bazaarIdeaGuest.id,
      bazaarEventId = None,
      bazaarTeachLearnId = None,
      userId = bazaarIdeaGuest.userId,
      firstName = if (bazaarIdeaGuest.userId.isDefined) None else Some(bazaarIdeaGuest.firstName),
      lastName = if (bazaarIdeaGuest.userId.isDefined) None else Some(bazaarIdeaGuest.lastName),
      title = if (bazaarIdeaGuest.userId.isDefined) None else Some(bazaarIdeaGuest.title),
      tutor = false
    )

}
