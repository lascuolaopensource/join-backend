package models.daos

import java.sql.Timestamp

import models._

import scala.concurrent.Future


trait ActivityStatDAO {

  def next(from: Timestamp, limit: Int, lang: Language): Future[Seq[ActivityStat]]

  def top(from: Timestamp, to: Timestamp, limit: Int, lang: Language): Future[Seq[ActivityStat]]

  def count(date: Timestamp): Future[ActivityStatCount]

  def topProjects(from: Timestamp, to: Timestamp, limit: Int, lang: Language): Future[Seq[ActivityProjectStat]]

  def countProjects(from: Timestamp, to: Timestamp): Future[ActivityProjectStatCount]

}
