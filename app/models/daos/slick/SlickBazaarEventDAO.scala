package models.daos.slick

import java.sql.Timestamp

import models._
import models.daos.slick.tables.SlickBazaarEventTable.{DBBazaarEvent, DBBazaarEventS}
import models.daos.slick.tables.SlickTopicTable._
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}


private[slick] object SlickBazaarEventDAO {
  def apply(dbConfigProvider: DatabaseConfigProvider, defaultDeadline: Int)
    (implicit ec: ExecutionContext): SlickBazaarIdeaSpecificDAO[BazaarEvent, DBBazaarEvent, DBBazaarEventS] =
      new SlickBazaarEventDAO(dbConfigProvider, defaultDeadline)
}

private[slick] class SlickBazaarEventDAO(
  protected val dbConfigProvider: DatabaseConfigProvider,
  protected val defaultDeadline: Int
)(implicit val ec: ExecutionContext)
  extends SlickBazaarIdeaSpecificDAO[BazaarEvent, DBBazaarEvent, DBBazaarEventS]
    with SlickBazaarIdeaHelpers {

  import profile.api._


  override type TableTypeS = BazaarEventTableS

  override protected def table2Disabled(t: TableTypeS) = t.disabled
  private val timestampPlusDD = timestampPlus(defaultDeadline)
  override protected def table2PastDeadline(t: TableTypeS, now: Timestamp) =
    timestampPlusDD(t.createdAt) < now

  override def allSlim(userId: Long, disabled: Boolean): Future[Seq[BazaarIdeaSlim]] = db run (for {
    ideaUserPrefAct <- allQuery.includeDisabled(disabled).includePastDeadline(disabled)
      .join(UsersQuery).on(_.creatorId === _.id)
      .joinLeft(BazaarPreferenceQuery.filter(_.userId === userId)).on(_._1.id === _.bazaarEventId)
      .joinLeft(ActivityTeachEventQuery).on(_._1._1.id === _.bazaarEventId).result
    topics <- BazaarIdeaTopicQuery.filter(_.bazaarEventId inSet ideaUserPrefAct.map(_._1._1._1.id))
      .join(TopicQuery).on(_.topicId === _.id)
      .map { case (link, topic) => (link.bazaarEventId.get, topic.id, topic.topic) }.result
  } yield {
    val topicsMap = topics.groupBy(_._1).mapValues(_.map { case (_, id, name) => Topic(id, name) })
    ideaUserPrefAct.map {
      case (((idea, user), pref), act) =>
        BazaarIdeaSlim(
          id = idea.id,
          title = idea.title,
          creator = user.toShort,
          topics = topicsMap.getOrElse(idea.id, Seq()),
          createdAt = idea.createdAt,
          updatedAt = idea.updatedAt,
          ideaType = BazaarEventType,
          preference = pref.map(_.toSlim),
          score = idea.score,
          counts = getPreferenceCounts(idea.views, idea.agrees, idea.wishes, idea.comments, idea.favorites),
          activityId = act.map(_.id))
    }
  })

  override def enhance(event: DBBazaarEventS): Future[BazaarEvent] = {
    val queries = for {
      dbUser <- UsersQuery.filter(_.id === event.creatorId).result.head
      topics <- topicsQuery(event.id, BazaarEventType).result
      abstractIdea <- BazaarAbstractIdeaQuery.filter(_.id === event.bazaarAbstractIdeaId).result.head
      audience <- BazaarIdeaAudienceQuery.filter(_.bazaarAbstractIdeaId === abstractIdea.id).result
      meetings <- BazaarIdeaMeetingDurationQuery.filter(_.bazaarAbstractIdeaId === abstractIdea.id).result
      dates <- BazaarIdeaDateQuery.filter(_.bazaarAbstractIdeaId === abstractIdea.id).result
      funding <- BazaarIdeaFundingQuery.filter(_.bazaarAbstractIdeaId === abstractIdea.id).result
      extGuests <- guestsQuery(event.id).result
      guests <- guestUsersQuery(event.id).result
      activityId <- ActivityTeachEventQuery.filter(_.bazaarEventId === event.id).map(_.id).result.headOption
    } yield (dbUser, topics, abstractIdea, audience, meetings, dates, funding, extGuests, guests, activityId)

    db run queries map {
      case (dbUser, topics, abstractIdea, audience, meetings, dates, funding, extGuests, guests, activityId) =>
        BazaarEvent(
          id = event.id,
          title = event.title,
          creator = dbUser,
          topics = topics.map(dbTopicToTopic),
          activityType = event.activityType,
          audience = audience.map(_.audienceType: Audience),
          meetings = getMeetings(abstractIdea, meetings),
          dates = dates.map(dbBazaarDateToDate),
          requiredSpaces = event.requiredSpaces,
          maxParticipants = abstractIdea.maxParticipants,
          programDetails = abstractIdea.programDetails,
          valueDetails = event.valueDetails,
          motivation = event.motivation,
          requiredResources = abstractIdea.requiredResources,
          funding = funding.map(dbFundingToFunding),
          isOrganizer = event.isOrganizer,
          guests = transformGuests(extGuests, guests),
          bookingRequired = event.bookingRequired,
          activityId = activityId,
          createdAt = event.createdAt,
          updatedAt = event.updatedAt,
          score = event.score,
          counts = getPreferenceCounts(event.views, event.agrees, event.wishes, event.comments, event.favorites)
        )
    }
  }

  override protected def allQuery = BazaarEventQueryS.map(identity)

  override protected def findQuery(id: Long) = BazaarEventQueryS.filter(_.id === id)

  override def create(bazaarEvent: BazaarEvent): Future[BazaarEvent] = {
    val now = currentTimestamp()
    val dbBazaarEvent: DBBazaarEvent = bazaarEvent.copy(id = 0, createdAt = now, updatedAt = now)

    val actions = for {
      abstractIdeaId <- insertAbstractIdea(
        bazaarEvent.meetings, bazaarEvent.requiredResources, bazaarEvent.maxParticipants, bazaarEvent.programDetails)

      bazaarEventId <- (BazaarEventQueryAll returning BazaarEventQueryAll.map(_.id)) +=
        dbBazaarEvent.copy(bazaarAbstractIdeaId = abstractIdeaId)

      newTopics <- insertTopics(bazaarEvent.topics, bazaarEventId, BazaarEventType)
      newGuests <- insertGuests(bazaarEvent.guests, bazaarEventId, isTeachLearn = false)

      _ <- insertAudience(bazaarEvent.audience, abstractIdeaId)
      _ <- insertMeetings(bazaarEvent.meetings, abstractIdeaId)
      _ <- insertDates(bazaarEvent.dates, abstractIdeaId)
      _ <- insertFunding(bazaarEvent.funding, abstractIdeaId)
    } yield (bazaarEventId, newTopics, newGuests)

    db run actions.transactionally map {
      case (bazaarEventId, newTopics, newGuests) =>
        bazaarEvent.copy(
          id = bazaarEventId,
          topics = newTopics,
          guests = newGuests)
    }
  }

  override def update(bazaarEvent: BazaarEvent): Future[BazaarEvent] = {
    val updatedBazaarEvent = bazaarEvent.copy(updatedAt = currentTimestamp())
    val bazaarEventId = updatedBazaarEvent.id
    val abstractIdea = getAbstractIdea(updatedBazaarEvent.meetings, updatedBazaarEvent.requiredResources,
      updatedBazaarEvent.maxParticipants, updatedBazaarEvent.programDetails)

    val actions = for {
      abstractIdeaId <- updateAbstractIdea(bazaarEventId, abstractIdea, isTeach = false)

      ideaWithAbstractId = bazaarEventToDB(updatedBazaarEvent).copy(bazaarAbstractIdeaId = abstractIdeaId)
      _ <- BazaarEventQuery.filter(_.id === bazaarEventId).map(_.updatableFields)
        .update(ideaWithAbstractId.updatableFields)

      updatedTopics <- updateTopics(updatedBazaarEvent.topics, bazaarEventId, BazaarEventType)

      updatedGuests <- updateGuests(updatedBazaarEvent.guests, bazaarEventId, isTeachLearn = false)

      _ <- updateAudience(updatedBazaarEvent.audience, abstractIdeaId)
      updatedMeetings <- updateMeetings(updatedBazaarEvent.meetings, abstractIdeaId)
      updatedDates <- updateDates(updatedBazaarEvent.dates, abstractIdeaId)
      _ <- updateFunding(updatedBazaarEvent.funding, abstractIdeaId)
    } yield (updatedTopics, updatedGuests, updatedMeetings, updatedDates)

    db run actions.transactionally map {
      case (updatedTopics, updatedGuests, updatedMeetings, updatedDates) => updatedBazaarEvent.copy(
        topics = updatedTopics,
        guests = updatedGuests,
        meetings = updatedMeetings,
        dates = updatedDates)
    }
  }

  override def delete(id: Long) = db run deleteEventAction(id).transactionally

}
