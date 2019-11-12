package models.daos.slick

import java.sql.Timestamp

import models._
import models.daos.slick.tables.SlickBazaarResearchTable._
import models.daos.slick.tables.SlickSkillTable._
import models.daos.slick.tables.SlickTopicTable.dbTopicToTopic
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}


private[slick] object SlickBazaarResearchDAO {
  def apply(dbConfigProvider: DatabaseConfigProvider)
           (implicit ec: ExecutionContext): SlickBazaarIdeaSpecificDAO[BazaarResearch, DBBazaarResearch, DBBazaarResearchS] =
    new SlickBazaarResearchDAO(dbConfigProvider)
}

private[slick] class SlickBazaarResearchDAO(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit val ec: ExecutionContext)
  extends SlickBazaarIdeaSpecificDAO[BazaarResearch, DBBazaarResearch, DBBazaarResearchS]
    with SlickBazaarIdeaHelpers {

  import Implicits.DefaultWithId._
  import profile.api._


  override protected type TableTypeS = BazaarResearchTableS

  override protected def table2Disabled(t: TableTypeS) = t.disabled
  override protected def table2PastDeadline(t: TableTypeS, now: Timestamp) =
    timestampPlus(t.createdAt, t.deadline) < now

  override def allSlim(userId: Long, disabled: Boolean): Future[Seq[BazaarIdeaSlim]] = db run (for {
    ideaUserPrefAct <- allQuery.includeDisabled(disabled).includePastDeadline(disabled)
      .join(UsersQuery).on(_.creatorId === _.id)
      .joinLeft(BazaarPreferenceQuery.filter(_.userId === userId)).on(_._1.id === _.bazaarResearchId)
      .joinLeft(ActivityResearchQuery).on(_._1._1.id === _.bazaarResearchId)
      .result
    topics <- BazaarIdeaTopicQuery.filter(_.bazaarResearchId inSet ideaUserPrefAct.map(_._1._1._1.id))
      .join(TopicQuery).on(_.topicId === _.id)
      .map { case (link, topic) => (link.bazaarResearchId.get, topic.id, topic.topic) }
      .result
    skills <- allQuery.includeDisabled(disabled).includePastDeadline(disabled)
      .join(BazaarResearchRoleQuery).on(_.id === _.bazaarResearchId)
      .join(BazaarResearchSkillQuery).on(_._2.id === _.bazaarResearchRoleId)
      .join(SkillQuery).on(_._2.skillId === _.id)
      .map { case (((idea, _), _), skill) => (idea.id, skill.id, skill.name) }
      .result
  } yield {
    val topicsMap = topics.groupBy(_._1).mapValues(_.map { case (_, id, name) => Topic(id, name) })
    val skillsMap = skills.groupBy(_._1).mapValues(_.distinct.map { case (_, id, name) => SlimSkill(id, name) })
    ideaUserPrefAct.map {
      case (((idea, user), pref), act) =>
        BazaarIdeaSlim(
          id = idea.id,
          title = idea.title,
          creator = user.toShort,
          topics = topicsMap.getOrElse(idea.id, Seq()),
          createdAt = idea.createdAt,
          updatedAt = idea.updatedAt,
          ideaType = BazaarResearchType,
          preference = pref.map(_.toSlim),
          deadline = Some(idea.deadline),
          skills = skillsMap.getOrElse(idea.id, Seq()),
          score = idea.score,
          counts = getPreferenceCounts(idea.views, idea.agrees, idea.wishes, idea.comments, idea.favorites),
          activityId = act.map(_.id))
    }
  })

  override def enhance(idea: DBBazaarResearchS) = {
    val queries = for {
      dbUser <- UsersQuery.filter(_.id === idea.creatorId).result.head
      topics <- topicsQuery(idea.id, BazaarResearchType).result
      roles <- BazaarResearchRoleQuery.filter(_.bazaarResearchId === idea.id).result
      roles <- DBIO.sequence {
        roles.map { role =>
          BazaarResearchSkillQuery.filter(_.bazaarResearchRoleId === role.id)
            .join(SkillQuery).on(_.skillId === _.id).result
            .map(skills => (role, skills.map(_._2)))
        }
      }
      activityId <- ActivityResearchQuery.filter(_.bazaarResearchId === idea.id).map(_.id).result.headOption
    } yield (dbUser, topics, roles, activityId)

    db run queries map {
      case (dbUser, topics, roles, activityId) => BazaarResearch(
        id = idea.id,
        title = idea.title,
        creator = dbUser,
        topics = topics.map(dbTopicToTopic),
        organizationName = idea.organizationName,
        valueDetails = idea.valueDetails,
        motivation = idea.motivation,
        requiredResources = idea.requiredResources,
        positions = roles.map {
          case (role, dbSkills) =>
            val skills = dbSkills.map(dbSkillToSkill)
            BazaarResearchRole(role.id, role.people, skills)
        },
        deadline = idea.deadline,
        duration = idea.duration,
        activityId = activityId,
        createdAt = idea.createdAt,
        updatedAt = idea.updatedAt,
        score = idea.score,
        counts = getPreferenceCounts(idea.views, idea.agrees, idea.wishes, idea.comments, idea.favorites)
      )
    }
  }

  override protected def allQuery = BazaarResearchQueryS

  override protected def findQuery(id: Long) = BazaarResearchQueryS.filter(_.id === id)

  protected def linkSkill(roleId: Long, skillId: Long) = BazaarResearchSkillQuery += DBBazaarResearchSkill(roleId, skillId)

  protected def createSkills(skills: Seq[Skill], roleId: Long) = DBIO.sequence {
    skills.map { skill =>
      for {
        skillOpt <- SkillQuery.filter(_.name.toLowerCase.like(s"%${skill.name.toLowerCase}%")).result.headOption
        skillId <- skillOpt match {
          case Some(dbSkill) =>
            for (_ <- linkSkill(roleId, dbSkill.id)) yield dbSkill.id
          case None =>
            for {
              skillId <- (SkillQuery returning SkillQuery.map(_.id)) += DBSkill(0, skill.name, None, request = true)
              _ <- linkSkill(roleId, skillId)
            } yield skillId
        }
      } yield skillId
    }
  }

  protected def createRoles(roles: Seq[BazaarResearchRole], ideaId: Long) = DBIO.sequence {
    roles.map { role =>
      for {
        roleId <- (BazaarResearchRoleQuery returning BazaarResearchRoleQuery.map(_.id)) += DBBazaarResearchRole(0, ideaId, role.people)
        skillIds <- createSkills(role.skills, roleId)
      } yield role.copy(id = roleId, skills = zipWithIds(role.skills, skillIds))
    }
  }

  override def create(idea: BazaarResearch) = {
    val now = currentTimestamp()
    val dbIdea: DBBazaarResearch = idea.copy(id = 0, createdAt = now, updatedAt = now)

    val actions = for {
      ideaId <- (BazaarResearchQuery returning BazaarResearchQuery.map(_.id)) += dbIdea
      newTopics <- insertTopics(idea.topics, ideaId, BazaarResearchType)
      roles <- createRoles(idea.positions, ideaId)
    } yield (ideaId, newTopics, roles)

    db run actions.transactionally map {
      case (ideaId, topics, roles) => idea.copy(
        id = ideaId,
        topics = topics,
        positions = roles,
        createdAt = now,
        updatedAt = now
      )
    }
  }

  protected def updateRole(ideaId: Long, role: BazaarResearchRole) = {
    for {
      _ <- BazaarResearchRoleQuery.filter(r => r.id === role.id && r.bazaarResearchId === ideaId)
        .update(DBBazaarResearchRole(role.id, ideaId, role.people))

      deletableSkillIds = getDeletableIds(role.skills)
      _ <- BazaarResearchSkillQuery.filter(link => link.bazaarResearchRoleId === role.id && link.skillId.inSet(deletableSkillIds)).delete

      creatableSkills = getCreatable(role.skills)
      newSkillIds <- createSkills(creatableSkills, role.id)

      existingSkills = getExisting(role.skills)
      _ <- DBIO.sequence {
        existingSkills.map { skill =>
          for {
            optSkill <- BazaarResearchSkillQuery.filter(s => s.skillId === skill.id && s.bazaarResearchRoleId === role.id).result.headOption
            _ <- optSkill match {
              case Some(_) =>
                DBIO.successful(())
              case None =>
                BazaarResearchSkillQuery += DBBazaarResearchSkill(role.id, skill.id)
            }
          } yield ()
        }
      }
    } yield role.copy(skills = existingSkills ++ zipWithIds(creatableSkills, newSkillIds))
  }

  protected def updateRoles(roles: Seq[BazaarResearchRole], ideaId: Long) = DBIO.sequence {
    roles.map(updateRole(ideaId, _))
  }

  override def update(idea: BazaarResearch) = {
    val updatedIdea = idea.copy(updatedAt = currentTimestamp())
    val ideaId = idea.id

    val actions = for {
      _ <- BazaarResearchQuery.filter(_.id === ideaId).map(_.updatableFields).update(updatedIdea.updatableFields)
      updatedTopics <- updateTopics(idea.topics, ideaId, BazaarResearchType)

      validRoleIds <- BazaarResearchRoleQuery.filter(_.bazaarResearchId === ideaId).map(_.id).result
      isValidRoleId = validRoleIds.contains[Long] _

      creatableRoles = getCreatable(idea.positions)
      newRoles <- createRoles(creatableRoles, ideaId)

      deletableRoleIds = getDeletableIds(idea.positions).filter(isValidRoleId)
      _ <- BazaarResearchSkillQuery.filter(_.bazaarResearchRoleId.inSet(deletableRoleIds)).delete
      _ <- BazaarResearchRoleQuery.filter(r => r.bazaarResearchId === ideaId && r.id.inSet(deletableRoleIds)).delete

      existingRoles = getExisting(idea.positions).filter(r => isValidRoleId(r.id))
      roles <- updateRoles(existingRoles, ideaId)
    } yield (updatedTopics, roles ++ newRoles)

    db run actions.transactionally map {
      case (topics, roles) =>
        updatedIdea.copy(topics = topics, positions = roles)
    }
  }

  override def delete(id: Long) = db run deleteResearchAction(id).transactionally

}
