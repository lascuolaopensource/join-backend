package models.daos.slick.tables

import java.sql.Timestamp

import models.daos.slick.tables.SlickUserTable._
import models._
import slick.lifted.ProvenShape

import scala.language.implicitConversions


private[slick] object SlickUserTable {

  case class DBUser(
    id: Long,
    firstName: String,
    lastName: String,
    email: String,
    emailConfirmed: Boolean,
    createdAt: Timestamp,
    role: UserRole,
    preferredLang: String,
    agreement: Boolean,
    telephone: Option[String],
    bio: Option[String],
    title: Option[String],
    city: Option[String],
    cityOther: Option[Boolean],
    dummy: Boolean)
  {

    lazy val updatableFields =
      (firstName, lastName, email, emailConfirmed, preferredLang, agreement, telephone, bio, title, city, cityOther)

    def toShort = UserShort(id, email, firstName, lastName)

    def toContacts = UserContacts(id, email, telephone, firstName, lastName)

  }

}


trait SlickUserTable extends SlickTable {

  import profile.api._

  implicit val userRoleMappedValue = MappedColumnType.base[UserRole, Int](
    {
      case NormalRole => 0
      case AdminRole => 1
    }, {
      case 0 => NormalRole
      case 1 => AdminRole
      case r => throw new MatchError(s"Unexpected user role $r")
    }
  )

  protected class UsersTable(tag: Tag) extends Table[DBUser](tag, "user") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def firstName: Rep[String] = column[String]("first_name")
    def lastName: Rep[String] = column[String]("last_name")
    def email: Rep[String] = column[String]("email")
    def emailConfirmed: Rep[Boolean] = column("email_confirmed")
    def createdAt: Rep[Timestamp] = column("created_at")
    def role: Rep[UserRole] = column("role")
    def preferredLang: Rep[String] = column("preferred_lang")
    def agreement: Rep[Boolean] = column("agreement")
    def telephone: Rep[Option[String]] = column("telephone")
    def bio: Rep[Option[String]] = column("bio")
    def title: Rep[Option[String]] = column("title")
    def city: Rep[Option[String]] = column("city")
    def cityOther: Rep[Option[Boolean]] = column("city_other")
    def dummy: Rep[Boolean] = column("dummy")
    override def * : ProvenShape[DBUser] =
      (id, firstName, lastName, email, emailConfirmed, createdAt, role, preferredLang, agreement,
        telephone, bio, title, city, cityOther, dummy) <> (DBUser.tupled, DBUser.unapply)

    def fullName: Rep[String] = firstName ++ " " ++ lastName
    def updatableFields =
      (firstName, lastName, email, emailConfirmed, preferredLang, agreement, telephone, bio, title, city, cityOther)
  }

  protected val UsersQuery: TableQuery[UsersTable] = TableQuery[UsersTable]



  implicit def dbUserToUser(dbUser: DBUser): User = User(
    id = dbUser.id,
    firstName = dbUser.firstName,
    lastName = dbUser.lastName,
    email = dbUser.email,
    emailConfirmed = dbUser.emailConfirmed,
    loginInfo = Seq(),
    createdAt = dbUser.createdAt,
    userRole = dbUser.role,
    preferredLang = dbUser.preferredLang,
    agreement = dbUser.agreement,
    telephone = dbUser.telephone,
    bio = dbUser.bio,
    title = dbUser.title,
    city = (dbUser.city, dbUser.cityOther) match {
      case (Some(city), Some(false)) => Some(GivenCity(city))
      case (None, Some(true))        => Some(OtherCity)
      case _                         => None
    },
    dummy = dbUser.dummy
  )

  implicit def userToDBUser(user: User): DBUser = DBUser(
    id = user.id,
    firstName = user.firstName,
    lastName = user.lastName,
    email = user.email,
    emailConfirmed = user.emailConfirmed,
    createdAt = user.createdAt,
    role = user.userRole,
    preferredLang = user.preferredLang.language,
    agreement = user.agreement,
    telephone = user.telephone,
    bio = user.bio,
    title = user.title,
    city = user.city.flatMap(_.cityOpt),
    cityOther = user.city.flatMap(_.cityOtherOpt),
    dummy = user.dummy
  )

}
