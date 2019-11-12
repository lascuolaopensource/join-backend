package models.daos.slick.tables

import java.sql.Timestamp

import org.joda.time.DateTime
import slick.lifted.ProvenShape

import scala.language.implicitConversions
import scalaoauth2.provider.AccessToken


trait SlickAccessTokenTable extends SlickTable {

  import profile.api._

  // API Access Token table
  case class DBAccessToken(id: Long, userId: Long, token: String, refreshToken: Option[String], expiresIn: Timestamp,
    clientId: String, scope: String, createdAt: Timestamp)

  protected class AccessTokenTable(tag: Tag) extends Table[DBAccessToken](tag, "api_access_token") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId: Rep[Long] = column[Long]("user_id")
    def token: Rep[String] = column[String]("token")
    def refreshToken: Rep[Option[String]] = column[Option[String]]("refresh_token")
    def expiresIn: Rep[Timestamp] = column[Timestamp]("expires_in")
    def clientId: Rep[String] = column[String]("client_id")
    def scope: Rep[String] = column[String]("scope")
    def createdAt: Rep[Timestamp] = column[Timestamp]("created_at")
    override def * : ProvenShape[DBAccessToken] = (id, userId, token, refreshToken, expiresIn, clientId, scope, createdAt) <> (DBAccessToken.tupled, DBAccessToken.unapply)
  }

  val AccessTokenQuery: TableQuery[AccessTokenTable] = TableQuery[AccessTokenTable]



  implicit def dbAccessTokenToAccessToken(dbAccessToken: DBAccessToken): AccessToken = AccessToken(
    token = dbAccessToken.token,
    refreshToken = dbAccessToken.refreshToken,
    scope = Some(dbAccessToken.scope),
    lifeSeconds = Some((dbAccessToken.expiresIn.getTime - dbAccessToken.createdAt.getTime) / 1000),
    createdAt = new DateTime(dbAccessToken.createdAt).toDate,
    params = Map()
  )

}
