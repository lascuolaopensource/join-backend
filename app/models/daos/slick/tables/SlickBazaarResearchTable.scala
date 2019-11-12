package models.daos.slick.tables

import java.sql.Timestamp

import models.BazaarResearch

import scala.language.implicitConversions


private[slick] object SlickBazaarResearchTable {
  case class DBBazaarResearch(
    id: Long,
    title: String,
    creatorId: Long,
    valueDetails: String,
    motivation: String,
    organizationName: Option[String],
    requiredResources: String,
    deadline: Int,
    duration: Int,
    disabled: Boolean,
    createdAt: Timestamp,
    updatedAt: Timestamp)
  {
    def updatableFields = (title, creatorId, valueDetails, motivation, organizationName,
      requiredResources, deadline, duration, updatedAt)
  }

  case class DBBazaarResearchS(
     id: Long,
     title: String,
     creatorId: Long,
     valueDetails: String,
     motivation: String,
     organizationName: Option[String],
     requiredResources: String,
     deadline: Int,
     duration: Int,
     disabled: Boolean,
     createdAt: Timestamp,
     updatedAt: Timestamp,
     score: Option[Double],
     views: Option[Int],
     agrees: Option[Int],
     wishes: Option[Int],
     favorites: Option[Int],
     comments: Option[Int])

  val dbBazaarResearchS = (idea: DBBazaarResearch) =>
    DBBazaarResearchS(
      id = idea.id,
      title = idea.title,
      creatorId = idea.creatorId,
      valueDetails = idea.valueDetails,
      motivation = idea.motivation,
      organizationName = idea.organizationName,
      requiredResources = idea.requiredResources,
      deadline = idea.deadline,
      duration = idea.duration,
      disabled = idea.disabled,
      createdAt = idea.createdAt,
      updatedAt = idea.updatedAt,
      score = None,
      views = None,
      agrees = None,
      wishes = None,
      favorites = None,
      comments = None
    )

  case class DBBazaarResearchRole(
    id: Long,
    bazaarResearchId: Long,
    people: Int
  )

  case class DBBazaarResearchSkill(
    bazaarResearchRoleId: Long,
    skillId: Long
  )
}


private[slick] trait SlickBazaarResearchTable extends SlickTable {

  import SlickBazaarResearchTable._
  import profile.api._

  protected abstract class BazaarResearchTableA[A](tag: Tag, tableName: String)
    extends Table[A](tag, tableName)
  {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def title: Rep[String] = column("title")
    def creatorId: Rep[Long] = column("creator_id")
    def valueDetails: Rep[String] = column("value_details")
    def motivation: Rep[String] = column("motivation")
    def organizationName: Rep[Option[String]] = column("organization_name")
    def requiredResources: Rep[String] = column("required_resources")
    def deadline: Rep[Int] = column("deadline")
    def duration: Rep[Int] = column("duration")
    def disabled: Rep[Boolean] = column("disabled")
    def createdAt: Rep[Timestamp] = column("created_at")
    def updatedAt: Rep[Timestamp] = column("updated_at")
  }

  protected class BazaarResearchTable(tag: Tag)
    extends BazaarResearchTableA[DBBazaarResearch](tag, "bazaar_research")
  {
    def updatableFields = (title, creatorId, valueDetails, motivation, organizationName,
      requiredResources, deadline, duration, updatedAt)
    override def * =
      (id, title, creatorId, valueDetails, motivation, organizationName,
        requiredResources, deadline, duration, disabled, createdAt, updatedAt
      ).mapTo[DBBazaarResearch]
  }

  protected class BazaarResearchTableS(tag: Tag)
    extends BazaarResearchTableA[DBBazaarResearchS](tag, "bazaar_research_s")
  {
    def score: Rep[Double] = column("score")
    def views: Rep[Int] = column("views")
    def agrees: Rep[Int] = column("agrees")
    def wishes: Rep[Int] = column("wishes")
    def favorites: Rep[Int] = column("favorites")
    def comments: Rep[Int] = column("comments")
    override def * =
      (id, title, creatorId, valueDetails, motivation, organizationName,
        requiredResources, deadline, duration, disabled, createdAt, updatedAt,
        score.asColumnOf[Option[Double]], views.asColumnOf[Option[Int]],
        agrees.asColumnOf[Option[Int]], wishes.asColumnOf[Option[Int]],
        favorites.asColumnOf[Option[Int]], comments.asColumnOf[Option[Int]]
      ).mapTo[DBBazaarResearchS]
  }

  protected val BazaarResearchQuery: TableQuery[BazaarResearchTable] = TableQuery[BazaarResearchTable]
  protected val BazaarResearchQueryS: TableQuery[BazaarResearchTableS] = TableQuery[BazaarResearchTableS]

  protected implicit def researchToDB(idea: BazaarResearch): DBBazaarResearch =
    DBBazaarResearch(
      id = idea.id,
      title = idea.title,
      creatorId = idea.creator.id,
      organizationName = idea.organizationName,
      valueDetails = idea.valueDetails,
      motivation = idea.motivation,
      requiredResources = idea.requiredResources,
      deadline = idea.deadline,
      duration = idea.duration,
      disabled = false,
      createdAt = idea.createdAt,
      updatedAt = idea.updatedAt
    )


  protected class BazaarResearchRoleTable(tag: Tag) extends Table[DBBazaarResearchRole](tag, "bazaar_research_role") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def bazaarResearchId: Rep[Long] = column("bazaar_research_id")
    def people: Rep[Int] = column("people")
    override def * = (id, bazaarResearchId, people) <> (DBBazaarResearchRole.tupled, DBBazaarResearchRole.unapply)
  }

  protected val BazaarResearchRoleQuery: TableQuery[BazaarResearchRoleTable] = TableQuery[BazaarResearchRoleTable]


  protected class BazaarResearchSkillTable(tag: Tag) extends Table[DBBazaarResearchSkill](tag, "bazaar_research_skill") {
    def bazaarResearchRoleId: Rep[Long] = column("bazaar_research_role_id")
    def skillId: Rep[Long] = column("skill_id")
    def pk = primaryKey("pk", (bazaarResearchRoleId, skillId))
    override def * = (bazaarResearchRoleId, skillId) <> (DBBazaarResearchSkill.tupled, DBBazaarResearchSkill.unapply)
  }

  protected val BazaarResearchSkillQuery: TableQuery[BazaarResearchSkillTable] = TableQuery[BazaarResearchSkillTable]

}
