package models.daos.slick.tables

import java.sql.Timestamp


private[slick] object SlickUserFavoriteTable {

  case class DBUserFavorite(userId: Long, otherId: Long, createdAt: Timestamp)

}


trait SlickUserFavoriteTable extends SlickTable {

  import profile.api._
  import SlickUserFavoriteTable._

  protected class UserFavoriteTable(tag: Tag) extends Table[DBUserFavorite](tag, "user_favorite") {
    def userId: Rep[Long] = column("user_id")
    def otherId: Rep[Long] = column("other_user_id")
    def createdAt: Rep[Timestamp] = column("created_at")
    def pk = primaryKey("pk", (userId, otherId))
    override def * =
      (userId, otherId, createdAt) <> (DBUserFavorite.tupled, DBUserFavorite.unapply)
  }

  protected val UserFavoriteQuery: TableQuery[UserFavoriteTable] = TableQuery[UserFavoriteTable]

}
