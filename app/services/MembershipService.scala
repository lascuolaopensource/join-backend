package services

import java.sql.Timestamp

import javax.inject.Inject
import models.daos.{MembershipDAO, MembershipTypeDAO}
import models._

import scala.concurrent.{ExecutionContext, Future}


trait MembershipService extends StatService {

  protected implicit val ec: ExecutionContext

  def allRequests(language: Language, limit: Int = defaultLimit): Future[Seq[MembershipRequest]]

  def requestNew(userId: Long, membershipTypeId: Long, language: Language): Future[ServiceReply[Membership, BadRequest]]
  def requestRenewal(userId: Long, language: Language): Future[ServiceReply[Membership, BadRequest]]
  def acceptRequest(userId: Long, language: Language): Future[ServiceReply[Membership, NotFound]]
  def deleteRequest(userId: Long): Future[ServiceReply[Unit, NotFound]]
  def deleteRenewal(userId: Long): Future[ServiceReply[Unit, NotFound]]
  def deleteActive(userId: Long): Future[ServiceReply[Unit, NotFound]]
  def enrichWithMemberships(user: User, language: Language): Future[User]
  def enrichWithMemberships(optUser: Option[User], language: Language): Future[Option[User]] =
    optionToFutureOption[User](optUser, enrichWithMemberships(_, language))

  def allTypes(language: Language): Future[Seq[MembershipType]]
  def createType(membershipType: MembershipType): Future[MembershipType]
  def updateType(membershipType: MembershipType): Future[ServiceReply[MembershipType, NotFound]]
  def deleteType(typeId: Long): Future[ServiceReply[Unit, NotFound]]

  def countActiveByType(time: Timestamp, language: Language): Future[Seq[MembershipStatCount]]

}


class MembershipServiceImpl @Inject()(
  membershipDAO: MembershipDAO,
  membershipTypeDAO: MembershipTypeDAO
)(implicit val ec: ExecutionContext)
  extends MembershipService {

  import services.Service.Ops._
  import Function.const


  override def allRequests(language: Language, limit: Int) =
    membershipDAO.allRequests(language, limit)

  override def requestNew(userId: Long, membershipTypeId: Long, language: Language) =
    membershipDAO.requestNew(userId, membershipTypeId, language) map (_.toBadRequest("A request already exists"))

  override def requestRenewal(userId: Long, language: Language) =
    membershipDAO.requestRenewal(userId, language)
      .map (_.toBadRequest("A request already exists or no active membership found"))

  override def acceptRequest(userId: Long, language: Language) =
    membershipDAO.acceptRequest(userId, language) map (_.toNotFound)

  override def deleteRequest(userId: Long) =
    membershipDAO.deleteRequest(userId) map (_.toNotFound.map(const(Unit)))

  override def deleteActive(userId: Long) =
    membershipDAO.deleteActive(userId) map (_.toNotFound.map(const(Unit)))

  override def deleteRenewal(userId: Long) =
    membershipDAO.deleteRenewal(userId) map (_.toNotFound.map(const(Unit)))

  override def enrichWithMemberships(user: User, language: Language): Future[User] =
    Future sequence {
      membershipDAO.findActive(user.id, language) ::
      membershipDAO.findRenewal(user.id, language) ::
      membershipDAO.findRequest(user.id, language) :: Nil
    } map { case List(active, renewal, request) =>
      user.copy(memberships = UserMemberships(active, renewal, request))
    }

  override def allTypes(language: Language) =
    membershipTypeDAO.all(language)

  override def createType(membershipType: MembershipType) =
    membershipTypeDAO.create(membershipType)

  override def updateType(membershipType: MembershipType) =
    membershipTypeDAO.update(membershipType) map (_.toNotFound)

  override def deleteType(typeId: Long) =
    membershipTypeDAO.delete(typeId) map (_.toNotFound)

  override def countActiveByType(time: Timestamp, language: Language) =
    membershipDAO.countActiveByType(time, language)

}
