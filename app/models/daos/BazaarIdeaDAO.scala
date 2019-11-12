package models.daos

import models._

import scala.concurrent.Future

trait BazaarIdeaDAO {

  def allSlim(userId: Long, disabled: Boolean): Future[Seq[BazaarIdeaSlim]]

  def allTeach(disabled: Boolean): Future[Seq[BazaarTeach]]
  def allLearn(disabled: Boolean): Future[Seq[BazaarLearn]]
  def allEvent(disabled: Boolean): Future[Seq[BazaarEvent]]
  def allResearch(disabled: Boolean): Future[Seq[BazaarResearch]]

  def findTeach(id: Long, disabled: Boolean): Future[Option[BazaarTeach]]
  def findLearn(id: Long, disabled: Boolean): Future[Option[BazaarLearn]]
  def findEvent(id: Long, disabled: Boolean): Future[Option[BazaarEvent]]
  def findResearch(id: Long, disabled: Boolean): Future[Option[BazaarResearch]]

  def create(bazaarTeach: BazaarTeach): Future[BazaarTeach]
  def create(bazaarLearn: BazaarLearn): Future[BazaarLearn]
  def create(bazaarEvent: BazaarEvent): Future[BazaarEvent]
  def create(bazaarResearch: BazaarResearch): Future[BazaarResearch]

  def update(bazaarTeach: BazaarTeach): Future[BazaarTeach]
  def update(bazaarLearn: BazaarLearn): Future[BazaarLearn]
  def update(bazaarEvent: BazaarEvent): Future[BazaarEvent]
  def update(bazaarResearch: BazaarResearch): Future[BazaarResearch]

  def deleteTeach(id: Long): Future[Boolean]
  def deleteLearn(id: Long): Future[Boolean]
  def deleteEvent(id: Long): Future[Boolean]
  def deleteResearch(id: Long): Future[Boolean]

  def searchTopics(topic: Option[String]): Future[Seq[Topic]]

  def search(value: String, disabled: Boolean): Future[Seq[BazaarIdea]]

  def favorites(userId: Long, disabled: Boolean): Future[Seq[BazaarIdea]]

  def byUser(userId: Long): Future[Seq[BazaarIdeaMini]]

}
