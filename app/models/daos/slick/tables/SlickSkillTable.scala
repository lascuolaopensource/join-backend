package models.daos.slick.tables

import java.sql.Timestamp

import models.Skill
import models.daos.slick.tables.SlickSkillTable._
import slick.lifted.ProvenShape

import scala.language.implicitConversions


private[slick] object SlickSkillTable {

  case class DBSkill(
    id: Long,
    name: String,
    parentId: Option[Long],
    request: Boolean)

  case class DBUserSkill(
    id: Long,
    userId: Long,
    skillId: Long,
    createdAt: Timestamp)


  implicit def dbSkillToSkill(dBSkill: DBSkill): Skill = Skill(
    id = dBSkill.id,
    name = dBSkill.name,
    parentId = dBSkill.parentId,
    request = dBSkill.request
  )

}


private[slick] trait SlickSkillTable extends SlickTable {

  import profile.api._

  protected class SkillTable(tag: Tag) extends Table[DBSkill](tag, "skill") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def name: Rep[String] = column("name")
    def parentId: Rep[Option[Long]] = column("parent_id")
    def request: Rep[Boolean] = column("request")
    override def * : ProvenShape[DBSkill] = (id, name, parentId, request) <> (DBSkill.tupled, DBSkill.unapply)
  }

  protected val SkillQuery: TableQuery[SkillTable] = TableQuery[SkillTable]


  protected class UserSkillTable(tag: Tag) extends Table[DBUserSkill](tag, "user_skill") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def userId: Rep[Long] = column("user_id")
    def skillId: Rep[Long] = column("skill_id")
    def createdAt: Rep[Timestamp] = column("created_at")
    override def * : ProvenShape[DBUserSkill] =
      (id, userId, skillId, createdAt) <> (DBUserSkill.tupled, DBUserSkill.unapply)
  }

  protected val UserSkillQuery: TableQuery[UserSkillTable] = TableQuery[UserSkillTable]

}
