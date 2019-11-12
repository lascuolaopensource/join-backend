package models.daos.slick

import java.sql.Timestamp

import models.daos.slick.tables.SlickBazaarTeachLearnTable.{DBBazaarTeachLearn, DBBazaarTeachLearnS}
import models._
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}


private[slick] object SlickBazaarLearnDAO {
  def apply(dbConfigProvider: DatabaseConfigProvider, defaultDeadline: Int)
    (implicit ec: ExecutionContext): SlickBazaarIdeaSpecificDAO[BazaarLearn, DBBazaarTeachLearn, DBBazaarTeachLearnS] =
      new SlickBazaarLearnDAO(dbConfigProvider, defaultDeadline)
}

private[slick] class SlickBazaarLearnDAO(
  protected val dbConfigProvider: DatabaseConfigProvider,
  protected val defaultDeadline: Int
)(implicit val ec: ExecutionContext)
  extends SlickBazaarIdeaSpecificDAO[BazaarLearn, DBBazaarTeachLearn, DBBazaarTeachLearnS]
    with SlickBazaarIdeaHelpers {

  import profile.api._


  override protected type TableTypeS = BazaarTeachLearnTableS

  override protected def table2Disabled(t: TableTypeS) = t.disabled
  private val timestampPlusDD = timestampPlus(defaultDeadline)
  override protected def table2PastDeadline(t: TableTypeS, now: Timestamp) =
    timestampPlusDD(t.createdAt) < now

  override protected def allQuery = BazaarLearnQueryS

  override protected def findQuery(id: Long) = BazaarLearnQueryS.filter(_.id === id)

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
          ideaType = BazaarLearnType,
          preference = pref.map(_.toSlim),
          score = idea.score,
          counts = getPreferenceCounts(idea.views, idea.agrees, idea.wishes, idea.comments, idea.favorites),
          activityId = act.map(_.id))
    }
  })

  override def enhance(dbIdea: DBBazaarTeachLearnS) = enhanceBazaarLearn(dbIdea)

  override def create(bazaarLearn: BazaarLearn): Future[BazaarLearn] = {
    val now = currentTimestamp()
    val dbBazaarTeachLearn: DBBazaarTeachLearn = bazaarLearn.copy(id = 0, createdAt = now, updatedAt = now)

    val actions = for {
      bazaarLearnId <- (BazaarTeachLearnQuery returning BazaarTeachLearnQuery.map(_.id)) += dbBazaarTeachLearn
      newTopics <- insertTopics(bazaarLearn.topics, bazaarLearnId, BazaarLearnType)
      newTeachers <- insertGuests(bazaarLearn.teachers, bazaarLearnId, isTeachLearn = true)
      newTutors <- insertGuests(bazaarLearn.tutors, bazaarLearnId, isTeachLearn = true, tutor = true)
    } yield (bazaarLearnId, newTopics, newTeachers, newTutors)

    db run actions.transactionally map {
      case (bazaarLearnId, newTopics, newTeachers, newTutors) =>
        bazaarLearn.copy(
          id = bazaarLearnId,
          topics = newTopics,
          teachers = newTeachers,
          tutors = newTutors
        )
    }
  }

  override def update(bazaarLearn: BazaarLearn): Future[BazaarLearn] = {
    val updatedBazaarLearn = bazaarLearn.copy(updatedAt = currentTimestamp())
    val bazaarLearnId = updatedBazaarLearn.id

    val actions = for {
      _ <- BazaarTeachLearnQuery.filter(_.id === bazaarLearnId).map(_.updatableFields)
        .update(updatedBazaarLearn.updatableFields)

      updatedTopics <- updateTopics(updatedBazaarLearn.topics, bazaarLearnId, BazaarLearnType)

      updatedTeachers <- updateGuests(updatedBazaarLearn.teachers, bazaarLearnId, isTeachLearn = true)
      updatedTutors <- updateGuests(updatedBazaarLearn.tutors, bazaarLearnId, isTeachLearn = true, tutor = true)
    } yield (updatedTopics, updatedTeachers, updatedTutors)

    db run actions.transactionally map {
      case (updatedTopics, updatedTeachers, updatedTutors) => updatedBazaarLearn.copy(
        topics = updatedTopics,
        teachers = updatedTeachers,
        tutors = updatedTutors
      )
    }
  }

  override def delete(id: Long) = db run deleteLearnAction(id).transactionally

}
