package models.daos

import java.sql.Timestamp

import models._

import scala.concurrent.Future


trait SkillDAO {

  def all: Future[Seq[Skill]]
  def all(user: User): Future[Seq[UserSkill]]
  def search(name: String, all: Boolean): Future[Seq[Skill]]
  def searchAdmin(name: Option[String], user: Option[String]): Future[Seq[SkillAdmin]]
  def create(userSkill: UserSkill): Future[UserSkill]
  def delete(userSkillId: Long): Future[Int]
  def deleteForUser(skillId: Long, userId: Long): Future[Boolean]

  def getPath(skillId: Long): Future[Option[Seq[Skill]]]

  def requests: Future[Seq[(Skill, Option[UserShort])]]
  def confirmRequest(skillId: Long, confirm: Boolean): Future[Boolean]

  def find(skillId: Long): Future[Option[(Skill, Seq[UserShort])]]
  def create(skill: Skill): Future[Skill]
  def update(skill: Skill): Future[Skill]
  def moveSkills(fromSkillId: Long, toSkillId: Long): Future[Option[Unit]]
  def deleteSkill(skillId: Long): Future[Boolean]

  def latest(limit: Int): Future[Seq[SlimSkill]]
  def byUserCount(from: Timestamp, to: Timestamp, limit: Int): Future[Seq[SkillTopStat]]

}
