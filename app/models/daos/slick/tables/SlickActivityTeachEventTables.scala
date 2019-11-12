package models.daos.slick.tables

import java.sql.{Date, Time, Timestamp}

import models.daos.slick.tables.SlickActivityTeachEventTables._
import slick.collection.heterogeneous.HNil
import slick.collection.heterogeneous.syntax._
import slick.lifted.PrimaryKey


private[slick] object SlickActivityTeachEventTables {

  case class DBActivityTeachEvent(
    id: Long,
    isTeach: Boolean,
    coverExt: String,
    galleryId: Long,
    level: Option[Int],
    activityType: Int,
    costs: Option[Double],
    payments: Boolean,
    deadline: Option[Date],
    minParticipants: Option[Int],
    maxParticipants: Option[Int],
    recurringDays: Option[Int],
    recurringEvery: Option[Int],
    recurringEntity: Option[Int],
    recurringHours: Option[Int],
    totalDays: Option[Int],
    totalHours: Option[Int],
    startTime: Option[Timestamp],
    teachCategory: Option[Int],
    bazaarTeachLearnId: Option[Long],
    bazaarEventId: Option[Long],
    createdAt: Timestamp,
    updatedAt: Timestamp)
  {
    lazy val updatableFields = (isTeach, coverExt, galleryId, level, activityType, costs, payments, deadline,
      minParticipants, maxParticipants, recurringDays, recurringEvery, recurringEntity, recurringHours,
      totalDays, totalHours, startTime, teachCategory, bazaarTeachLearnId, bazaarEventId, updatedAt)
  }

  case class DBActivityTeachEventT(
    activityId: Long,
    language: String,
    title: String,
    description: String,
    outputType: String,
    outputDescription: Option[String],
    program: String)

  case class DBActivityTeachEventSkill(
    activityId: Long,
    skillId: Long,
    required: Boolean,
    acquired: Boolean)

  case class DBActivityTeachEventDate(
    id: Long,
    activityId: Long,
    date: Date,
    startTime: Time,
    endTime: Time)

  case class DBActivityTeachEventGuest(
    id: Long,
    activityId: Long,
    userId: Option[Long],
    firstName: String,
    lastName: String)

  case class DBActivityTeachEventGuestT(
    guestId: Long,
    language: String,
    title: String,
    bio: String)

  case class DBActivityTeachEventSub(
    activityId: Long,
    userId: Long,
    paymentInfoId: Option[Long],
    createdAt: Timestamp)

}


private[slick] trait SlickActivityTeachEventTables extends SlickActivityCommonTables {

  import profile.api._

  protected class ActivityTeachEventTable(tag: Tag) extends Table[DBActivityTeachEvent](tag, "activity_teach_event") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def isTeach: Rep[Boolean] = column("is_teach")
    def coverExt: Rep[String] = column("cover_ext")
    def galleryId: Rep[Long] = column("gallery_id")
    def level: Rep[Option[Int]] = column("level")
    def activityType: Rep[Int] = column("activity_type")
    def costs: Rep[Option[Double]] = column("costs")
    def payments: Rep[Boolean] = column("payments")
    def deadline: Rep[Option[Date]] = column("deadline")
    def minParticipants: Rep[Option[Int]] = column("min_participants")
    def maxParticipants: Rep[Option[Int]] = column("max_participants")
    def recurringDays: Rep[Option[Int]] = column("recurring_days")
    def recurringEvery: Rep[Option[Int]] = column("recurring_every")
    def recurringEntity: Rep[Option[Int]] = column("recurring_entity")
    def recurringHours: Rep[Option[Int]] = column("recurring_hours")
    def totalDays: Rep[Option[Int]] = column("total_days")
    def totalHours: Rep[Option[Int]] = column("total_hours")
    def startTime: Rep[Option[Timestamp]] = column("start_time")
    def teachCategory: Rep[Option[Int]] = column("teach_category")
    def bazaarTeachLearnId: Rep[Option[Long]] = column("bazaar_teach_learn_id")
    def bazaarEventId: Rep[Option[Long]] = column("bazaar_event_id")
    def createdAt: Rep[Timestamp] = column("created_at")
    def updatedAt: Rep[Timestamp] = column("updated_at")
    override def * = id :: isTeach :: coverExt :: galleryId :: level :: activityType :: costs :: payments :: deadline ::
      minParticipants :: maxParticipants :: recurringDays :: recurringEvery :: recurringEntity :: recurringHours ::
      totalDays :: totalHours :: startTime :: teachCategory :: bazaarTeachLearnId :: bazaarEventId :: createdAt :: updatedAt :: HNil <>
      ({
        case id :: isTeach :: coverExt :: galleryId :: level :: activityType :: costs :: payments :: deadline ::
             minParticipants :: maxParticipants :: recurringDays :: recurringEvery :: recurringEntity :: recurringHours ::
             totalDays :: totalHours :: startTime :: teachCategory :: bazaarTeachLearnId :: bazaarEventId :: createdAt :: updatedAt :: HNil =>
          DBActivityTeachEvent(id, isTeach, coverExt, galleryId, level, activityType, costs, payments, deadline,
            minParticipants, maxParticipants, recurringDays, recurringEvery, recurringEntity, recurringHours,
            totalDays, totalHours, startTime, teachCategory, bazaarTeachLearnId, bazaarEventId, createdAt, updatedAt)
      }, (data: DBActivityTeachEvent) => data match {
        case DBActivityTeachEvent(id, isTeach, coverExt, galleryId, level, activityType, costs, payments, deadline,
              minParticipants, maxParticipants, recurringDays, recurringEvery, recurringEntity, recurringHours,
              totalDays, totalHours, startTime, teachCategory, bazaarTeachLearnId, bazaarEventId, createdAt, updatedAt) =>
          Some(id :: isTeach :: coverExt :: galleryId :: level :: activityType :: costs :: payments :: deadline ::
            minParticipants :: maxParticipants :: recurringDays :: recurringEvery :: recurringEntity :: recurringHours ::
            totalDays :: totalHours :: startTime :: teachCategory :: bazaarTeachLearnId :: bazaarEventId :: createdAt :: updatedAt :: HNil)
      })

    def updatableFields = (isTeach, coverExt, galleryId, level, activityType, costs, payments, deadline,
      minParticipants, maxParticipants, recurringDays, recurringEvery, recurringEntity, recurringHours,
      totalDays, totalHours, startTime, teachCategory, bazaarTeachLearnId, bazaarEventId, updatedAt)
  }

  protected val ActivityTeachEventQuery = TableQuery[ActivityTeachEventTable]


  protected class ActivityTeachEventTTable(tag: Tag) extends Table[DBActivityTeachEventT](tag, "activity_teach_event_t") {
    def activityId: Rep[Long] = column("activity_id")
    def language: Rep[String] = column("language")
    def title: Rep[String] = column("title")
    def description: Rep[String] = column("description")
    def outputType: Rep[String] = column("output_type")
    def outputDescription: Rep[Option[String]] = column("output_description")
    def program: Rep[String] = column("program")
    def pk: PrimaryKey = primaryKey("pk", (activityId, language))
    override def * = (activityId, language, title, description, outputType, outputDescription, program) <>
      (DBActivityTeachEventT.tupled, DBActivityTeachEventT.unapply)
  }

  protected val ActivityTeachEventTQuery = TableQuery[ActivityTeachEventTTable]


  protected class ActivityTeachEventTopicTable(tag: Tag) extends SlickActivityTopicTable(tag, "activity_teach_event_topic") {
    def activityId: Rep[Long] = column("activity_id")
    def topicId: Rep[Long] = column("topic_id")
    def pk: PrimaryKey = primaryKey("pk", (activityId, topicId))
  }

  protected val ActivityTeachEventTopicQuery = TableQuery[ActivityTeachEventTopicTable]


  protected class ActivityTeachEventAudienceTable(tag: Tag) extends Table[(Long, Int)](tag, "activity_teach_event_audience") {
    def activityId: Rep[Long] = column("activity_id")
    def audience: Rep[Int] = column("audience")
    def pk: PrimaryKey = primaryKey("pk", (activityId, audience))
    override def * = (activityId, audience)
  }

  protected val ActivityTeachEventAudienceQuery = TableQuery[ActivityTeachEventAudienceTable]


  protected class ActivityTeachEventSkillTable(tag: Tag) extends Table[DBActivityTeachEventSkill](tag, "activity_teach_event_skill") {
    def activityId: Rep[Long] = column("activity_id")
    def skillId: Rep[Long] = column("skill_id")
    def required: Rep[Boolean] = column("required")
    def acquired: Rep[Boolean] = column("acquired")
    def pk: PrimaryKey = primaryKey("pk", (activityId, skillId))
    override def * = (activityId, skillId, required, acquired) <> (DBActivityTeachEventSkill.tupled, DBActivityTeachEventSkill.unapply)
  }

  protected val ActivityTeachEventSkillQuery = TableQuery[ActivityTeachEventSkillTable]


  protected class ActivityTeachEventDateTable(tag: Tag) extends Table[DBActivityTeachEventDate](tag, "activity_teach_event_date") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def activityId: Rep[Long] = column("activity_id")
    def date: Rep[Date] = column("date")
    def startTime: Rep[Time] = column("start_time")
    def endTime: Rep[Time] = column("end_time")
    override def * = (id, activityId, date, startTime, endTime) <>
      (DBActivityTeachEventDate.tupled, DBActivityTeachEventDate.unapply)
  }

  protected val ActivityTeachEventDateQuery = TableQuery[ActivityTeachEventDateTable]


  protected class ActivityTeachEventGuestTable(tag: Tag) extends Table[DBActivityTeachEventGuest](tag, "activity_teach_event_guest") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def activityId: Rep[Long] = column("activity_id")
    def userId: Rep[Option[Long]] = column("user_id")
    def firstName: Rep[String] = column("first_name")
    def lastName: Rep[String] = column("last_name")
    override def * = (id, activityId, userId, firstName, lastName) <>
      (DBActivityTeachEventGuest.tupled, DBActivityTeachEventGuest.unapply)
  }

  protected val ActivityTeachEventGuestQuery = TableQuery[ActivityTeachEventGuestTable]


  protected class ActivityTeachEventGuestTTable(tag: Tag) extends Table[DBActivityTeachEventGuestT](tag, "activity_teach_event_guest_t") {
    def guestId: Rep[Long] = column("activity_teach_event_guest_id")
    def language: Rep[String] = column("language")
    def title: Rep[String] = column("title")
    def bio: Rep[String] = column("bio")
    def pk: PrimaryKey = primaryKey("pk", (guestId, language))
    override def * = (guestId, language, title, bio) <> (DBActivityTeachEventGuestT.tupled, DBActivityTeachEventGuestT.unapply)
  }

  protected val ActivityTeachEventGuestTQuery = TableQuery[ActivityTeachEventGuestTTable]


  protected class ActivityTeachEventFavoriteTable(tag: Tag) extends SlickActivityFavoriteTable(tag, "activity_teach_event_favorite") {
    def activityId: Rep[Long] = column("activity_id")
    def userId: Rep[Long] = column("user_id")
    def pk = primaryKey("activity_teach_event_favorite_pkey", (activityId, userId))
  }

  protected val ActivityTeachEventFavoriteQuery = TableQuery[ActivityTeachEventFavoriteTable]


  protected class ActivityTeachEventSubTable(tag: Tag) extends Table[DBActivityTeachEventSub](tag, "activity_teach_event_sub") {
    def activityId: Rep[Long] = column("activity_id")
    def userId: Rep[Long] = column("user_id")
    def paymentInfoId: Rep[Option[Long]] = column("payment_info_id")
    def createdAt: Rep[Timestamp] = column("created_at")
    def pk = primaryKey("activity_teach_event_sub_pkey", (activityId, userId))
    override def * =
      (activityId, userId, paymentInfoId, createdAt) <> (DBActivityTeachEventSub.tupled, DBActivityTeachEventSub.unapply)
  }

  protected val ActivityTeachEventSubQuery = TableQuery[ActivityTeachEventSubTable]

}
