package models.daos.slick

import java.sql.Date
import java.util.Calendar

import models.Topic
import models.daos.slick.tables.SlickTopicTable.DBTopic

import scala.language.implicitConversions


private[slick] object SlickActivityHelpers {

  sealed trait GenericQueryFilter { def applyFilter = true }

  final case class QueryTeach(future: Boolean) extends GenericQueryFilter
  final case class QueryEvent(future: Boolean) extends GenericQueryFilter

  sealed trait ActivitiesQueryFilter extends GenericQueryFilter

  final case object QueryNoFilter extends ActivitiesQueryFilter { override val applyFilter = false }

  final case class QueryBySearch(
    optSearch: Option[String], skillIds: Seq[Long],
    matchAll: Boolean, future: Boolean
  ) extends ActivitiesQueryFilter

  final case class QueryByIds(activityIds: Seq[Long]) extends ActivitiesQueryFilter

  implicit def seqIdsToQueryFilter(ids: Seq[Long]): ActivitiesQueryFilter = QueryByIds(ids)


  def deadlineClosed(deadline: Date, now: Long): Boolean = {
    val d = Calendar.getInstance()
    d.setTime(deadline)
    d.add(Calendar.DATE, 1)
    d.getTime.getTime < now
  }

}


private[slick] trait SlickActivityHelpers extends SlickDAO with SlickImageGalleryHelpers {

  import profile.api._
  import models.Implicits.DefaultWithId._

  protected def ActivityTopicQuery: TableQuery[_ <: SlickActivityTopicTable]

  protected def updateTopics(activityId: Long, topics: Seq[Topic]) =
    for {
      _ <- getDeletableIds(topics) match {
        case Seq() => DBIO.successful(())
        case deletableIds => ActivityTopicQuery
          .filter(at => at.activityId === activityId && at.topicId.inSet(deletableIds)).delete
      }

      creatable = getCreatable(topics)
      newIds <- (TopicQuery returning TopicQuery.map(_.id)) ++= creatable.map(t => DBTopic(0, t.topic.toLowerCase))

      existing = getExisting(topics)
      existingLinks <- ActivityTopicQuery.filter(_.activityId === activityId).map(_.topicId).result

      // those that are saved and have not been linked + new ones
      creatableWithIds = zipWithIds(creatable, newIds)
      topicsToLink = existing.filterNot(t => existingLinks.contains(t.id)) ++ creatableWithIds
      _ <- ActivityTopicQuery ++= topicsToLink.map(t => (activityId, t.id))
    } yield creatableWithIds ++ existing

  protected def ActivityFavoriteQuery: TableQuery[_ <: SlickActivityFavoriteTable]

  def favorite(activityId: Long, userId: Long, favorite: Boolean) = {
    val query = ActivityFavoriteQuery.filter(a => a.activityId === activityId && a.userId === userId)

    val action = for {
      exists <- query.exists.result
      n <- {
        if (exists == favorite) DBIO.successful(1)
        else if (favorite) ActivityFavoriteQuery += (activityId, userId)
        else query.delete
      }
    } yield n == 1

    db run action
  }

}
