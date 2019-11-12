package services

import java.sql.Timestamp

import javax.inject.{Inject, Singleton}
import models.UserStat
import models.daos.UserStatDAO

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class UserStatService @Inject()(
  userStatDAO: UserStatDAO
)(implicit ec: ExecutionContext)
  extends StatService
{

  def topTeaching(from: Timestamp, to: Timestamp, limit: Int = defaultLimit): Future[Seq[UserStat]] =
    userStatDAO.topTeaching(from, to, limit)

  def topResearch(from: Timestamp, to: Timestamp, limit: Int = defaultLimit): Future[Seq[UserStat]] =
    userStatDAO.topResearch(from, to, limit)

  def topIdeas(from: Timestamp, to: Timestamp, limit: Int = defaultLimit): Future[Seq[UserStat]] =
    userStatDAO.topIdeas(from, to, limit)

  def topSkills(from: Timestamp, to: Timestamp, limit: Int = defaultLimit): Future[Seq[UserStat]] =
    userStatDAO.topSkills(from, to, limit)

  def topMaker(from: Timestamp, to: Timestamp, limit: Int = defaultLimit): Future[Seq[UserStat]] =
    userStatDAO.topMaker(from, to, limit)

  def topFavored(from: Timestamp, to: Timestamp, limit: Int = defaultLimit): Future[Seq[UserStat]] =
    userStatDAO.topFavored(from, to, limit)

  def topFavorites(from: Timestamp, to: Timestamp, limit: Int = defaultLimit): Future[Seq[UserStat]] =
    userStatDAO.topFavorites(from, to, limit)

}
