package services

import javax.inject.Inject

import models.daos.BazaarPreferenceDAO
import models.{BazaarIdeaType, BazaarPreference}

import scala.concurrent.Future


trait BazaarPreferenceService extends Service {

  def count(ideaId: Long, bazaarIdeaType: BazaarIdeaType): Future[(Int, Int, Int, Int)]
  def find(userId: Long, ideaId: Long, bazaarIdeaType: BazaarIdeaType): Future[Option[BazaarPreference]]
  def upsertWish(bazaarPreference: BazaarPreference): Future[BazaarPreference]
  def upsertFlags(bazaarPreference: BazaarPreference): Future[BazaarPreference]
  def deleteWish(bazaarPreference: BazaarPreference): Future[Option[BazaarPreference]]

}

class BazaarPreferenceServiceImpl @Inject()(
  bazaarPreferenceDAO: BazaarPreferenceDAO
) extends BazaarPreferenceService {

  override def count(ideaId: Long, bazaarIdeaType: BazaarIdeaType) = bazaarPreferenceDAO.count(ideaId, bazaarIdeaType)

  override def find(userId: Long, ideaId: Long, bazaarIdeaType: BazaarIdeaType) = bazaarPreferenceDAO.find(userId, ideaId, bazaarIdeaType)

  override def upsertWish(bazaarPreference: BazaarPreference) = bazaarPreferenceDAO.upsertWish(bazaarPreference)

  override def upsertFlags(bazaarPreference: BazaarPreference) = bazaarPreferenceDAO.upsertFlags(bazaarPreference)

  override def deleteWish(bazaarPreference: BazaarPreference) = bazaarPreferenceDAO.deleteWish(bazaarPreference)

}
