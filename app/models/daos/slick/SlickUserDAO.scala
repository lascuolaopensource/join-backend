package models.daos.slick

import javax.inject.Inject
import com.mohiva.play.silhouette.api.LoginInfo
import models.{SlimSkill, User, UserRole}
import models.daos.UserDAO
import models.daos.slick.tables.SlickSkillTable.{DBSkill, DBUserSkill}
import models.daos.slick.tables.SlickUserFavoriteTable.DBUserFavorite
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}


class SlickUserDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit val ec: ExecutionContext
) extends UserDAO with SlickDAO {

  import profile.api._


  private def countIdeasQ(id: Rep[Long]) =
      BazaarEventQuery.filter(_.creatorId === id).length +
        BazaarTeachLearnQuery.filter(i => i.deletedAt.isEmpty && i.creatorId === id).length +
        BazaarResearchQuery.filter(_.creatorId === id).length


  override def all: Future[Seq[User]] = {
    val q = for {
      user <- UsersQuery.sortBy(_.createdAt.desc)
    } yield (user, countIdeasQ(user.id))

    db.run(q.result).map (_.map {
      case (user, count) => dbUserToUser(user).copy(ideasCount = Some(count))
    })
  }


  override def find(id: Long, userId: Option[Long] = None): Future[Option[User]] = {
    val userQuery = UsersQuery.filter(_.id === id)

    userId match {
      case Some(uId) =>
        val q = userQuery.joinLeft(UserFavoriteQuery.filter(_.userId === uId)).on(_.id === _.otherId)
        db run q.result.headOption map (_.map {
          case (user, f) => dbUserToUser(user).copy(favorite = Some(f.isDefined))
        })
      case None =>
        db run userQuery.result.headOption map(_.map(u => u))
    }
  }


  override def find(loginInfo: LoginInfo): Future[Option[User]] =
    headOptionUser(UsersQuery.filter(_.email === loginInfo.providerKey))


  private def headOptionUser(query: Query[UsersTable, UsersTable#TableElementType, Seq]): Future[Option[User]] =
    db.run(query.result.headOption).map {
      case Some(user) => Some(user)
      case _ => None
    }


  protected def linkSkill(userId: Long, skillId: Long) =
    UserSkillQuery += DBUserSkill(0, userId, skillId, currentTimestamp())

  override def save(user: User, optSkills: Option[Seq[SlimSkill]]): Future[User] = {
    val query = UsersQuery.filter(_.id === user.id)

    def updateSkills(userId: Long) = optSkills match {
      case Some(skills) if skills.isEmpty =>
        UserSkillQuery.filter(_.userId === userId).delete.map(_ => ())
      case Some(skills) =>
        val keepIds = skills.map(_.id).filter(_ != 0)
        for {
          _ <- UserSkillQuery.filter(us => us.userId === userId && !us.skillId.inSet(keepIds)).delete
          _ <- DBIO.sequence {
            skills.map { skill =>
              for {
                foundSkill <- SkillQuery
                  .filter(s => s.id === skill.id || s.name.toLowerCase === skill.name.toLowerCase)
                  .joinLeft(UserSkillQuery.filter(_.userId === userId))
                  .on(_.id === _.skillId)
                  .result.headOption
                _ <- foundSkill match {
                  case Some((_, Some(_))) =>
                    DBIO.successful(())
                  case Some((s, None)) =>
                    linkSkill(userId, s.id) map (_ => ())
                  case None =>
                    for {
                      sId <- (SkillQuery returning SkillQuery.map(_.id))
                          .+=(DBSkill(0, skill.name.toLowerCase, None, request = true))
                      _ <- linkSkill(userId, sId)
                    } yield ()
                }
              } yield ()
            }
          }
        } yield ()
      case None => DBIO.successful(())
    }

    val actions = for {
      dbUser <- query.result.headOption
      userId <- dbUser match {
        case Some(_) =>
          query.map(_.updatableFields).update(user.updatableFields).map(_ => user.id)
        case None =>
          (UsersQuery returning UsersQuery.map(_.id)) += user
      }
      _ <- updateSkills(userId)
    } yield userId

    db.run(actions.transactionally).map(userId => user.copy(id = userId))
  }

  override def delete(id: Long) = {
    val action = for {
      _ <- UserSkillQuery.filter(_.userId === id).delete
      _ <- UserFavoriteQuery.filter(uf => uf.userId === id || uf.otherId === id).delete
      _ <- ActivityTeachEventFavoriteQuery.filter(_.userId === id).delete
      _ <- ActivityResearchFavoriteQuery.filter(_.userId === id).delete
      _ <- ActivityResearchRoleAppQuery.filter(_.userId === id).delete
      _ <- ActivityResearchTeamQuery.filter(_.userId === id).delete

      activitySubsQ = ActivityTeachEventSubQuery.filter(_.userId === id)
      paymentInfoIds <- activitySubsQ.map(_.paymentInfoId).result
      _ <- activitySubsQ.delete
      _ <- PaymentInfoQuery.filter(_.id inSet paymentInfoIds.flatten).delete

      _ <- BazaarCommentQuery.filter(_.userId === id).delete
      _ <- BazaarPreferenceQuery.filter(_.userId === id).delete
      _ <- BazaarIdeaGuestQuery.filter(_.userId === id).delete

      learnIds <- BazaarLearnQueryAll.filter(_.creatorId === id).map(_.id).result
      _ <- DBIO.sequence(learnIds.map(deleteLearnAction))
      teachIds <- BazaarTeachQueryAll.filter(_.creatorId === id).map(_.id).result
      _ <- DBIO.sequence(teachIds.map(deleteTeachAction))
      eventIds <- BazaarEventQueryAll.filter(_.creatorId === id).map(_.id).result
      _ <- DBIO.sequence(eventIds.map(deleteEventAction))
      researchIds <- BazaarResearchQuery.filter(_.creatorId === id).map(_.id).result
      _ <- DBIO.sequence(researchIds.map(deleteResearchAction))

      fablabReservationQ = FablabReservationQuery.filter(_.userId === id)
      _ <- FablabReservationTimeQuery.filter(_.fablabReservationId in fablabReservationQ.map(_.id)).delete
      _ <- fablabReservationQ.delete

      fablabQuotationQ = FablabQuotationQuery.filter(_.userId === id)
      _ <- FablabQuotationMachineQuery.filter(_.quotationId in fablabQuotationQ.map(_.id)).delete
      _ <- fablabQuotationQ.delete

      _ <- MembershipsQuery.filter(_.userId === id).delete
      _ <- AccessTokenQuery.filter(_.userId === id).delete
      _ <- PasswordInfoQuery.filter(_.userId === id).delete
      _ <- MailTokenQuery.filter(_.userId === id).delete
      n <- UsersQuery.filter(_.id === id).delete
    } yield n == 1

    db run action.transactionally
  }

  override def search(userId: Long, value: Option[String], skillIds: Seq[Long], matchAllSkills: Boolean): Future[Seq[User]] = {
    if (value.isEmpty && skillIds.isEmpty)
      return Future.failed(new IllegalArgumentException("A value or skills must be supplied"))

    val baseQ = UsersQuery.joinLeft(UserFavoriteQuery.filter(_.userId === userId)).on(_.id === _.otherId)

    val filterByName = value match {
      case Some(v) =>
        val searchTerm = s"%${v.toLowerCase}%"
        baseQ.filter { case (u, _) =>
          val uName = u.firstName ++ " " ++ u.lastName
          uName.toLowerCase like searchTerm
        }
      case None => baseQ
    }

    val query = if (skillIds.isEmpty) {
      filterByName
    } else {
      val joined = filterByName.join(UserSkillQuery).on(_._1.id === _.userId)
      val subQuery = UserSkillQuery.filter(_.skillId inSet skillIds)

      val skillFiltered = if (matchAllSkills) {
        joined.filter {
            case (_, us) => us.userId in {
              //noinspection ScalaDeprecation
              subQuery.groupBy(_.userId)
                // using group.distinctOn(_.skillId).length throws runtime error
                // https://github.com/slick/slick/issues/1760
                .map { case (uId, group) => (uId, group.countDistinct) }
                .filter(_._2 === skillIds.length)
                .map(_._1)
            }
          }
      } else {
        joined.filter {
            case (_, us) => us.userId.in(subQuery.map(_.userId))
          }
      }

      skillFiltered.map(_._1).distinctOn(_._1.id)
    }

    db run query.result map (_.map {
      case (u, optF) => dbUserToUser(u).copy(favorite = if (optF.isDefined) Some(true) else Some(false))
    })
  }

  override def favorite(userId: Long, otherId: Long, favorite: Boolean) = {
    val query = UserFavoriteQuery.filter(f => f.userId === userId && f.otherId === otherId)

    val action = for {
      exists <- query.exists.result
      n <- {
        if (exists == favorite) DBIO.successful(1)
        else if (favorite) UserFavoriteQuery += DBUserFavorite(userId, otherId, currentTimestamp())
        else query.delete
      }
    } yield n == 1

    db run action
  }

  override def favorites(userId: Long) = {
    val query = UserFavoriteQuery.filter(_.userId === userId).join(UsersQuery).on(_.otherId === _.id)

    db run query.result map (_.map {
      case (_, user) => (user: User).copy(favorite = Some(true))
    })
  }

  override def updateRole(userId: Long, userRole: UserRole) = {
    val action = UsersQuery.filter(_.id === userId).map(_.role).update(userRole)
    db run action.map(_ == 1)
  }

}
