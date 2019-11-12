package models.daos

import models.{BazaarIdeaType, BazaarPreference}

import scala.concurrent.Future


trait BazaarPreferenceDAO {

  def count(ideaId: Long, bazaarIdeaType: BazaarIdeaType): Future[(Int, Int, Int, Int)]
  def find(userId: Long, ideaId: Long, bazaarIdeaType: BazaarIdeaType): Future[Option[BazaarPreference]]
  def upsertFlags(bazaarPreference: BazaarPreference): Future[BazaarPreference]
  def upsertWish(bazaarPreference: BazaarPreference): Future[BazaarPreference]
  def deleteWish(bazaarPreference: BazaarPreference): Future[Option[BazaarPreference]]

}
