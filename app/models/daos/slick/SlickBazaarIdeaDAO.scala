package models.daos.slick

import javax.inject.Inject
import models._
import models.daos.BazaarIdeaDAO
import models.daos.slick.tables.SlickBazaarEventTable.dbBazaarEventS
import models.daos.slick.tables.SlickBazaarResearchTable.dbBazaarResearchS
import models.daos.slick.tables.SlickBazaarTeachLearnTable.dbBazaarTeachLearnS
import models.daos.slick.tables.SlickTopicTable.dbTopicToTopic
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}


class SlickBazaarIdeaDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider,
  protected val configuration: Configuration
)(implicit val ec: ExecutionContext)
  extends BazaarIdeaDAO with SlickBazaarIdeaHelpers {

  import profile.api._


  protected val defaultDeadline = configuration.get[Int]("bazaar.score.deadline")
  protected val timestampPlusDD = timestampPlus(defaultDeadline)

  private val learn = SlickBazaarLearnDAO(dbConfigProvider, defaultDeadline)
  override def allLearn(disabled: Boolean) = learn.all(disabled)
  override def findLearn(id: Long, disabled: Boolean) = learn.find(id, disabled)
  override def create(bazaarLearn: BazaarLearn) = learn.create(bazaarLearn)
  override def update(bazaarLearn: BazaarLearn) = learn.update(bazaarLearn)
  override def deleteLearn(id: Long) = learn.delete(id)

  private val teach = SlickBazaarTeachDAO(dbConfigProvider, defaultDeadline)
  override def allTeach(disabled: Boolean) = teach.all(disabled)
  override def findTeach(id: Long, disabled: Boolean) = teach.find(id, disabled)
  override def create(bazaarTeach: BazaarTeach) = teach.create(bazaarTeach)
  override def update(bazaarTeach: BazaarTeach) = teach.update(bazaarTeach)
  override def deleteTeach(id: Long) = teach.delete(id)

  private val event = SlickBazaarEventDAO(dbConfigProvider, defaultDeadline)
  override def allEvent(disabled: Boolean) = event.all(disabled)
  override def findEvent(id: Long, disabled: Boolean) = event.find(id, disabled)
  override def create(bazaarEvent: BazaarEvent) = event.create(bazaarEvent)
  override def update(bazaarEvent: BazaarEvent) = event.update(bazaarEvent)
  override def deleteEvent(id: Long) = event.delete(id)

  private val research = SlickBazaarResearchDAO(dbConfigProvider)
  override def allResearch(disabled: Boolean) = research.all(disabled)
  override def findResearch(id: Long, disabled: Boolean) = research.find(id, disabled)
  override def create(bazaarResearch: BazaarResearch) = research.create(bazaarResearch)
  override def update(bazaarResearch: BazaarResearch) = research.update(bazaarResearch)
  override def deleteResearch(id: Long) = research.delete(id)

  private implicit val slimOrdered: Ordering[BazaarIdeaSlim] = Ordering.by { idea: BazaarIdeaSlim =>
    idea.createdAt.getTime
  }.reverse

  override def allSlim(userId: Long, disabled: Boolean) = for {
    l <- learn.allSlim(userId, disabled)
    t <- teach.allSlim(userId, disabled)
    e <- event.allSlim(userId, disabled)
    r <- research.allSlim(userId, disabled)
  } yield (l ++ t ++ e ++ r).sorted

  override def searchTopics(topic: Option[String]): Future[Seq[Topic]] = {
    val query = topic match {
      case Some(t) => TopicQuery.filter(_.topic like s"%${t.toLowerCase}%")
      case None => TopicQuery
    }

    db run query.result map (_.map(dbTopicToTopic))
  }

  private def mapFutures[A, B](f: A => Future[B])(seq: Seq[A]): DBIO[Seq[B]] = DBIO.from(Future.sequence(seq.map(f)))

  override def search(value: String, disabled: Boolean) = {
    val valueLower = s"%${value.toLowerCase}%"

    val topicsSubQ = BazaarIdeaTopicQuery.join(TopicQuery).on(_.topicId === _.id).filter {
      case (_, t) => t.topic.toLowerCase like valueLower
    }

    val now = currentTimestamp()

    def teachLearnQuery(ideaType: BazaarIdeaType) = {
      val q = if (disabled) BazaarTeachLearnQuery
        else BazaarTeachLearnQuery.filter(i => i.disabled === false && timestampPlusDD(i.createdAt) > now)
      q.join(UsersQuery).on(_.creatorId === _.id).filter {
        case (idea, creator) =>
          idea.deletedAt.isEmpty && idea.ideaType === (if (ideaType == BazaarLearnType) 0 else 1) &&
            ((idea.title.toLowerCase like valueLower) ||
              ((creator.firstName ++ " " ++ creator.lastName).toLowerCase like valueLower) ||
              (idea.id in topicsSubQ.filter(_._1.bazaarTeachLearnId.isDefined).map(_._1.bazaarTeachLearnId)))
      }.map(_._1).result
    }

    val eventQuery = {
      val eq = if (disabled) BazaarEventQuery
        else BazaarEventQuery.filter(i => i.disabled === false && timestampPlusDD(i.createdAt) > now)
      eq.join(UsersQuery).on(_.creatorId === _.id).filter {
        case (idea, creator) =>
          (idea.title.toLowerCase like valueLower) ||
            ((creator.firstName ++ " " ++ creator.lastName).toLowerCase like valueLower) ||
            (idea.id in topicsSubQ.filter(_._1.bazaarEventId.isDefined).map(_._1.bazaarEventId))
      }.map(_._1).result
    }

    val researchQuery = {
      val rq = if (disabled) BazaarResearchQuery
        else BazaarResearchQuery.filter(i => i.disabled === false && timestampPlus(i.createdAt, i.deadline) > now)
      rq.join(UsersQuery).on(_.creatorId === _.id).filter {
        case (idea, creator) =>
          (idea.title.toLowerCase like valueLower) ||
            ((creator.firstName ++ " " ++ creator.lastName).toLowerCase like valueLower) ||
            (idea.id in topicsSubQ.filter(_._1.bazaarResearchId.isDefined).map(_._1.bazaarResearchId))
      }.map(_._1).result
    }

    val action = for {
      learn <- teachLearnQuery(BazaarLearnType).flatMap(mapFutures(enhanceLearn))
      teach <- teachLearnQuery(BazaarTeachType).flatMap(mapFutures(enhanceTeach))
      event <- eventQuery.flatMap(mapFutures(enhanceEvent))
      research <- researchQuery.flatMap(mapFutures(enhanceResearch))
    } yield learn ++ teach ++ event ++ research

    db run action
  }

  private val enhanceLearn = dbBazaarTeachLearnS.andThen(learn.enhance)
  private val enhanceTeach = dbBazaarTeachLearnS.andThen(teach.enhance)
  private val enhanceEvent = dbBazaarEventS.andThen(event.enhance)
  private val enhanceResearch = dbBazaarResearchS.andThen(research.enhance)

  override def favorites(userId: Long, disabled: Boolean) = {
    val now = currentTimestamp()

    val tlq = if (disabled) BazaarTeachLearnQuery
      else BazaarTeachLearnQuery.filter(i => i.disabled === false && timestampPlusDD(i.createdAt) > now)
    def teachLearnQuery(isTeach: Boolean, teachLearnIds: Seq[Long]) =
      tlq.filter(idea =>
        idea.deletedAt.isEmpty && idea.ideaType === (if (isTeach) 1 else 0) && (idea.id inSet teachLearnIds)).result

    val query = for {
      preferences <- BazaarPreferenceQuery.filter(p => p.userId === userId && p.favorite.isDefined).result

      teachLearnIds = preferences.filter(_.bazaarTeachLearnId.isDefined).map(_.bazaarTeachLearnId.get)
      learn <- teachLearnQuery(isTeach = false, teachLearnIds).flatMap(mapFutures(enhanceLearn))
      teach <- teachLearnQuery(isTeach = true, teachLearnIds).flatMap(mapFutures(enhanceTeach))

      eventIds = preferences.filter(_.bazaarEventId.isDefined).map(_.bazaarEventId.get)
      eq = if (disabled) BazaarEventQuery
        else BazaarEventQuery.filter(i => i.disabled === false && timestampPlusDD(i.createdAt) > now)
      event <- eq.filter(_.id inSet eventIds).result.flatMap(mapFutures(enhanceEvent))

      researchIds = preferences.filter(_.bazaarResearchId.isDefined).map(_.bazaarResearchId.get)
      rq = if (disabled) BazaarResearchQuery
        else BazaarResearchQuery.filter(i => i.disabled === false && timestampPlus(i.createdAt, i.deadline) > now)
      research <- rq.filter(_.id inSet researchIds).result.flatMap(mapFutures(enhanceResearch))
    } yield learn ++ teach ++ event ++ research

    db run query
  }

  override def byUser(userId: Long) = {
    val q = BazaarEventQuery.filter(_.creatorId === userId).map(r => (r.id, r.title, r.createdAt, 2))
        .union(
          BazaarResearchQuery.filter(_.creatorId === userId).map(r => (r.id, r.title, r.createdAt, 3)))
        .union(
          BazaarTeachLearnQuery.filter(t => t.deletedAt.isEmpty && t.creatorId === userId)
            .map(r => (r.id, r.title, r.createdAt, r.ideaType)))
        .sortBy(_._3.asc)

    db run q.result map (_.map {
      case (id, title, _, typeInt) =>
        val ty = typeInt match {
          case 0 => BazaarLearnType
          case 1 => BazaarTeachType
          case 2 => BazaarEventType
          case 3 => BazaarResearchType
          case n => throw new MatchError(s"Wrong bazaar idea type $n")
        }
        BazaarIdeaMini(id, title, ty)
    })
  }

}
