package models.daos.slick

import javax.inject.Inject
import models.{Language, MembershipType}
import models.daos.MembershipTypeDAO
import models.daos.slick.tables.SlickMembershipTables._
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext


class SlickMembershipTypeDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit val ec: ExecutionContext)
  extends MembershipTypeDAO with SlickDAO {

  import profile.api._

  override def all(language: Language) = db run
    MembershipTypeQuery.join(MembershipTypeTQuery.byLanguage(language))
      .on(_.id === _.membershipTypeId).sortBy(_._1.position.asc).result
      .map(_.map(dbMembershipTypeToMembershipType))

  override def create(membershipType: MembershipType) = db run (for {
    mId <- (MembershipTypeQuery returning MembershipTypeQuery.map(_.id)) += membershipTypeToDB(membershipType, Some(0))
    _ <- MembershipTypeTQuery += membershipTypeTToDB(membershipType, Some(mId))
  } yield membershipType.copy(id = mId)).transactionally

  override def update(membershipType: MembershipType) = db run (for {
    n <- MembershipTypeQuery.filter(_.id === membershipType.id).update(membershipTypeToDB(membershipType))
    n <- {
      if (n == 1)
        MembershipTypeTQuery.insertOrUpdate(membershipTypeTToDB(membershipType))
      else DBIO.successful(0)
    }
  } yield if (n == 1) Some(membershipType) else None).transactionally

  override def delete(id: Long) = db run (for {
    _ <- MembershipsQuery.filter(_.membershipTypeId === id).delete
    _ <- MembershipTypeTQuery.filter(_.membershipTypeId === id).delete
    n <- MembershipTypeQuery.filter(_.id === id).delete
  } yield n > 0).transactionally

}
