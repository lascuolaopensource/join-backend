package models.daos.slick.tables

import java.sql.{Date, Timestamp}

import models.ActivityResearchTeam
import models.daos.slick.tables.SlickActivityResearchTables._
import slick.lifted.PrimaryKey


private[slick] object SlickActivityResearchTables {

  case class DBActivityResearch(
    id: Long,
    coverExt: String,
    galleryId: Long,
    organizationName: Option[String],
    deadline: Date,
    startDate: Date,
    duration: Int,
    projectLink: Option[String],
    bazaarResearchId: Option[Long],
    createdAt: Timestamp,
    updatedAt: Timestamp)
  {
    val updatableFields = (coverExt, galleryId, organizationName, deadline, startDate, duration, projectLink, updatedAt)
  }

  case class DBActivityResearchT(
    activityId: Long,
    language: String,
    title: String,
    valueDetails: String,
    motivation: String)

  case class DBActivityResearchRole(
    id: Long,
    activityResearchId: Long,
    people: Int)

  case class DBActivityResearchApp(
    activityResearchRoleId: Long,
    userId: Long,
    motivation: Option[String],
    createdAt: Timestamp)

  case class DBActivityResearchTeam(
    id: Long,
    activityResearchId: Long,
    userId: Option[Long],
    firstName: String,
    lastName: String,
    title: String)
  {
    def toModel = ActivityResearchTeam(id, userId, firstName, lastName, title)
  }

}


private[slick] trait SlickActivityResearchTables extends SlickActivityCommonTables {

  import profile.api._

  protected class ActivityResearchTable(tag: Tag) extends Table[DBActivityResearch](tag, "activity_research") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def coverExt: Rep[String] = column("cover_ext")
    def galleryId: Rep[Long] = column("gallery_id")
    def organizationName: Rep[Option[String]] = column("organization_name")
    def deadline: Rep[Date] = column("deadline")
    def startDate: Rep[Date] = column("start_date")
    def duration: Rep[Int] = column("duration")
    def projectLink: Rep[Option[String]] = column("project_link")
    def bazaarResearchId: Rep[Option[Long]] = column("bazaar_research_id")
    def createdAt: Rep[Timestamp] = column("created_at")
    def updatedAt: Rep[Timestamp] = column("updated_at")
    override def * = (id, coverExt, galleryId, organizationName, deadline, startDate, duration,
      projectLink, bazaarResearchId, createdAt, updatedAt) <> (DBActivityResearch.tupled, DBActivityResearch.unapply)

    def updatableFields = (coverExt, galleryId, organizationName, deadline, startDate, duration, projectLink, updatedAt)
  }

  protected val ActivityResearchQuery = TableQuery[ActivityResearchTable]


  protected class ActivityResearchTTable(tag: Tag) extends Table[DBActivityResearchT](tag, "activity_research_t") {
    def activityId: Rep[Long] = column("activity_id")
    def language: Rep[String] = column("language")
    def title: Rep[String] = column("title")
    def valueDetails: Rep[String] = column("value_details")
    def motivation: Rep[String] = column("motivation")
    def pk: PrimaryKey = primaryKey("pk", (activityId, language))
    override def * = (activityId, language, title, valueDetails, motivation) <>
      (DBActivityResearchT.tupled, DBActivityResearchT.unapply)
  }

  protected val ActivityResearchTQuery = TableQuery[ActivityResearchTTable]


  protected class ActivityResearchTopicTable(tag: Tag)
    extends SlickActivityTopicTable(tag, "activity_research_topic")
  {
    def activityId: Rep[Long] = column("activity_id")
    def topicId: Rep[Long] = column("topic_id")
  }

  protected val ActivityResearchTopicQuery = TableQuery[ActivityResearchTopicTable]


  protected class ActivityResearchRoleTable(tag: Tag)
    extends Table[DBActivityResearchRole](tag, "activity_research_role")
  {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def activityResearchId: Rep[Long] = column("activity_research_id")
    def people: Rep[Int] = column("people")
    override def * = (id, activityResearchId, people) <>
      (DBActivityResearchRole.tupled, DBActivityResearchRole.unapply)
  }

  protected val ActivityResearchRoleQuery = TableQuery[ActivityResearchRoleTable]


  protected class ActivityResearchRoleSkillTable(tag: Tag)
    extends Table[(Long, Long)](tag, "activity_research_role_skill")
  {
    def activityResearchRoleId: Rep[Long] = column("activity_research_role_id")
    def skillId: Rep[Long] = column("skill_id")
    override def * = (activityResearchRoleId, skillId)
  }

  protected val ActivityResearchRoleSkillQuery = TableQuery[ActivityResearchRoleSkillTable]


  protected class ActivityResearchRoleAppTable(tag: Tag)
    extends Table[DBActivityResearchApp](tag, "activity_research_role_application")
  {
    def activityResearchRoleId: Rep[Long] = column("activity_research_role_id")
    def userId: Rep[Long] = column("user_id")
    def motivation: Rep[Option[String]] = column("motivation")
    def createdAt: Rep[Timestamp] = column("created_at")
    def pk = primaryKey("activity_research_role_application_pkey", (activityResearchRoleId, userId))
    override def * = (activityResearchRoleId, userId, motivation, createdAt) <>
      (DBActivityResearchApp.tupled, DBActivityResearchApp.unapply)
  }

  protected val ActivityResearchRoleAppQuery = TableQuery[ActivityResearchRoleAppTable]


  protected class ActivityResearchTeamTable(tag: Tag)
    extends Table[DBActivityResearchTeam](tag, "activity_research_team")
  {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def activityResearchId = column[Long]("activity_research_id")
    def userId = column[Long]("user_id")
    def firstName = column[String]("first_name")
    def lastName = column[String]("last_name")
    def title = column[String]("title")
    override def * = (id, activityResearchId, userId.?, firstName, lastName, title) <>
      (DBActivityResearchTeam.tupled, DBActivityResearchTeam.unapply)
  }

  protected val ActivityResearchTeamQuery = TableQuery[ActivityResearchTeamTable]


  protected class ActivityResearchFavoriteTable(tag: Tag)
    extends SlickActivityFavoriteTable(tag, "activity_research_favorite")
  {
    override def activityId: Rep[Long] = column("activity_id")
    override def userId: Rep[Long] = column("user_id")
    def pk = primaryKey("activity_research_favorite_pkey", (activityId, userId))
  }

  protected val ActivityResearchFavoriteQuery = TableQuery[ActivityResearchFavoriteTable]

}
