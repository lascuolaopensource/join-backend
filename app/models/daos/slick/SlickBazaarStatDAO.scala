package models.daos.slick

import java.sql.Timestamp

import javax.inject.Inject
import models._
import models.daos.BazaarStatDAO
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.GetResult
import slick.sql.SqlStreamingAction

import scala.concurrent.ExecutionContext


private[slick] object SlickBazaarStatDAO {

  implicit val getResIdeaStatRow = GetResult[BazaarIdeaStatRow] { p =>
    BazaarIdeaStatRow(
      ideaId = p.<<,
      title = p.<<,
      score = p.<<,
      createdAt = p.<<,
      userId = p.<<,
      firstName = p.<<,
      lastName = p.<<,
      ideaType = p.nextString() match {
        case "teach" => BazaarTeachType
        case "learn" => BazaarLearnType
        case "event" => BazaarEventType
        case "research" => BazaarResearchType
        case x => throw new MatchError(s"Wrong idea type $x")
      },
      trend = None)
  }

  implicit val getResTopCreator = GetResult[BazaarTopCreator] { p =>
    BazaarTopCreator(
      id = p.<<,
      firstName = p.<<,
      lastName = p.<<,
      average = p.<<,
      ideasCount = p.<<,
      trend = None)
  }

}


class SlickBazaarStatDAO @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit val ec: ExecutionContext)
  extends BazaarStatDAO with SlickDAO
{

  import SlickBazaarStatDAO._
  import profile.api._

  override def latestIdeas(to: Timestamp, limit: Int) = {
    val q = sql"""
         | SELECT
         |   bazaar_teach_learn.id,
         |   bazaar_teach_learn.title,
         |   s.score,
         |   bazaar_teach_learn.created_at,
         |   "user".id,
         |   first_name,
         |   last_name,
         |   CASE bazaar_teach_learn.type
         |   WHEN 1 THEN 'teach'
         |   ELSE 'learn'
         |   END
         | FROM bazaar_teach_learn
         | JOIN "user" ON bazaar_teach_learn.creator_id = "user".id,
         |  get_score(bazaar_teach_learn.id, 'teach', $to) s
         | WHERE bazaar_teach_learn.deleted_at IS NULL
         |  AND bazaar_teach_learn.created_at < $to
         | UNION
         | SELECT
         |   bazaar_event.id,
         |   bazaar_event.title,
         |   s.score,
         |   bazaar_event.created_at,
         |   "user".id,
         |   first_name,
         |   last_name,
         |   'event'
         | FROM bazaar_event
         | JOIN "user" ON bazaar_event.creator_id = "user".id,
         |  get_score(bazaar_event.id, 'event', $to) s
         | WHERE bazaar_event.deleted_at IS NULL
         |  AND bazaar_event.created_at < $to
         | UNION
         | SELECT
         |   bazaar_research.id,
         |   bazaar_research.title,
         |   s.score,
         |   bazaar_research.created_at,
         |   "user".id,
         |   first_name,
         |   last_name,
         |   'research'
         | FROM bazaar_research
         | JOIN "user" ON bazaar_research.creator_id = "user".id,
         |  get_score(bazaar_research.id, 'research', $to) s
         | WHERE bazaar_research.created_at < $to
         | ORDER BY created_at DESC
         | LIMIT $limit;
     """.stripMargin.as[BazaarIdeaStatRow]
    db run q
  }

  override def count(to: Timestamp) = {
    val q = for {
      t <- BazaarTeachLearnQuery.filter(i => i.deletedAt.isEmpty && i.createdAt < to).length.result
      e <- BazaarEventQuery.filter(_.createdAt < to).length.result
      r <- BazaarResearchQuery.filter(_.createdAt < to).length.result
    } yield BazaarIdeasCount(t, e, r)

    db run q
  }

  private def topScored_(
    from: Timestamp, to: Timestamp, limit: Int,
    f: (Timestamp, Int) => SqlStreamingAction[Vector[BazaarIdeaStatRow], BazaarIdeaStatRow, Effect]
  ) = db run {
    for {
      p1 <- f(to, limit)
      p0 <- f(from, limit)
      p1Trends = computeTrends(p0.map(_.ideaId), p1.map(_.ideaId))
    } yield p1 zip p1Trends map { case (r, t) => r.copy(trend = Some(t)) }
  }

  private def topScoredEventQ(to: Timestamp, limit: Int) =
    sql"""
         | SELECT
         |   be.id,
         |   be.title,
         |   s.score,
         |   be.created_at,
         |   "user".id,
         |   first_name,
         |   last_name,
         |   'event'
         | FROM bazaar_event be
         | JOIN "user" ON be.creator_id = "user".id,
         |   get_score(be.id, 'event', $to) s
         | WHERE be.deleted_at IS NULL AND be.created_at < $to
         | ORDER BY score DESC
         | LIMIT $limit;
      """.stripMargin.as[BazaarIdeaStatRow]

  override def topScoredEvent(from: Timestamp, to: Timestamp, limit: Int) =
    topScored_(from, to, limit, topScoredEventQ)

  private def topScoredTeachQ(to: Timestamp, limit: Int) =
    sql"""
         | SELECT
         |   btl.id,
         |   btl.title,
         |   s.score,
         |   btl.created_at,
         |   "user".id,
         |   first_name,
         |   last_name,
         |   CASE btl.type
         |   WHEN 1 THEN 'teach'
         |   ELSE 'learn'
         |   END
         | FROM bazaar_teach_learn btl
         | JOIN "user" ON btl.creator_id = "user".id,
         |   get_score(btl.id, 'teach', $to) s
         | WHERE btl.deleted_at IS NULL AND btl.created_at < $to
         | ORDER BY score DESC
         | LIMIT $limit;
      """.stripMargin.as[BazaarIdeaStatRow]

  override def topScoredTeach(from: Timestamp, to: Timestamp, limit: Int) =
    topScored_(from, to, limit, topScoredTeachQ)

  private def topScoredResearchQ(to: Timestamp, limit: Int) =
    sql"""
         | SELECT
         |   br.id,
         |   br.title,
         |   s.score,
         |   br.created_at,
         |   "user".id,
         |   first_name,
         |   last_name,
         |   'research'
         | FROM bazaar_research br
         | JOIN "user" ON br.creator_id = "user".id,
         |   get_score(br.id, 'research', $to) s
         | WHERE br.created_at < $to
         | ORDER BY score DESC
         | LIMIT $limit;
      """.stripMargin.as[BazaarIdeaStatRow]

  override def topScoredResearch(from: Timestamp, to: Timestamp, limit: Int) =
    topScored_(from, to, limit, topScoredResearchQ)

  private def topCreatorsQ(to: Timestamp, limit: Int) =
    sql"""
       | SELECT
       |   u.id,
       |   u.first_name,
       |   u.last_name,
       |   avg(scores.score) as average,
       |   count(*)
       | FROM "user" u
       | JOIN (
       |   SELECT creator_id, score
       |   FROM bazaar_teach_learn btl, get_score(btl.id, 'teach', $to)
       |   WHERE btl.deleted_at IS NULL AND btl.created_at < $to
       |   UNION ALL
       |   SELECT creator_id, score
       |   FROM bazaar_event be, get_score(be.id, 'event', $to)
       |   WHERE be.deleted_at IS NULL AND be.created_at < $to
       |   UNION ALL
       |   SELECT creator_id, score
       |   FROM bazaar_research br, get_score(br.id, 'research', $to)
       |   WHERE br.created_at < $to
       | ) scores ON scores.creator_id = u.id
       | GROUP BY u.id
       | ORDER BY average DESC
       | LIMIT $limit;
    """.stripMargin.as[BazaarTopCreator]

  override def topCreators(from: Timestamp, to: Timestamp, limit: Int) = {
    val q = for {
      p1 <- topCreatorsQ(to, limit)
      p0 <- topCreatorsQ(from, limit)
      p1Trends = computeTrends(p0.map(_.id), p1.map(_.id))
    } yield p1.zip(p1Trends).map { case (r, t) => r.copy(trend = Some(t)) }

    db run q
  }

  private def topEverQ(to: Timestamp, limit: Int) =
    sql"""
         | SELECT
         |   btl.id,
         |   btl.title,
         |   s.score,
         |   btl.created_at,
         |   u.id,
         |   u.first_name,
         |   u.last_name,
         |   CASE btl.type
         |   WHEN 1 THEN 'teach'
         |   ELSE 'learn'
         |   END
         | FROM bazaar_teach_learn btl
         | JOIN "user" u ON btl.creator_id = u.id
         | LEFT JOIN activity_teach_event ate ON btl.id = ate.bazaar_teach_learn_id,
         | get_score(btl.id, 'teach', coalesce(ate.created_at, $to)) s
         | WHERE btl.deleted_at IS NULL AND btl.created_at < $to
         | UNION
         | SELECT
         |   be.id,
         |   be.title,
         |   s.score,
         |   be.created_at,
         |   u.id,
         |   u.first_name,
         |   u.last_name,
         |   'event'
         | FROM bazaar_event be
         | JOIN "user" u ON be.creator_id = u.id
         | LEFT JOIN activity_teach_event ate ON be.id = ate.bazaar_event_id,
         | get_score(be.id, 'event', coalesce(ate.created_at, $to)) s
         | WHERE be.deleted_at IS NULL AND be.created_at < $to
         | UNION
         | SELECT
         |   br.id,
         |   br.title,
         |   s.score,
         |   br.created_at,
         |   u.id,
         |   u.first_name,
         |   u.last_name,
         |   'research'
         | FROM bazaar_research br
         | JOIN "user" u ON br.creator_id = u.id
         | LEFT JOIN activity_research ar ON br.id = ar.bazaar_research_id,
         | get_score(br.id, 'research', coalesce(ar.created_at, $to)) s
         | WHERE br.created_at < $to
         | ORDER BY score DESC
         | LIMIT $limit;
       """.stripMargin.as[BazaarIdeaStatRow]

  override def topEver(limit: Int) = {
    val now = currentTimestamp()

    val latestInsertedTimestampQ =
      sql"""
           | SELECT btl.created_at
           | FROM bazaar_teach_learn btl
           | WHERE btl.deleted_at IS NULL
           | UNION
           | SELECT be.created_at
           | FROM bazaar_event be
           | WHERE be.deleted_at IS NULL
           | UNION
           | SELECT br.created_at
           | FROM bazaar_research br
           | ORDER BY created_at DESC
           | LIMIT 1;
         """.stripMargin.as[Timestamp]

    val action = for {
      p1 <- topEverQ(now, limit)
      latestInsertedTimestamp <- latestInsertedTimestampQ
      p0 <- topEverQ(latestInsertedTimestamp.headOption.getOrElse(now), limit)
      p1Trends = computeTrends(p0.map(_.ideaId), p1.map(_.ideaId))
    } yield p1 zip p1Trends map { case (r, t) => r.copy(trend = Some(t)) }

    db run action
  }

}
