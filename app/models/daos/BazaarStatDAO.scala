package models.daos

import java.sql.Timestamp

import models.{BazaarIdeaStatRow, BazaarIdeasCount, BazaarTopCreator}

import scala.concurrent.Future


trait BazaarStatDAO {

  def latestIdeas(to: Timestamp, limit: Int): Future[Seq[BazaarIdeaStatRow]]

  def count(to: Timestamp): Future[BazaarIdeasCount]

  def topScoredEvent(from: Timestamp, to: Timestamp, limit: Int): Future[Seq[BazaarIdeaStatRow]]
  def topScoredTeach(from: Timestamp, to: Timestamp, limit: Int): Future[Seq[BazaarIdeaStatRow]]
  def topScoredResearch(from: Timestamp, to: Timestamp, limit: Int): Future[Seq[BazaarIdeaStatRow]]

  def topCreators(from: Timestamp, to: Timestamp, limit: Int): Future[Seq[BazaarTopCreator]]

  def topEver(limit: Int): Future[Seq[BazaarIdeaStatRow]]

}
