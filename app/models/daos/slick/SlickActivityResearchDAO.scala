package models.daos.slick

import java.sql.{Date, Timestamp}

import javax.inject.Inject
import models._
import models.daos.ActivityResearchDAO
import models.daos.ActivityResearchDAO.{ApplicationReply, DeadlinePassed, RoleNotFound}
import models.daos.slick.tables.SlickActivityResearchTables._
import models.daos.slick.tables.SlickImageGalleryTables._
import models.daos.slick.tables.SlickSkillTable._
import models.daos.slick.tables.SlickTopicTable._
import models.daos.slick.tables.SlickUserTable.DBUser
import play.api.db.slick.DatabaseConfigProvider

import scala.Function.const
import scala.concurrent.ExecutionContext


private[slick] object SlickActivityResearchDAO {

  private def getAppFromDB(dbApp: DBActivityResearchApp) = ActivityResearchApp(
    userId = dbApp.userId,
    motivation = dbApp.motivation,
    createdAt = dbApp.createdAt)

  private def getDBActivity(activity: ActivityResearch, galleryId: Option[Long] = None) = DBActivityResearch(
    id = activity.id,
    coverExt = activity.coverPic.extension,
    galleryId = galleryId.getOrElse(activity.gallery.id),
    organizationName = activity.organizationName,
    deadline = activity.deadline,
    startDate = activity.startDate,
    duration = activity.duration,
    projectLink = activity.projectLink,
    bazaarResearchId = activity.bazaarIdeaId,
    createdAt = activity.createdAt,
    updatedAt = activity.updatedAt)

  private def getDBActivityT(activity: ActivityResearch, activityId: Option[Long] = None) = DBActivityResearchT(
    activityId = activityId.getOrElse(activity.id),
    language = activity.language.language,
    title = activity.title,
    valueDetails = activity.valueDetails,
    motivation = activity.motivation)

  private def teamToDB(activityId: Long, team: ActivityResearchTeam) = DBActivityResearchTeam(
    id = team.id,
    activityResearchId = activityId,
    userId = team.userId,
    firstName = team.firstName,
    lastName = team.lastName,
    title = team.title)

  private def composeAll(language: Language)(
    tpl: (Seq[(DBActivityResearch, DBActivityResearchT)], Map[Long, DBUser],
      Seq[((Long, Long), DBTopic)], Map[Long, Int], Option[Map[Long, Long]], Option[Map[Long, Int]],
      Option[Map[Long, Set[DBSkill]]])
  ) = tpl match {
    case (activitiesT, users, topics, rolesCounts, favorites, applications, skills) =>
      val now = System.currentTimeMillis()

      val topicsMap = topics.groupBy(_._1._1).mapValues(_.map(_._2: Topic))

      activitiesT map {
        case (activity, translation) =>
          val topics_ = topicsMap.getOrElse(activity.id, Seq())
          val roles = rolesCounts.getOrElse(activity.id, 0)
          val favorite = favorites.map(_.contains(activity.id))
          val applicationsCount = applications.map(_.getOrElse(activity.id, 0))
          val skills_ = skills.map(_.getOrElse(activity.id, Set()).map(s => SlimSkill(s.id, s.name)))

          ActivityResearchSlim(
            id = activity.id,
            title = translation.title,
            topics = topics_,
            owner = activity.bazaarResearchId.map(users(_).toShort),
            deadline = ActivityDeadline(
              date = activity.deadline,
              closed = SlickActivityHelpers.deadlineClosed(activity.deadline, now)),
            startDate = activity.startDate,
            rolesCount = roles,
            bazaarIdeaId = activity.bazaarResearchId,
            participantsCount = applicationsCount,
            skills = skills_,
            favorite = favorite,
            createdAt = activity.createdAt,
            updatedAt = activity.updatedAt)
      }
  }

}


class SlickActivityResearchDAO @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends ActivityResearchDAO with SlickActivityHelpers {

  import SlickActivityHelpers._
  import SlickActivityResearchDAO._
  import models.Implicits.DefaultWithId._
  import profile.api._

  override protected val ActivityTopicQuery = ActivityResearchTopicQuery
  override protected val ActivityFavoriteQuery = ActivityResearchFavoriteQuery

  private def queryT(language: Language, future: Boolean) = {
    val q = ActivityResearchQuery.join(ActivityResearchTQuery).on(_.id === _.activityId)
      .filter(_._2.language === language.language)
    (if (future) q.filter(_._1.startDate > today())
     else q).sortBy(_._1.startDate.desc.nullsLast)
  }

  private val queryJoinedSkills = ActivityResearchRoleSkillQuery.join(SkillQuery).on(_.skillId === _.id)

  private val queryJoinedIdeaUser = BazaarResearchQuery.join(UsersQuery).on(_.creatorId === _.id)

  private def queryAll(language: Language, userId: Long, fromAdmin: Boolean, queryFilter: ActivitiesQueryFilter) = for {
    activitiesT <- (queryFilter match {
      case QueryBySearch(optS, skillIds, matchAll, future) =>
        val q = queryT(language, future)
        val q1 = optS match {
          case Some(s) =>
            q.joinLeft(queryJoinedIdeaUser).on(_._1.bazaarResearchId === _._1.id)
              .filter {
                case ((_, aT), optIdea) =>
                  val v = s"%${s.toLowerCase}%"
                  aT.title.toLowerCase.like(v) || optIdea.map {
                    case (_, iO) => (iO.firstName ++ " " ++ iO.lastName).toLowerCase.like(v)
                  }
              }.map(_._1)
          case None => q
        }
        if (skillIds.nonEmpty) {
          q1.join(ActivityResearchRoleQuery).on(_._1.id === _.activityResearchId).filter(_._2.id in {
            val subQ = ActivityResearchRoleSkillQuery.filter(_.skillId inSet skillIds)
            if (matchAll)
              subQ.groupBy(_.activityResearchRoleId)
                .map { case (aId, g) => (aId, g.length) }
                .filter(_._2 === skillIds.length).map(_._1)
            else subQ.map(_.activityResearchRoleId)
          }).map(_._1)
        } else q1
      case QueryByIds(ids) => queryT(language, future = false).filter(_._1.id inSet ids)
      case QueryNoFilter => queryT(language, future = false)
    }).result

    activityIds = if (queryFilter.applyFilter) activitiesT.map(_._1.id).toSet else Set()

    users <- {
      val q = queryJoinedIdeaUser
      if (queryFilter.applyFilter)
        q.filter(_._1.id inSet activitiesT.flatMap(_._1.bazaarResearchId))
      else q
    }.map { case (idea, user) => (idea.id, user) }.result

    topics <- {
      val q = ActivityResearchTopicQuery.join(TopicQuery).on(_.topicId === _.id)
      if (queryFilter.applyFilter)
        q.filter(_._1.activityId inSet activityIds)
      else q
    }.result

    rolesCount <- {
      val q = ActivityResearchRoleQuery
      if (queryFilter.applyFilter)
        q.filter(_.activityResearchId inSet activityIds)
      else q
    }.groupBy(_.activityResearchId).map { case (id, g) => (id, g.length) }
      .result.map(_.toMap)

    favorites <- {
      if (fromAdmin)
        DBIO.successful(None)
      else {
        val q = ActivityResearchFavoriteQuery.filter(_.userId === userId)
        (if (queryFilter.applyFilter)
          q.filter(_.activityId inSet activityIds)
        else q).result.map(seq => Some(seq.toMap))
      }
    }

    applications <- {
      if (fromAdmin)
        (if (queryFilter.applyFilter)
          ActivityResearchRoleQuery.filter(_.activityResearchId.inSet(activityIds))
        else ActivityResearchRoleQuery)
          .join(ActivityResearchRoleAppQuery).on(_.id === _.activityResearchRoleId)
          .map(_._1.activityResearchId).groupBy(identity).map { case (id, g) => (id, g.length) }
          .result.map(seq => Some(seq.toMap))
      else
        DBIO.successful(None)
    }

    skills <- {
      if (fromAdmin)
        (if (queryFilter.applyFilter)
          ActivityResearchRoleQuery.filter(_.activityResearchId.inSet(activityIds))
        else ActivityResearchRoleQuery)
          .join(ActivityResearchRoleSkillQuery).on(_.id === _.activityResearchRoleId)
          .join(SkillQuery).on(_._2.skillId === _.id)
          .map { case ((role, _), skill) => (role.activityResearchId, skill) }
          .result.map { seq => Some(seq.groupBy(_._1).mapValues(_.map(_._2).toSet)) }
      else
        DBIO.successful(None)
    }
  } yield (activitiesT, users.toMap, topics, rolesCount, favorites, applications, skills)

  override def all(language: Language, userId: Long, fromAdmin: Boolean, search: Option[String],
                   searchSkillIds: Seq[Long], matchAll: Boolean, future: Boolean) = {
    val q = if (search.isEmpty && searchSkillIds.isEmpty && !future) QueryNoFilter
            else QueryBySearch(search, searchSkillIds, matchAll, future)
    db run queryAll(language, userId, fromAdmin, q) map composeAll(language)
  }

  override def find(id: Long, language: Language, userId: Long, future: Boolean) = {
    val query = queryT(language, future).filter(_._1.id === id).result.headOption flatMap { optActivity =>
      val optAction = optActivity map {
        case (activity, translation) => for {
          gallery <- ImageGalleryQuery.filter(_.id === activity.galleryId).result.head
          galleryImages <- ImageGalleryImageQuery.filter(_.galleryId === gallery.id).result

          topics <- ActivityResearchTopicQuery.join(TopicQuery).on(_.topicId === _.id)
            .filter(_._1.activityId === activity.id).map(_._2).result

          roles <- ActivityResearchRoleQuery.filter(_.activityResearchId === activity.id)
            .joinLeft(ActivityResearchRoleAppQuery.filter(_.userId === userId))
            .on(_.id === _.activityResearchRoleId).result
          skills <- queryJoinedSkills.filter(_._1.activityResearchRoleId inSet roles.map(_._1.id)).result

          favorite <- ActivityResearchFavoriteQuery
            .filter(a => a.activityId === id && a.userId === userId).exists.result

          userHasAccess <- queryUserAccess(id, userId).result

          team <- ActivityResearchTeamQuery.filter(_.activityResearchId === activity.id).result
        } yield (activity, translation, gallery, galleryImages, topics, roles, skills, favorite, userHasAccess, team)
      }

      DBIO.sequenceOption(optAction)
    }

    db run query map (_.map {
      case (activity, translation, gallery, galleryImages, topics, roles, skills, favorite, userHasAccess, team) =>
        ActivityResearch(
          id = activity.id,
          language = translation.language,
          title = translation.title,
          coverPic = DataImage(activity.coverExt),
          gallery = dbToImageGallery(gallery, galleryImages),
          topics = topics.map(t => t: Topic),
          organizationName = activity.organizationName,
          motivation = translation.motivation,
          valueDetails = translation.valueDetails,
          deadline = activity.deadline,
          startDate = activity.startDate,
          duration = activity.duration,
          projectLink = activity.projectLink,
          roles = roles.map {
            case (r, app) =>
              ActivityResearchRole(
                id = r.id,
                people = r.people,
                skills = skills.filter(_._1._1 == r.id).map(_._2: Skill),
                application = app map getAppFromDB)
          },
          team = team.map(_.toModel),
          bazaarIdeaId = activity.bazaarResearchId,
          favorite = Some(favorite),
          userHasAccess = Some(userHasAccess),
          createdAt = activity.createdAt,
          updatedAt = activity.updatedAt)
    })
  }

  private def linkRoleSkill(roleId: Long, skillId: Long) =
    ActivityResearchRoleSkillQuery += (roleId, skillId)

  private def createRoleSkills(roleId: Long, skills: Seq[Skill]) = DBIO.sequence {
    skills map { skill =>
      SkillQuery.filter(_.name.toLowerCase === skill.name.toLowerCase).result.headOption.flatMap {
        case Some(dbSkill) => linkRoleSkill(roleId, dbSkill.id) map (_ => dbSkill: Skill)
        case None => for {
          skillId <- (SkillQuery returning SkillQuery.map(_.id)) +=
            DBSkill(0, skill.name.toLowerCase, None, request = true)
          _ <- linkRoleSkill(roleId, skillId)
        } yield skill.copy(id = skillId)
      }
    }
  }

  private def updateRoles(activityId: Long, roles: Seq[ActivityResearchRole]) = for {
    newRoles <- DBIO.sequence {
      getCreatable(roles) map { role =>
        for {
          roleId <- (ActivityResearchRoleQuery returning ActivityResearchRoleQuery.map(_.id)) +=
            DBActivityResearchRole(0, activityId, role.people)
          newSkills <- createRoleSkills(roleId, getCreatable(role.skills))
          existingSkills = getExisting(role.skills)
          _ <- ActivityResearchRoleSkillQuery ++= existingSkills map (skill => (roleId, skill.id))
        } yield role.copy(id = roleId, skills = newSkills ++ existingSkills)
      }
    }

    updatedRoles <- DBIO.sequence {
      getExisting(roles) map { role =>
        for {
          _ <- ActivityResearchRoleQuery.filter(_.id === role.id)
            .update(DBActivityResearchRole(role.id, activityId, role.people))
          newSkills <- createRoleSkills(role.id, getCreatable(role.skills))
          existingSkills = getExisting(role.skills)
          linkedSkillIds <- ActivityResearchRoleSkillQuery
            .filter(_.activityResearchRoleId === role.id).map(_.skillId).result
          alreadyLinked = (skill: Skill) => linkedSkillIds.contains(skill.id)
          _ <- ActivityResearchRoleSkillQuery ++= existingSkills filterNot alreadyLinked map (skill => (role.id, skill.id))
          _ <- getDeletableIds(role.skills) match {
            case Seq() => DBIO.successful(())
            case deletableSkillIds => ActivityResearchRoleSkillQuery
                .filter(rs => rs.activityResearchRoleId === role.id && rs.skillId.inSet(deletableSkillIds))
                .delete
          }
        } yield role.copy(skills = newSkills ++ existingSkills)
      }
    }

    _ <- getDeletableIds(roles) match {
      case Seq() => DBIO.successful(())
      case deletableRoleIds => for {
        _ <- ActivityResearchRoleAppQuery.filter(_.activityResearchRoleId inSet deletableRoleIds).delete
        _ <- ActivityResearchRoleSkillQuery.filter(_.activityResearchRoleId inSet deletableRoleIds).delete
        _ <- ActivityResearchRoleQuery.filter(_.id inSet deletableRoleIds).delete
      } yield ()
    }
  } yield newRoles ++ updatedRoles

  private def updateTeam(activityId: Long, team: Seq[ActivityResearchTeam]) = for {
    newTeam <- getCreatable(team) match {
      case creatable if creatable.nonEmpty => for {
        ids <- (ActivityResearchTeamQuery returning ActivityResearchTeamQuery.map(_.id)) ++=
          creatable.map(teamToDB(activityId, _))
      } yield zipWithIds(creatable, ids)
      case _ => DBIO.successful(Seq.empty)
    }
    updatedTeam <- getExisting(team) match {
      case existing if existing.nonEmpty =>
        val qs = existing.map { e =>
          ActivityResearchTeamQuery.filter(_.id === e.id)
            .update(teamToDB(activityId, e)).map(const(e))
        }
        DBIO.sequence(qs)
      case _ => DBIO.successful(Seq.empty)
    }
    _ <- getDeletableIds(team) match {
      case ids if ids.nonEmpty => ActivityResearchTeamQuery.filter(_.id inSet ids).delete
      case _ => DBIO.successful(0)
    }
  } yield newTeam ++ updatedTeam

  override def create(activity: models.ActivityResearch) = {
    val now = currentTimestamp()
    val activityWithTime = activity.copy(id = 0, createdAt = now, updatedAt = now)

    val action = for {
      gallery <- {
        if (activity.gallery.id == 0) {
          for {
            gId <- (ImageGalleryQuery returning ImageGalleryQuery.map(_.id)) += DBImageGallery(0, activity.gallery.name)
            images <- updateGalleryImages(gId, activity.gallery.images)
          } yield activity.gallery.copy(id = gId, images = images)
        } else {
          for {
            images <- updateGalleryImages(activity.gallery.id, activity.gallery.images)
          } yield  activity.gallery.copy(images = images)
        }
      }

      activityId <- (ActivityResearchQuery returning ActivityResearchQuery.map(_.id)) +=
        getDBActivity(activityWithTime, Some(gallery.id))
      _ <- ActivityResearchTQuery += getDBActivityT(activity, Some(activityId))

      topics <- updateTopics(activityId, activity.topics)
      roles <- updateRoles(activityId, activity.roles)
      team <- updateTeam(activityId, activity.team)

      _ <- activity.bazaarIdeaId match {
        case Some(ideaId) =>
          BazaarResearchQuery.filter(_.id === ideaId).map(_.disabled).update(true)
        case None =>
          DBIO.successful(0)
      }
    } yield (activityId, gallery, topics, roles, team)

    db run action.transactionally map {
      case (activityId, gallery, topics, roles, team) =>
        activityWithTime.copy(
          id = activityId,
          gallery = gallery,
          topics = topics,
          roles = roles,
          team = team)
    }
  }

  override def update(activity: ActivityResearch) = {
    val queryActivity = ActivityResearchQuery.filter(_.id === activity.id)
    lazy val activityWithTime = activity.copy(updatedAt = currentTimestamp())

    val action = queryActivity.exists.result flatMap { exists =>
      if (exists)
        for {
          _ <- queryActivity.map(_.updatableFields).update(getDBActivity(activityWithTime).updatableFields)
          _ <- ActivityResearchTQuery.insertOrUpdate(getDBActivityT(activity))

          gallery <- for {
            images <- updateGalleryImages(activity.gallery.id, activity.gallery.images)
          } yield activity.gallery.copy(images = images)

          topics <- updateTopics(activity.id, activity.topics)
          roles <- updateRoles(activity.id, activity.roles)
          team <- updateTeam(activity.id, activity.team)
        } yield Some((gallery, topics, roles, team))
      else
        DBIO.successful(None)
    }

    db run action.transactionally map (_.map {
      case (gallery, topics, roles, team) =>
        activityWithTime.copy(
          gallery = gallery,
          topics = topics,
          roles = roles,
          team = team)
    })
  }

  override def delete(id: Long) = {
    val action = for {
      _ <- ActivityFavoriteQuery.filter(_.activityId === id).delete
      _ <- ActivityResearchTopicQuery.filter(_.activityId === id).delete
      _ <- ActivityResearchTeamQuery.filter(_.activityResearchId === id).delete

      rolesQ = ActivityResearchRoleQuery.filter(_.activityResearchId === id)
      _ <- ActivityResearchRoleAppQuery.filter(_.activityResearchRoleId in rolesQ.map(_.id)).delete
      _ <- ActivityResearchRoleSkillQuery.filter(_.activityResearchRoleId in rolesQ.map(_.id)).delete
      _ <- rolesQ.delete

      _ <- ActivityResearchTQuery.filter(_.activityId === id).delete
      r <- ActivityResearchQuery.filter(_.id === id).delete
    } yield r == 1

    db run action.transactionally
  }

  override def getCoverPic(id: Long) = {
    val query = ActivityResearchQuery.filter(_.id === id).map(_.coverExt)
    db run query.result.headOption
  }

  override def favorites(userId: Long, language: Language) = {
    val query = for {
      activityIds <- ActivityResearchFavoriteQuery.filter(_.userId === userId).map(_.activityId).result
      r <- queryAll(language, userId, fromAdmin = false, activityIds)
    } yield r

    db run query map composeAll(language)
  }

  override def changeApplication(roleId: Long, applied: Boolean, app: ActivityResearchApp) = {
    lazy val queryExisting = ActivityResearchRoleAppQuery
      .filter(a => a.userId === app.userId && a.activityResearchRoleId === roleId)

    val q = ActivityResearchQuery.join(ActivityResearchRoleQuery).on(_.id === _.activityResearchId)
      .filter(_._2.id === roleId).map(_._1)

    val action = q.result.headOption.flatMap {
      case Some(activity) if activity.deadline.compareTo(today()) >= 0 =>
        queryExisting.result.headOption.flatMap {
          case Some(dbApp) if applied =>
            DBIO.successful(Some(getAppFromDB(dbApp)))
          case Some(_) =>
            queryExisting.delete map (_ => None)
          case None if applied =>
            val dbApp = DBActivityResearchApp(roleId, app.userId, app.motivation, currentTimestamp())
            (ActivityResearchRoleAppQuery += dbApp) map (_ => Some(app.copy(createdAt = dbApp.createdAt)))
          case None =>
            DBIO.successful(None)
        } map ApplicationReply
      case Some(_) =>
        DBIO.successful(DeadlinePassed)
      case None =>
        DBIO.successful(RoleNotFound)
    }

    db run action.transactionally
  }

  override def applications(activityId: Long) = {
    val query = ActivityResearchQuery.filter(_.id === activityId).exists.result.flatMap { exists =>
      if (exists)
        for {
          roles <- ActivityResearchRoleQuery.filter(_.activityResearchId === activityId).result
          roleIds = roles.map(_.id).toSet
          skills <- queryJoinedSkills.filter(_._1.activityResearchRoleId inSet roleIds).result
          apps <- ActivityResearchRoleAppQuery.join(UsersQuery).on(_.userId === _.id)
            .filter(_._1.activityResearchRoleId inSet roleIds).result
        } yield Some((roles, skills, apps))
      else
        DBIO.successful(None)
    }

    db run query map (_.map {
      case (roles, skills, apps) =>
        roles map { role =>
          ActivityResearchRole(
            id = role.id,
            people = role.people,
            skills = skills.filter(_._1._1 == role.id).map(t => dbSkillToSkill(t._2)),
            applications = apps.filter(_._1.activityResearchRoleId == role.id).map {
              case (app, dbUser) =>
                ActivityResearchAppFull(
                  user = dbUser.toShort,
                  motivation = app.motivation,
                  createdAt = app.createdAt)
            })
        }
    })
  }

  private def queryUserAccess(activityId: Long, userId: Long) =
    ActivityResearchQuery.join(BazaarResearchQuery).on(_.bazaarResearchId === _.id)
      .filter { case (activity, idea) => activity.id === activityId && idea.creatorId === userId }
      .exists

  override def userHasAccess(activityId: Long, userId: Long) =
    db run queryUserAccess(activityId, userId).result

  override def byUser(userId: Long, language: Language) = {
    val q = queryT(language, future = false).filter { case (a, _) =>
        val t = today()
        a.startDate <= t && timestampPlus(a.startDate.asColumnOf[Timestamp], a.duration).asColumnOf[Date] > t
      }
      .join(BazaarResearchQuery.filter(_.creatorId === userId))
      .on(_._1.bazaarResearchId === _.id)
      .map { case ((a, aT), _) => (a.id, aT.title) }

    db run q.result map(_.map { case (id, title) => ActivityMini(id, title) })
  }

}
