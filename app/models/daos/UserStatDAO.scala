package models.daos

import java.sql.Timestamp

import models.UserStat

import scala.concurrent.Future


trait UserStatDAO {

  def topTeaching(from: Timestamp, to: Timestamp, limit: Int): Future[Seq[UserStat]]
  def topResearch(from: Timestamp, to: Timestamp, limit: Int): Future[Seq[UserStat]]
  def topIdeas(from: Timestamp, to: Timestamp, limit: Int): Future[Seq[UserStat]]
  def topSkills(from: Timestamp, to: Timestamp, limit: Int): Future[Seq[UserStat]]
  def topMaker(from: Timestamp, to: Timestamp, limit: Int): Future[Seq[UserStat]]
  def topFavored(from: Timestamp, to: Timestamp, limit: Int): Future[Seq[UserStat]]
  def topFavorites(from: Timestamp, to: Timestamp, limit: Int): Future[Seq[UserStat]]

}
