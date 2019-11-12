package services

import java.sql.Timestamp

import javax.inject.Inject
import models.daos.SkillDAO
import models._

import scala.concurrent.{ExecutionContext, Future}


trait SkillService extends StatService {

  protected implicit val ec: ExecutionContext

  def all: Future[Seq[Skill]]
  def all(user: User): Future[Seq[UserSkill]]
  def search(name: String, all: Boolean): Future[Seq[Skill]]
  def searchAdmin(name: Option[String], user: Option[String]): Future[Seq[SkillAdmin]]
  def save(userSkill: UserSkill): Future[UserSkill]
  def delete(userSkillId: Long): Future[Int]
  def enrichWithSkills(user: User): Future[User]
  def enrichWithSkills(optUser: Option[User]): Future[Option[User]] =
    optionToFutureOption[User](optUser, enrichWithSkills)

  def requests: Future[Map[Skill, Seq[UserShort]]]
  def confirmRequest(skillId: Long, confirm: Boolean): Future[ServiceReply[Unit, NotFound]]

  def find(skillId: Long): Future[Option[(Skill, Seq[UserShort])]]
  def create(skill: Skill): Future[Skill]
  def update(skill: Skill): Future[Skill]
  def moveSkills(fromSkillId: Long, toSkillId: Long): Future[ServiceReply[Unit, NotFound]]
  def deleteSkill(skillId: Long): Future[ServiceReply[Unit, NotFound]]
  def deleteForUser(skillId: Long, userId: Long): Future[ServiceReply[Unit, NotFound]]

  def latest(limit: Int = defaultLimitLarge): Future[Seq[SlimSkill]]
  def byUserCount(from: Timestamp, to: Timestamp, limit: Int = defaultLimit): Future[Seq[SkillTopStat]]

}


class SkillServiceImpl @Inject() (skillDAO: SkillDAO)(implicit val ec: ExecutionContext) extends SkillService {

  import Service.Ops._

  override def all: Future[Seq[Skill]] = skillDAO.all

  override def all(user: User): Future[Seq[UserSkill]] = skillDAO.all(user)

  override def search(name: String, all: Boolean): Future[Seq[Skill]] =
    skillDAO.search(name, all).flatMap(seq => Future.sequence(seq.map(enrichWithPath)))

  override def searchAdmin(name: Option[String], user: Option[String]): Future[Seq[SkillAdmin]] =
    skillDAO.searchAdmin(name, user)

  override def save(userSkill: UserSkill): Future[UserSkill] = skillDAO.create(userSkill)

  override def delete(userSkillId: Long): Future[Int] = skillDAO.delete(userSkillId)

  override def enrichWithSkills(user: User): Future[User] = all(user).map(skills => user.copy(skills = skills))


  override def requests = skillDAO.requests.map { plainSkills =>
    plainSkills.groupBy(_._1).mapValues(_.flatMap(_._2))
  }

  override def confirmRequest(skillId: Long, confirm: Boolean) =
    skillDAO.confirmRequest(skillId, confirm).map(toNotFound)

  private def enrichWithPath(skill: Skill): Future[Skill] = skillDAO.getPath(skill.id).map(optPath => skill.copy(path = optPath))

  override def find(skillId: Long) = skillDAO.find(skillId).flatMap(optionToFutureOption2 {
    case (skill, users) =>
      enrichWithPath(skill).map((_, users))
  })

  override def create(skill: Skill) = skillDAO.create(skill).flatMap(enrichWithPath)

  override def update(skill: Skill) = skillDAO.update(skill).flatMap(enrichWithPath)

  override def moveSkills(fromSkillId: Long, toSkillId: Long) =
    skillDAO.moveSkills(fromSkillId, toSkillId).map(_.toNotFound)

  override def deleteSkill(skillId: Long) =  skillDAO.deleteSkill(skillId).map(_.toNotFound)

  override def deleteForUser(skillId: Long, userId: Long) =
    skillDAO.deleteForUser(skillId, userId).map(toNotFound)

  override def latest(limit: Int) = skillDAO.latest(limit)

  override def byUserCount(from: Timestamp, to: Timestamp, limit: Int) =
    skillDAO.byUserCount(from, to, limit)

}
