package models.daos.slick

import java.sql.{Date, Timestamp}

import javax.inject.Inject
import models.UserStat
import models.daos.UserStatDAO
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext


private object SlickUserStatDAO {
  type UserStatTuple = (Long, String, String, Int)
}

class SlickUserStatDAO @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit val ec: ExecutionContext)
  extends UserStatDAO with SlickDAO
{

  import profile.api._
  import SlickUserStatDAO._

  implicit class UserCountQueryOps(query: Query[(Rep[Long], Rep[Int]), (Long, Int), Seq]) {
    def getTopAndJoinUser(limit: ConstColumn[Long]) =
      (for {
        (uId, count) <- query.sortBy(_._2.desc).take(limit)
        user <- UsersQuery if uId === user.id
      } yield (uId, user.firstName, user.lastName, count))
      .sortBy(_._4.desc)
  }

  implicit class StatTuplesOps(p1: Seq[UserStatTuple]) {
    def withTrendsFrom(p0: Seq[UserStatTuple]) =
      p1 zip computeTrends(p0.map(_._1), p1.map(_._1)) map {
        case ((id, fName, lName, count), trend) => UserStat(id, fName, lName, count, trend)
      }
  }

  private lazy val topTeachingQ = Compiled {
    (to: Rep[Timestamp], limit: ConstColumn[Long]) =>
      ActivityTeachEventQuery.filter(a => a.isTeach && a.createdAt < to)
        .join(BazaarTeachLearnQuery).on(_.bazaarTeachLearnId === _.id)
        .groupBy(_._2.creatorId)
        .map { case (uId, g) => (uId, g.length) }
        .getTopAndJoinUser(limit)
  }

  override def topTeaching(from: Timestamp, to: Timestamp, limit: Int) = {
    val action = for {
      p1 <- topTeachingQ(to, limit).result
      p0 <- topTeachingQ(from, limit).result
    } yield p1 withTrendsFrom p0

    db run action
  }

  private lazy val topResearchQ = Compiled {
    (to: Rep[Timestamp], limit: ConstColumn[Long]) =>
      ActivityResearchQuery.filter(_.createdAt < to)
        .join(BazaarResearchQuery).on(_.bazaarResearchId === _.id)
        .groupBy(_._2.creatorId)
        .map { case (uId, g) => (uId, g.length) }
        .getTopAndJoinUser(limit)
  }

  override def topResearch(from: Timestamp, to: Timestamp, limit: Int) = {
    val action = for {
      p1 <- topResearchQ(to, limit).result
      p0 <- topResearchQ(from, limit).result
    } yield p1 withTrendsFrom p0

    db run action
  }

  private lazy val topIdeasQ = Compiled {
    (to: Rep[Timestamp], limit: ConstColumn[Long]) =>
      ( BazaarTeachLearnQuery.filter(i => i.deletedAt.isEmpty && i.createdAt < to).map(_.creatorId) ++
        BazaarEventQuery.filter(_.createdAt < to).map(_.creatorId) ++
        BazaarResearchQuery.filter(_.createdAt < to).map(_.creatorId))
        .groupBy(identity)
        .map { case (uId, g) => (uId, g.length) }
        .getTopAndJoinUser(limit)
  }

  override def topIdeas(from: Timestamp, to: Timestamp, limit: Int) = {
    val action = for {
      p1 <- topIdeasQ(to, limit).result
      p0 <- topIdeasQ(from, limit).result
    } yield p1 withTrendsFrom p0

    db run action
  }

  private lazy val topSkillsQ = Compiled {
    (to: Rep[Timestamp], limit: ConstColumn[Long]) =>
      UserSkillQuery.filter(_.createdAt < to)
        .groupBy(_.userId)
        .map { case (uId, g) => (uId, g.length) }
        .getTopAndJoinUser(limit)
  }

  override def topSkills(from: Timestamp, to: Timestamp, limit: Int) = {
    val action = for {
      p1 <- topSkillsQ(to, limit).result
      p0 <- topSkillsQ(from, limit).result
    } yield p1 withTrendsFrom p0

    db run action
  }

  private lazy val topMakerQ = Compiled {
    (from: Rep[Date], to: Rep[Date], limit: ConstColumn[Long]) =>
      FablabReservationTimeQuery.filter(r => r.date > from && r.date <= to)
        .join(FablabReservationQuery).on(_.fablabReservationId === _.id)
        .groupBy(_._2.userId)
        .map { case (uId, g) => (uId, g.length) }
        .getTopAndJoinUser(limit)
  }

  override def topMaker(from: Timestamp, to: Timestamp, limit: Int) = {
    val fromD = new Date(from.getTime)
    val toD = new Date(to.getTime)
    val from0D = new Date(from.getTime - (to.getTime - from.getTime))

    val action = for {
      p1 <- topMakerQ(fromD, toD, limit).result
      p0 <- topMakerQ(from0D, fromD, limit).result
    } yield p1 withTrendsFrom p0

    db run action
  }

  private lazy val topFavoredQ = Compiled {
    (from: Rep[Timestamp], to: Rep[Timestamp], limit: ConstColumn[Long]) =>
      UserFavoriteQuery.filter(f => f.createdAt > from && f.createdAt <= to)
        .groupBy(_.otherId)
        .map { case (uId, g) => (uId, g.length) }
        .getTopAndJoinUser(limit)
  }

  override def topFavored(from: Timestamp, to: Timestamp, limit: Int) = {
    val from0 = new Timestamp(from.getTime - (to.getTime - from.getTime))

    val action = for {
      p1 <- topFavoredQ(from, to, limit).result
      p0 <- topFavoredQ(from0, from, limit).result
    } yield p1 withTrendsFrom p0

    db run action
  }

  private lazy val topFavoritesQ = Compiled {
    (from: Rep[Timestamp], to: Rep[Timestamp], limit: ConstColumn[Long]) =>
      UserFavoriteQuery.filter(f => f.createdAt > from && f.createdAt <= to)
        .groupBy(_.userId)
        .map { case (uId, g) => (uId, g.length) }
        .getTopAndJoinUser(limit)
  }

  override def topFavorites(from: Timestamp, to: Timestamp, limit: Int) = {
    val from0 = new Timestamp(from.getTime - (to.getTime - from.getTime))

    val action = for {
      p1 <- topFavoritesQ(from, to, limit).result
      p0 <- topFavoritesQ(from0, from, limit).result
    } yield p1 withTrendsFrom p0

    db run action
  }

}
