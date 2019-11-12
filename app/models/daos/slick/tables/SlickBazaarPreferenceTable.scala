package models.daos.slick.tables

import java.sql.Timestamp

import models.BazaarPreferenceSlim
import slick.lifted.ProvenShape

private[slick] object SlickBazaarPreferenceTable {

  case class DBBazaarPreference(
    id: Long,
    bazaarTeachLearnId: Option[Long],
    bazaarEventId: Option[Long],
    bazaarResearchId: Option[Long],
    userId: Long,
    agree: Option[Timestamp],
    wishId: Option[Long],
    favorite: Option[Timestamp],
    viewed: Option[Timestamp])
  {
    def toSlim = BazaarPreferenceSlim(
      id = id,
      agree = agree.isDefined,
      wish = wishId.isDefined,
      favorite = favorite.isDefined,
      viewed = viewed.isDefined)
  }

}


trait SlickBazaarPreferenceTable extends SlickTable {

  import profile.api._
  import SlickBazaarPreferenceTable._

  protected class BazaarPreferenceTable(tag: Tag) extends Table[DBBazaarPreference](tag, "bazaar_preference") {
    def id: Rep[Long] = column("id", O.AutoInc, O.PrimaryKey)
    def bazaarTeachLearnId: Rep[Option[Long]] = column("bazaar_teach_learn_id")
    def bazaarEventId: Rep[Option[Long]] = column("bazaar_event_id")
    def bazaarResearchId: Rep[Option[Long]] = column("bazaar_research_id")
    def userId: Rep[Long] = column("user_id")
    def agree: Rep[Option[Timestamp]] = column("agree")
    def wishId: Rep[Option[Long]] = column("wish_id")
    def favorite: Rep[Option[Timestamp]] = column("favorite")
    def viewed: Rep[Option[Timestamp]] = column("viewed")
    override def * : ProvenShape[DBBazaarPreference] =
      (id, bazaarTeachLearnId, bazaarEventId, bazaarResearchId, userId, agree, wishId, favorite, viewed) <>
        (DBBazaarPreference.tupled, DBBazaarPreference.unapply)
  }

  val BazaarPreferenceQuery: TableQuery[BazaarPreferenceTable] = TableQuery[BazaarPreferenceTable]

}
