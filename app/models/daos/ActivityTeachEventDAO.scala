package models.daos

import models._

import scala.concurrent.Future


trait ActivityTeachEventDAO {

  def all(language: Language, userId: Long, fromAdmin: Boolean, search: Option[String],
          searchSkillIds: Seq[Long], matchAll: Boolean, future: Boolean): Future[Seq[ActivityTeachEventSlim]]

  def allTeach(language: Language, userId: Long, future: Boolean): Future[Seq[ActivityTeachSlim]]
  def allEvent(language: Language, userId: Long, future: Boolean): Future[Seq[ActivityEventSlim]]

  def findTeach(id: Long, language: Language, userId: Long, future: Boolean): Future[Option[ActivityTeach]]
  def findEvent(id: Long, language: Language, userId: Long, future: Boolean): Future[Option[ActivityEvent]]

  def create[A <: ActivityTeachEvent[A]](activity: A): Future[A]

  def update[A <: ActivityTeachEvent[A]](activity: A): Future[Option[A]]

  def delete(id: Long): Future[Boolean]

  def getCoverPic(id: Long): Future[Option[(Boolean, String)]]

  def favorite(activityId: Long, userId: Long, favorite: Boolean): Future[Boolean]
  def favorites(userId: Long, language: Language): Future[Seq[ActivityTeachEventSlim]]

  def findSubscription(id: Long, userId: Long): Future[Option[ActivitySubscription]]
  def subscribe(id: Long, userId: Long, paymentInfo: Option[PaymentInfo]): Future[ActivitySubscription]
  def deleteSubscription(id: Long, userId: Long): Future[Boolean]
  def subscriptions(id: Long): Future[Seq[AdminActivitySubscription]]
  def verifySubscription(id: Long, userId: Long, success: Boolean): Future[AdminActivitySubscription]

}
