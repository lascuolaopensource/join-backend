package services

import java.sql.Timestamp

import javax.inject.{Inject, Singleton}
import models._
import models.daos.ActivityStatDAO

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class ActivityStatService @Inject()(
  activityStatDAO: ActivityStatDAO
)(implicit ec: ExecutionContext)
 extends StatService
{

  def next(from: Timestamp, lang: Language, limit: Int = defaultLimit): Future[Seq[ActivityStat]] =
    activityStatDAO.next(from, limit, lang)

  def top(from: Timestamp, to: Timestamp, lang: Language, limit: Int = defaultLimit): Future[Seq[ActivityStat]] =
    activityStatDAO.top(from, to, limit, lang)

  def count(date: Timestamp): Future[ActivityStatCount] =
    activityStatDAO.count(date)

  def topProjects(from: Timestamp, to: Timestamp, lang: Language, limit: Int = defaultLimit): Future[Seq[ActivityProjectStat]] =
    activityStatDAO.topProjects(from, to, limit, lang)

  def countProjects(from: Timestamp, to: Timestamp): Future[ActivityProjectStatCount] =
    activityStatDAO.countProjects(from, to)

}
