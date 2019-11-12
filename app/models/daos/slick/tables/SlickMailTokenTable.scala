package models.daos.slick.tables

import java.sql.Timestamp

import models._


trait SlickMailTokenTable extends SlickTable {

  import profile.api._

  protected implicit val tokenTypeMapper = MappedColumnType.base[MailTokenType, Int](
    {
      case SignUpToken => 0
      case ChangeEmailToken => 1
      case ResetPasswordToken => 2
    }, {
      case 0 => SignUpToken
      case 1 => ChangeEmailToken
      case 2 => ResetPasswordToken
    }
  )

  protected class MailTokenTable(tag: Tag) extends Table[MailToken](tag, "user_email_token") {
    def token = column[String]("token", O.PrimaryKey)
    def email = column[String]("email")
    def userId = column[Long]("user_id")
    def expiration = column[Timestamp]("expiration")
    def tokenType = column[MailTokenType]("token_type")
    override def * = (token, email, userId, expiration, tokenType) <> ((MailToken.apply _).tupled, MailToken.unapply)
  }

  protected val MailTokenQuery = TableQuery[MailTokenTable]

}
