package models.daos.slick

import java.sql.{Date, Timestamp}

import javax.inject.Inject
import models._
import models.daos.ActivityStatDAO
import play.api.db.slick.DatabaseConfigProvider
import slick.lifted.CanBeQueryCondition

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions


private[slick] object SlickActivityStatDAO {

  private def buildActivityStatF(id: Long, title: String, count: Int, start: Date, cls: String) =
    ActivityStat(
      id = id,
      title = title,
      subCount = count,
      startDate = start,
      activityClass = cls,
      trend = None)

  val buildActivityStat = (buildActivityStatF _).tupled

  implicit def string2ActivityClass(s: String): ActivityClass = s match {
    case "teach"    => ActivityTeachClass
    case "event"    => ActivityEventClass
    case "research" => ActivityResearchClass
    case x          => throw new MatchError(s"Wrong activity class $x")
  }

}


class SlickActivityStatDAO @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit val ec: ExecutionContext)
  extends ActivityStatDAO with SlickDAO
{

  import SlickActivityStatDAO._
  import profile.api._


  private def ActivityTeachEventStatQ[R <: Rep[_]](
    f: (ActivityTeachEventTable, ActivityTeachEventTTable, ActivityTeachEventSubTable) => R
  )(implicit wt: CanBeQueryCondition[R]) =
    ActivityTeachEventQuery
      .join(ActivityTeachEventTQuery).on(_.id === _.activityId)
      .join(ActivityTeachEventSubQuery).on(_._1.id === _.activityId)
      .filter { case ((a, aT), s) => f(a, aT, s) }
      .groupBy { case ((a, aT), _) =>
        (a.id, aT.title, a.startTime, a.isTeach) }
      .map { case ((id, title, st, it), group) =>
        (id, title, group.length, st.asColumnOf[Date], Case.If(it).Then("teach").Else("event"))
      }

  private val ActivityResearchJoined = ActivityResearchQuery
    .join(ActivityResearchTQuery).on(_.id === _.activityId)
    .join(ActivityResearchRoleQuery).on(_._1.id === _.activityResearchId)
    .join(ActivityResearchRoleAppQuery).on(_._2.id === _.activityResearchRoleId)

  private def ActivityResearchStatQ[R <: Rep[_]](
    f: (ActivityResearchTable, ActivityResearchTTable, ActivityResearchRoleAppTable) => R
  )(implicit wt: CanBeQueryCondition[R]) =
    ActivityResearchJoined
      .filter { case (((a, aT), _), aA) => f(a, aT, aA) }
      .groupBy { case (((a, aT), _), _) =>
        (a.id, aT.title, a.startDate) }
      .map { case ((id, title, sd), group) =>
        (id, title, group.length, sd, "research")
      }


  private def nextQ(from: Timestamp, limit: Int, lang: Language) =
    ActivityTeachEventStatQ((a, aT, _) =>
      aT.language === lang.language && a.startTime.isDefined && a.startTime > from)
    .union(
      ActivityResearchStatQ((a, aT, _) =>
        aT.language === lang.language && a.startDate > new Date(from.getTime)))
    .sortBy(_._4.asc)
    .take(limit)

  override def next(from: Timestamp, limit: Int, lang: Language) =
    db run nextQ(from, limit, lang).result map (_.map(buildActivityStat))


  private lazy val topQ = Compiled {
    (to: Rep[Timestamp], limit: ConstColumn[Long], lang: Rep[String]) =>
      ActivityTeachEventStatQ((a, aT, s) =>
        aT.language === lang && a.startTime.isDefined && s.createdAt < to)
        .union(
          ActivityResearchStatQ((_, aT, aA) =>
            aT.language === lang && aA.createdAt < to))
        .sortBy(_._3.desc)
        .take(limit)
  }

  override def top(from: Timestamp, to: Timestamp, limit: Int, lang: Language) = {
    val q = for {
      p1 <- topQ(to, limit, lang.language).result
      p0 <- topQ(from, limit, lang.language).result
    } yield p1 zip computeTrends(p0.map(_._1), p1.map(_._1))

    db run q map (_.map {
      case (tpl, trend) =>
        buildActivityStat(tpl).copy(trend = Some(trend))
    })
  }


  override def count(date: Timestamp) = {
    val action = for {
      programmedTE <- ActivityTeachEventQuery.filter(_.startTime > date).length.result
      programmedR <- ActivityResearchQuery.filter(_.startDate > new Date(date.getTime)).length.result
      te <- ActivityTeachEventQuery.filter(_.createdAt < date).map(_.isTeach).result
      teaching = te.count(identity)
      event = te.length - teaching
      research <- ActivityResearchQuery.filter(_.createdAt < date).length.result
      ideasTeach <- BazaarTeachLearnQuery.filter(i => i.deletedAt.isEmpty && i.createdAt < date).length.result
      ideasEvent <- BazaarEventQueryAll.filter(_.createdAt < date).length.result
      ideasResearch <- BazaarResearchQuery.filter(_.createdAt < date).length.result
    } yield ActivityStatCount(
      programmed = programmedTE + programmedR,
      teaching = teaching,
      event = event,
      research = research,
      ideasCount = ideasTeach + ideasEvent + ideasResearch)

    db run action
  }


  private lazy val topProjectsQ = Compiled {
    (to: Rep[Timestamp], limit: ConstColumn[Long], lang: Rep[String]) =>
      ActivityResearchJoined
        .filter { case (((_, aT), _), aA) =>
          aT.language === lang && aA.createdAt < to
        }
        .groupBy { case (((a, aT), _), _) =>
          (a.id, a.bazaarResearchId, aT.title) }
        .map { case ((id, bId, title), group) =>
          (id, bId, title, group.length)
        }
        .sortBy(_._4.desc)
        .take(limit)
        .joinLeft(BazaarResearchQuery.join(UsersQuery).on(_.creatorId === _.id))
        .on(_._2 === _._1.id)
  }

  override def topProjects(from: Timestamp, to: Timestamp, limit: Int, lang: Language) = {
    val action = for {
      p1 <- topProjectsQ(to, limit, lang.language).result
      p0 <- topProjectsQ(from, limit, lang.language).result
    } yield p1 zip computeTrends(p0.map(_._1._1), p1.map(_._1._1))

    db run action map (_.map {
      case (((id, _, title, subs), optUser), trend) =>
        ActivityProjectStat(id, title, subs, optUser.map(_._2.toShort), trend)
    })
  }


  override def countProjects(from: Timestamp, to: Timestamp) = {
    val query = for {
      ongoing <- sql""" SELECT count(DISTINCT id)
                      | FROM activity_research
                      | WHERE start_date <= $to
                      | AND (start_date + make_interval(days := duration)) > $to;
                    """.stripMargin.as[Int].head
      ideas <- BazaarResearchQuery.filter(_.createdAt < to).length.result
      users <- ActivityResearchRoleAppQuery
        .filter(a => a.createdAt > from && a.createdAt < to).length.result
    } yield ActivityProjectStatCount(ongoing, ideas, users)

    db run query
  }

}
