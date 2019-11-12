package models.daos.slick

import java.sql.Timestamp

import javax.inject.Inject
import models.daos.SkillDAO
import models.daos.slick.tables.SlickSkillTable._
import models._
import play.api.db.slick.DatabaseConfigProvider
import slick.lifted.CanBeQueryCondition

import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls
import scala.languageFeature.implicitConversions


class SlickSkillDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit val ec: ExecutionContext
) extends SkillDAO with SlickDAO {

  import profile.api._


  override def all: Future[Seq[Skill]] = db run SkillQuery.result map { skills =>
    skills map dbSkillToSkill
  }

  override def all(user: User): Future[Seq[UserSkill]] = {
    val query = for {
      us <- UserSkillQuery if us.userId === user.id
      s <- SkillQuery if us.skillId === s.id
    } yield (us, s)

    db run query.result map { userSkills =>
      userSkills.map { case (us, s) => UserSkill(us.id, s.name, s.id, us.userId, s.request, us.createdAt) }
    }
  }

  private def leafsQuery = SkillQuery joinLeft SkillQuery on (_.id === _.parentId) filter (_._2.isEmpty) map (_._1)

  override def search(name: String, all: Boolean): Future[Seq[Skill]] =
    db run (if (all) SkillQuery else leafsQuery).filter(_.name.toLowerCase like s"%${name.toLowerCase}%").result map (_.map(dbSkillToSkill))

  private def skillIdsByUserName(user: String) = usersJoin
    .filter(_._2.fullName.toLowerCase.like(s"%${user.toLowerCase}%"))
    .map(_._1.skillId)

  private def applySkillFilters[T <: Rep[_] : CanBeQueryCondition](fs: Option[SkillTable => T]*) =
    fs.foldLeft[Query[SkillTable, DBSkill, Seq]](SkillQuery) { (acc, optF) =>
      //noinspection ConvertibleToMethodValue
      optF.map(acc.filter(_)).getOrElse(acc)
    }

  override def searchAdmin(name: Option[String], user: Option[String]) = {
    val q0 = applySkillFilters(
      name.map(n => (table: SkillTable) => table.name.toLowerCase.like(s"%${n.toLowerCase}%")),
      user.map(u => (table: SkillTable) => table.id.in(skillIdsByUserName(u))))

    val q1 = for {
      (s, p) <- q0.joinLeft(SkillQuery).on(_.parentId === _.id)
    } yield (s, p, UserSkillQuery.filter(_.skillId === s.id).length)

    db.run(q1.sortBy(_._1.id.desc).result).map(_.map {
      case (s, p, c) =>
        SkillAdmin(
          id = s.id,
          name = s.name,
          parent = p.map(ps => SlimSkill(ps.id, ps.name)),
          request = s.request,
          usersCount = c)
    })
  }

  override def create(userSkill: UserSkill): Future[UserSkill] = {
    val userSkillL = userSkill.copy(name = userSkill.name.toLowerCase, createdAt = System.currentTimeMillis())

    val lookupSkill = leafsQuery.filter(_.name.toLowerCase === userSkillL.name)
    val lookupUserSkill = (skillId: Long) => UserSkillQuery.filter(ex => ex.userId === userSkillL.userId && ex.skillId === skillId).map(_.id)
    val linkSkillAction = (skillId: Long) => {
      (UserSkillQuery returning UserSkillQuery.map(_.id)) += DBUserSkill(0, userSkillL.userId, skillId, userSkillL.createdAt)
    }
    val createSkillAction = (SkillQuery returning SkillQuery.map(_.id)) += DBSkill(0, userSkillL.name, None, userSkillL.request)

    db run lookupSkill.result.headOption flatMap {
      case Some(DBSkill(skillId, _, _, _)) =>
        db run lookupUserSkill(skillId).result.headOption flatMap {
          case Some(usId) =>
            Future.successful(userSkillL.copy(id = usId, skillId = skillId))
          case None =>
            db run linkSkillAction(skillId) map { usId =>
              userSkillL.copy(id = usId, skillId = skillId)
            }
        }
      case None =>
        db run (for {
          skId <- createSkillAction
          usId <- linkSkillAction(skId)
        } yield (usId, skId)) map {
          case (usId, skId) => userSkillL.copy(id = usId, skillId = skId)
        }
    }
  }

  override def delete(userSkillId: Long): Future[Int] = {
    db run UserSkillQuery.filter(_.id === userSkillId).delete
  }

  override def deleteForUser(skillId: Long, userId: Long) =
    db.run(UserSkillQuery.filter(us => us.skillId === skillId && us.userId === userId).delete.map(_ == 1))


  private def queryParent(skillId: Rep[Long]) = SkillQuery filter (_.id === skillId) joinLeft SkillQuery on (_.parentId === _.id)

  override def getPath(skillId: Long) = {
    def recursiveQueries(skill: DBSkill, optParent: Option[DBSkill], acc: Seq[Skill] = Seq()): DBIO[Seq[Skill]] =
      optParent match {
        case Some(parent) if parent.parentId.isDefined =>
          for {
            (nextSkill, nextOptParent) <- queryParent(parent.parentId.get).result.head
            updatedSkills <- recursiveQueries(nextSkill, nextOptParent, acc ++ Seq(skill: Skill, parent: Skill))
          } yield updatedSkills
        case _ =>
          DBIO.successful(acc ++ Seq(skill: Skill) ++ optParent.map(dbSkillToSkill))
      }

    val action = for {
      optSkill <- queryParent(skillId).result.headOption
      optPath <- DBIO.sequenceOption(optSkill.map {
        case (skill, optParent) => recursiveQueries(skill, optParent)
      })
    } yield optPath.map(_.reverse)

    db run action
  }


  private val usersJoin = UserSkillQuery join UsersQuery on (_.userId === _.id)

  private def skillsUsersQuery[T <: Rep[Boolean]](skillsFilter: (SkillTable) => T) =
    SkillQuery.filter(skillsFilter) joinLeft usersJoin on (_.id === _._1.skillId) map {
      case (skill, u) => (skill, u.map(_._2))
    }

  override def requests = db run skillsUsersQuery(_.request).result map (_.map {
    case (skill, user) => (skill: Skill, user.map(_.toShort))
  })

  override def confirmRequest(skillId: Long, confirm: Boolean) =
    db.run(SkillQuery.filter(_.id === skillId).map(_.request).update(!confirm).map(_ == 1))

  override def find(skillId: Long) = db run skillsUsersQuery(_.id === skillId).result map {
    case Seq((skill, Some(user)), xs@_*) =>
      Some((skill, Seq(user.toShort) ++ xs.flatMap(_._2.map(_.toShort))))
    case Seq((skill, None), xs@_*) =>
      Some((skill, xs.flatMap(_._2.map(_.toShort))))

    case Seq((skill, Some(user))) => Some((skill, Seq(user.toShort)))
    case Seq((skill, None)) =>       Some((skill, Seq()))

    case Seq() => None
  }

  override def create(skill: Skill) = {
    val Skill(_, name, parentId, request, _, _) = skill

    val action = SkillQuery.filter(_.name.toLowerCase === name.toLowerCase).exists.result.flatMap { exists =>
      if (exists)
        DBIO.failed(new IllegalArgumentException(s"a skill with name $name already exists"))
      else for {
        ok <- parentId match {
          case Some(id) => SkillQuery.filter(_.id === id).exists.result
          case None => DBIO.successful(true)
        }
        newId <- {
          if (ok) (SkillQuery returning SkillQuery.map(_.id)) += DBSkill(0, name.toLowerCase, parentId, request)
          else DBIO.failed(new IllegalArgumentException(s"invalid parent id ${parentId.get}"))
        }
      } yield newId
    }

    db run action map (id => skill.copy(id = id))
  }

  override def update(skill: Skill) = {
    val Skill(skillId, name, parentId, request, _, _) = skill
    val nameLower = name.toLowerCase

    val action = SkillQuery.filter(s => s.name.toLowerCase === nameLower).result.headOption.flatMap {
      case Some(s) if s.id != skillId =>
        DBIO.failed(new IllegalArgumentException(s"a skill with name $nameLower already exists"))
      case _ =>
        for {
          ok <- parentId match {
            case Some(id) => SkillQuery.filter(_.id === id).exists.result
            case None => DBIO.successful(true)
          }
          _ <- {
            if (ok) SkillQuery.filter(_.id === skillId).update(DBSkill(skillId, nameLower, parentId, request))
            else DBIO.failed(new IllegalArgumentException(s"invalid parent id ${parentId.get}"))
          }
        } yield skill.copy(name = nameLower)
    }

    db run action
  }

  private def userSkillQuery(skillId: Long) = UserSkillQuery.filter(_.skillId === skillId)
  private def activityTESkillQuery(skillId: Long) = ActivityTeachEventSkillQuery.filter(_.skillId === skillId)
  private def activityRSkillQuery(skillId: Long) = ActivityResearchRoleSkillQuery.filter(_.skillId === skillId)
  private def bazaarRSkillQuery(skillId: Long) = BazaarResearchSkillQuery.filter(_.skillId === skillId)

  private def moveLinkedSkills[T1, T <: Table[T1] { def skillId: Rep[Long] }](
    query: Long => Query[T, T1, Seq]
  )(
    getLinkedId: T => Rep[Long],
    fromSkillId: Long, toSkillId: Long
  ) = {
    val subQ = query(toSkillId).map(getLinkedId)
    query(fromSkillId).filterNot(t => getLinkedId(t) in subQ).map(_.skillId).update(toSkillId)
  }

  override def moveSkills(fromSkillId: Long, toSkillId: Long) = {
    val action = for {
      optSkills <- SkillQuery.filter(_.id === fromSkillId).zip(SkillQuery.filter(_.id === toSkillId)).result.headOption
      opt <- optSkills match {
        case Some(_) =>
          for {
            _ <- moveLinkedSkills(userSkillQuery)(_.userId, fromSkillId, toSkillId)
            _ <- userSkillQuery(fromSkillId).delete

            _ <- moveLinkedSkills(activityTESkillQuery)(_.activityId, fromSkillId, toSkillId)
            _ <- activityTESkillQuery(fromSkillId).delete

            _ <- moveLinkedSkills(activityRSkillQuery)(_.activityResearchRoleId, fromSkillId, toSkillId)
            _ <- activityRSkillQuery(fromSkillId).delete

            _ <- moveLinkedSkills(bazaarRSkillQuery)(_.bazaarResearchRoleId, fromSkillId, toSkillId)
            _ <- bazaarRSkillQuery(fromSkillId).delete
          } yield Some(())
        case None =>
          DBIO.successful(None)
      }
    } yield opt

    db run action.transactionally
  }

  override def deleteSkill(skillId: Long) = {
    val action = for {
      optSkills <- queryParent(skillId).result.headOption
      result <- optSkills match {
        case Some((_, optParent)) =>
          for {
            _ <- optParent match {
              case Some(parent) =>
                for {
                  _ <- moveLinkedSkills(userSkillQuery)(_.userId, skillId, parent.id)
                  _ <- moveLinkedSkills(activityTESkillQuery)(_.activityId, skillId, parent.id)
                  _ <- moveLinkedSkills(activityRSkillQuery)(_.activityResearchRoleId, skillId, parent.id)
                  _ <- moveLinkedSkills(bazaarRSkillQuery)(_.bazaarResearchRoleId, skillId, parent.id)
                } yield ()
              case None =>
                for {
                  _ <- userSkillQuery(skillId).delete
                  _ <- activityTESkillQuery(skillId).delete
                  _ <- activityRSkillQuery(skillId).delete
                  _ <- bazaarRSkillQuery(skillId).delete
                } yield ()
            }
            _ <- SkillQuery.filter(_.parentId === skillId).map(_.parentId).update(optParent.map(_.id))
            n <- SkillQuery.filter(_.id === skillId).delete
          } yield n == 1
        case None =>
          DBIO.successful(false)
      }
    } yield result

    db run action.transactionally
  }

  override def latest(limit: Int) = {
    val q = SkillQuery.sortBy(_.id.desc).take(limit).map(s => (s.id, s.name))
    db.run(q.result).map(_.map(SlimSkill.tupled))
  }

  private lazy val byUserCountQ = Compiled {
    (to: Rep[Timestamp], limit: ConstColumn[Long]) =>
      for {
        (sId, count) <- UserSkillQuery.filter(_.createdAt < to)
          .groupBy(_.skillId)
          .map { case (sId, g) => (sId, g.length) }
          .sortBy(_._2.desc)
          .take(limit)
        skill <- SkillQuery if skill.id === sId
      } yield (skill, count)
  }

  override def byUserCount(from: Timestamp, to: Timestamp, limit: Int) = {
    val action = for {
      p1 <- byUserCountQ(to, limit).result
      p0 <- byUserCountQ(from, limit).result
    } yield p1.zip(computeTrends(p0.map(_._1.id), p1.map(_._1.id))).map {
      case ((skill, count), trend) =>
        SkillTopStat(
          id = skill.id,
          name = skill.name,
          count = count,
          trend = trend)
    }

    db.run(action)
  }

}
