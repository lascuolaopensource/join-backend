package models.daos.slick

import java.sql.Timestamp

import models._
import models.daos.slick.tables.SlickBazaarTeachLearnTable.{DBBazaarTeachLearn, DBBazaarTeachLearnS}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}


private[slick] object SlickBazaarTeachDAO {
  def apply(dbConfigProvider: DatabaseConfigProvider, defaultDeadline: Int)
    (implicit ec: ExecutionContext): SlickBazaarIdeaSpecificDAO[BazaarTeach, DBBazaarTeachLearn, DBBazaarTeachLearnS] =
      new SlickBazaarTeachDAO(dbConfigProvider, defaultDeadline)
}

private[slick] class SlickBazaarTeachDAO(
  protected val dbConfigProvider: DatabaseConfigProvider,
  protected val defaultDeadline: Int
)(implicit val ec: ExecutionContext)
  extends SlickBazaarIdeaSpecificDAO[BazaarTeach, DBBazaarTeachLearn, DBBazaarTeachLearnS]
    with SlickBazaarIdeaHelpers {

  import profile.api._


  override protected type TableTypeS = BazaarTeachLearnTableS

  override protected def table2Disabled(t: TableTypeS) = t.disabled
  private val timestampPlusDD = timestampPlus(defaultDeadline)
  override protected def table2PastDeadline(t: TableTypeS, now: Timestamp) =
    timestampPlusDD(t.createdAt) < now

  override def allSlim(userId: Long, disabled: Boolean) = db run (for {
    ideaUserPrefAct <- allQuery.includeDisabled(disabled).includePastDeadline(disabled)
      .join(UsersQuery).on(_.creatorId === _.id)
      .joinLeft(BazaarPreferenceQuery.filter(_.userId === userId)).on(_._1.id === _.bazaarTeachLearnId)
      .joinLeft(ActivityTeachEventQuery).on(_._1._1.id === _.bazaarTeachLearnId).result
    topics <- BazaarIdeaTopicQuery.filter(_.bazaarTeachLearnId inSet ideaUserPrefAct.map(_._1._1._1.id))
      .join(TopicQuery).on(_.topicId === _.id)
      .map { case (l, t) => (l.bazaarTeachLearnId.get, t.id, t.topic) }.result
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
          ideaType = BazaarTeachType,
          preference = pref.map(_.toSlim),
          score = idea.score,
          counts = getPreferenceCounts(idea.views, idea.agrees, idea.wishes, idea.comments, idea.favorites),
          activityId = act.map(_.id))
    }
  })

  override def enhance(idea: DBBazaarTeachLearnS): Future[BazaarTeach] = {
    val queries = for {
      abstractIdea <- BazaarAbstractIdeaQuery.filter(_.id === idea.bazaarAbstractIdeaId).result.head
      audience <- BazaarIdeaAudienceQuery.filter(_.bazaarAbstractIdeaId === abstractIdea.id).result
      meetings <- BazaarIdeaMeetingDurationQuery.filter(_.bazaarAbstractIdeaId === abstractIdea.id).result
      dates <- BazaarIdeaDateQuery.filter(_.bazaarAbstractIdeaId === abstractIdea.id).result
      funding <- BazaarIdeaFundingQuery.filter(_.bazaarAbstractIdeaId === abstractIdea.id).result
    } yield (abstractIdea, audience, meetings, dates, funding)

    db run queries flatMap {
      case (abstractIdea, audience, meetings, dates, funding) =>
        enhanceBazaarLearn(idea) map { learnIdea =>
          BazaarTeach(learnIdea)(
            activityType = idea.activityType.get,
            audience = audience.map(_.audienceType: Audience),
            level = idea.level.get,
            requiredResources = abstractIdea.requiredResources,
            meetings = getMeetings(abstractIdea, meetings),
            dates = dates.map(dbBazaarDateToDate),
            funding = funding.map(dbFundingToFunding),
            maxParticipants = abstractIdea.maxParticipants,
            programDetails = abstractIdea.programDetails,
            meetingDetails = idea.meetingDetails.get,
            outputDetails = idea.outputDetails.get
          )
        }
    }
  }

  override protected def allQuery = BazaarTeachQueryS

  override protected def findQuery(id: Long) = BazaarTeachQueryS.filter(_.id === id)

  override def create(bazaarTeach: BazaarTeach): Future[BazaarTeach] = {
    val now = currentTimestamp()
    val dbBazaarTeachLearn: DBBazaarTeachLearn = bazaarTeach.copy(id = 0, createdAt = now, updatedAt = now)

    val actions = for {
      abstractIdeaId <- insertAbstractIdea(
        bazaarTeach.meetings, bazaarTeach.requiredResources, bazaarTeach.maxParticipants, bazaarTeach.programDetails)

      bazaarTeachId <- (BazaarTeachLearnQuery returning BazaarTeachLearnQuery.map(_.id)) +=
        dbBazaarTeachLearn.copy(bazaarAbstractIdeaId = Some(abstractIdeaId))

      newTopics <- insertTopics(bazaarTeach.topics, bazaarTeachId, BazaarTeachType)
      newTeachers <- insertGuests(bazaarTeach.teachers, bazaarTeachId, isTeachLearn = true)
      newTutors <- insertGuests(bazaarTeach.tutors, bazaarTeachId, isTeachLearn = true)

      _ <- insertAudience(bazaarTeach.audience, abstractIdeaId)
      _ <- insertMeetings(bazaarTeach.meetings, abstractIdeaId)
      _ <- insertDates(bazaarTeach.dates, abstractIdeaId)
      _ <- insertFunding(bazaarTeach.funding, abstractIdeaId)
    } yield (bazaarTeachId, newTopics, newTeachers, newTutors)

    db run actions.transactionally map {
      case (bazaarTeachId, newTopics, newTeachers, newTutors) =>
        bazaarTeach.copy(
          id = bazaarTeachId,
          topics = newTopics,
          teachers = newTeachers,
          tutors = newTutors
        )
    }
  }

  override def update(bazaarTeach: BazaarTeach): Future[BazaarTeach] = {
    val updatedBazaarTeach = bazaarTeach.copy(updatedAt = currentTimestamp())
    val bazaarTeachId = updatedBazaarTeach.id
    val abstractIdea = getAbstractIdea(updatedBazaarTeach.meetings, updatedBazaarTeach.requiredResources,
      updatedBazaarTeach.maxParticipants, updatedBazaarTeach.programDetails)

    val actions = for {
      abstractIdeaId <- updateAbstractIdea(bazaarTeachId, abstractIdea, isTeach = true)

      ideaWithAbstractId = bazaarTeachToDB(updatedBazaarTeach).copy(bazaarAbstractIdeaId = Some(abstractIdeaId))
      _ <- BazaarTeachLearnQuery.filter(_.id === bazaarTeachId).map(_.updatableFields)
        .update(ideaWithAbstractId.updatableFields)

      updatedTopics <- updateTopics(updatedBazaarTeach.topics, bazaarTeachId, BazaarTeachType)

      updatedTeachers <- updateGuests(updatedBazaarTeach.teachers, bazaarTeachId, isTeachLearn = true)
      updatedTutors <- updateGuests(updatedBazaarTeach.tutors, bazaarTeachId, isTeachLearn = true, tutor = true)

      _ <- updateAudience(updatedBazaarTeach.audience, abstractIdeaId)
      updatedMeetings <- updateMeetings(updatedBazaarTeach.meetings, abstractIdeaId)
      updatedDates <- updateDates(updatedBazaarTeach.dates, abstractIdeaId)
      _ <- updateFunding(updatedBazaarTeach.funding, abstractIdeaId)
    } yield (updatedTopics, updatedTeachers, updatedTutors, updatedMeetings, updatedDates)

    db run actions.transactionally map {
      case (updatedTopics, updatedTeachers, updatedTutors, newMeetings, newDates) => updatedBazaarTeach.copy(
        topics = updatedTopics,
        teachers = updatedTeachers,
        tutors = updatedTutors,
        meetings = newMeetings,
        dates = newDates
      )
    }
  }

  override def delete(id: Long) = db run deleteTeachAction(id).transactionally

}
