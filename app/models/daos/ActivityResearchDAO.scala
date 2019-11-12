package models.daos

import models._
import models.daos.ActivityResearchDAO.ChangeApplicationReply

import scala.concurrent.Future


object ActivityResearchDAO {

  sealed trait ChangeApplicationReply
  final case object RoleNotFound extends ChangeApplicationReply
  final case object DeadlinePassed extends ChangeApplicationReply
  final case class ApplicationReply(reply: Option[ActivityResearchApp]) extends ChangeApplicationReply

}

trait ActivityResearchDAO {

  def all(language: Language, userId: Long, fromAdmin: Boolean, search: Option[String],
          searchSkillIds: Seq[Long], matchAll: Boolean, future: Boolean): Future[Seq[ActivityResearchSlim]]

  def find(id: Long, language: Language, userId: Long, future: Boolean): Future[Option[ActivityResearch]]

  def create(activity: ActivityResearch): Future[ActivityResearch]

  def update(activity: ActivityResearch): Future[Option[ActivityResearch]]

  def delete(id: Long): Future[Boolean]

  def getCoverPic(id: Long): Future[Option[String]]

  def favorite(activityId: Long, userId: Long, favorite: Boolean): Future[Boolean]
  def favorites(userId: Long, language: Language): Future[Seq[ActivityResearchSlim]]

  def changeApplication(roleId: Long, applied: Boolean, app: ActivityResearchApp): Future[ChangeApplicationReply]
  def applications(activityId: Long): Future[Option[Seq[ActivityResearchRole]]]

  def userHasAccess(activityId: Long, userId: Long): Future[Boolean]

  def byUser(userId: Long, language: Language): Future[Seq[ActivityMini]]

}
