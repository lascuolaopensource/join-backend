package services

import java.sql.Timestamp

import javax.inject.{Inject, Singleton}
import models.{BazaarIdeaStatRow, BazaarIdeasCount, BazaarTopCreator}
import models.daos.BazaarStatDAO

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class BazaarIdeaStatService @Inject()(
  bazaarStatDAO: BazaarStatDAO
)(implicit ec: ExecutionContext)
  extends StatService
{

  def latestIdeas(to: Timestamp, limit: Int = defaultLimit): Future[Seq[BazaarIdeaStatRow]] =
    bazaarStatDAO.latestIdeas(to, limit)

  def count(to: Timestamp): Future[BazaarIdeasCount] =
    bazaarStatDAO.count(to)

  def topScoredEvent(from: Timestamp, to: Timestamp, limit: Int = defaultLimit): Future[Seq[BazaarIdeaStatRow]] =
    bazaarStatDAO.topScoredEvent(from, to, limit)
  def topScoredTeach(from: Timestamp, to: Timestamp, limit: Int = defaultLimit): Future[Seq[BazaarIdeaStatRow]] =
    bazaarStatDAO.topScoredTeach(from, to, limit)
  def topScoredResearch(from: Timestamp, to: Timestamp, limit: Int = defaultLimit): Future[Seq[BazaarIdeaStatRow]] =
    bazaarStatDAO.topScoredResearch(from, to, limit)

  def topCreators(from: Timestamp, to: Timestamp, limit: Int = defaultLimit): Future[Seq[BazaarTopCreator]] =
    bazaarStatDAO.topCreators(from, to, limit)

  def topEver(limit: Int = defaultLimit): Future[Seq[BazaarIdeaStatRow]] =
    bazaarStatDAO.topEver(limit)

}
