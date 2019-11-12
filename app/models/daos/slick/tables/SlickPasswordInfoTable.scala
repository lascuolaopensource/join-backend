package models.daos.slick.tables

import com.mohiva.play.silhouette.api.util.PasswordInfo
import slick.lifted.ProvenShape

import scala.language.implicitConversions


trait SlickPasswordInfoTable extends SlickTable {

  import profile.api._

  // PasswordInfo table
  case class DBPasswordInfo(id: Long, hasher: String, password: String, salt: Option[String], userId: Long)

  protected class PasswordInfoTable(tag: Tag) extends Table[DBPasswordInfo](tag, "password_info") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def hasher: Rep[String] = column[String]("hasher")
    def password: Rep[String] = column[String]("password")
    def salt: Rep[Option[String]] = column[Option[String]]("salt")
    def userId: Rep[Long] = column[Long]("user_id")
    override def * : ProvenShape[DBPasswordInfo] = (id, hasher, password, salt, userId) <> (DBPasswordInfo.tupled, DBPasswordInfo.unapply)
  }

  val PasswordInfoQuery: TableQuery[PasswordInfoTable] = TableQuery[PasswordInfoTable]



  implicit def dbPasswordInfoToPasswordInfo(dbPasswordInfo: DBPasswordInfo): PasswordInfo = PasswordInfo(
    hasher = dbPasswordInfo.hasher,
    password = dbPasswordInfo.password,
    salt = dbPasswordInfo.salt
  )

}
