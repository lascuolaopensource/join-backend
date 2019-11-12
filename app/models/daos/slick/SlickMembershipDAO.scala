package models.daos.slick

import java.sql.Timestamp
import java.util.Calendar

import javax.inject.Inject
import models._
import models.daos.MembershipDAO
import models.daos.slick.tables.SlickMembershipTables._
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}


class SlickMembershipDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit val ec: ExecutionContext)
  extends MembershipDAO with SlickDAO {

  import profile.api._

  private def getEndsAt(startsAt: Timestamp): Timestamp = {
    val cal = Calendar.getInstance
    cal.setTime(startsAt.getTime)
    cal.add(Calendar.YEAR, 1)
    new Timestamp(cal.getTimeInMillis)
  }


  override def all(userId: Long, language: Language): Future[Seq[Membership]] =
    db run MembershipsQuery.findForUser(userId, language).result map (_.map(dbMembershipToMembership))


  override def allRequests(language: Language, limit: Int): Future[Seq[MembershipRequest]] = {
    val q = MembershipsQuery.filter(_.isRequest).sortBy(_.requestedAt.asc).take(limit)
        .join(MembershipTypeQuery.joinedT(language))
        .on(_.membershipTypeId === _._1.id)
        .map { case (m, (mt, mtT)) => (m, mt, mtT) }
        .join(UsersQuery)
        .on(_._1.userId === _.id)

    db.run(q.result).map(_.map {
      case ((m, mt, mtT), user) =>
        MembershipRequest(
          id = m.id,
          membershipType = MembershipTypeSlim(mt.id, mtT.map(_.name)),
          user = user.toShort,
          requestedAt = m.requestedAt)
    })
  }


  override def find(id: Long, language: Language): Future[Option[Membership]] =
    db run MembershipsQuery.findById(id, language).result.headOption map (_.map(dbMembershipToMembership))


  private def findActiveQ(userId: Long, language: Language) =
    MembershipsQuery.findForUser(userId, language)
      .sortBy(_._1.acceptedAt.desc)
      .filter(_._1.isActive)

  override def findActive(userId: Long, language: Language): Future[Option[Membership]] =
    db run findActiveQ(userId, language).result.headOption map (_.map(dbMembershipToMembership))


  private def findRenewalQ(userId: Long, language: Language) = {
    MembershipsQuery.findForUser(userId, language)
      .sortBy(_._1.acceptedAt.desc)
      .filter(_._1.isRenewal)
  }

  override def findRenewal(userId: Long, language: Language): Future[Option[Membership]] =
    db run findRenewalQ(userId, language).result.headOption map (_.map(dbMembershipToMembership))


  private def findRequestQ(userId: Long, language: Language) =
    MembershipsQuery.findForUser(userId, language)
      .sortBy(_._1.requestedAt.desc)
      .filter(_._1.isRequest)

  override def findRequest(userId: Long, language: Language): Future[Option[Membership]] =
    db run findRequestQ(userId, language).result.headOption map (_.map(dbMembershipToMembership))


  private def checkRequestAndType(userId: Long, membershipTypeId: Long) =
    for {
      requestExists <- MembershipsQuery.filter(m => m.userId === userId && m.acceptedAt.isEmpty).exists.result
      finalCheck <- {
        if (!requestExists) MembershipTypeQuery.filter(_.id === membershipTypeId).exists.result
        else DBIO.successful(false)
      }
    } yield finalCheck


  override def requestNew(userId: Long, membershipTypeId: Long, language: Language): Future[Option[Membership]] = db run (for {
    check <- checkRequestAndType(userId, membershipTypeId)
    newRequest <- {
      if (check) {
        val req = DBMembership(0, membershipTypeId, userId, currentTimestamp(), None, None, None)
        for {
          id <- (MembershipsQuery returning MembershipsQuery.map(_.id)) += req
          (m, mT) <- MembershipTypeQuery.find(membershipTypeId, language).result.head
        } yield Some(dbMembershipToMembership(req.copy(id = id), m, mT))
      } else DBIO.successful(None)
    }
  } yield newRequest).transactionally


  override def requestRenewal(userId: Long, language: Language): Future[Option[Membership]] = db run (for {
    active <- findActiveQ(userId, language).result.headOption
    active <- active match {
      case s@Some(_) =>
        MembershipsQuery.findForUser(userId).filter(_.isRequest).exists.result
          .map(if (_) None else s)
      case None =>
        DBIO.successful(None)
    }
    newRequest <- active match {
      case Some((m, mt, mtT)) =>
        val endsAt = Some(getEndsAt(m.endsAt.get))
        val request = DBMembership(0, m.membershipTypeId, userId, currentTimestamp(), None, m.endsAt, endsAt)
        for {
          id <- (MembershipsQuery returning MembershipsQuery.map(_.id)) += request
        } yield Some(dbMembershipToMembership(request.copy(id = id), mt, mtT))
      case None =>
        DBIO.successful(None)
    }
  } yield newRequest).transactionally


  override def acceptRequest(userId: Long, language: Language): Future[Option[Membership]] =
    db run findRequestQ(userId, language).result.headOption.flatMap {
      case Some((request, membershipType, membershipTypeT)) =>
        val now = currentTimestamp()
        val dbRequest = MembershipsQuery.findById(request.id)

        def buildMembership(req: DBMembership) =
          dbMembershipToMembership(req, membershipType, membershipTypeT)

        def failure(n: Int) = DBIO.failed(
          new Exception(s"Incorrect number of affected rows. Got $n expected 1 for membership ${request.id}"))

        if (request.startsAt.isDefined && request.endsAt.isDefined)
          dbRequest.map(_.acceptedAt).update(Some(now)).flatMap {
            case 1 =>
              DBIO.successful(Some(buildMembership(request.copy(acceptedAt = Some(now)))))
            case n => failure(n)
          }
        else {
          val endsAt = getEndsAt(now)
          dbRequest.map(m => (m.acceptedAt, m.startsAt, m.endsAt)).update((Some(now), Some(now), Some(endsAt)))
            .flatMap {
              case 1 =>
                val updatedRequest = request.copy(acceptedAt = Some(now), startsAt = Some(now), endsAt = Some(endsAt))
                DBIO.successful(Some(buildMembership(updatedRequest)))
              case n => failure(n)
            }
        }
      case None =>
        DBIO.successful(None)
    }.transactionally


  private def deletedRowsToOpt(n: Int) = if (n == 0) None else Some(n)

  override def deleteRequest(userId: Long): Future[Option[Int]] =
    db.run(MembershipsQuery.findForUser(userId).filter(_.isRequest).delete).map(deletedRowsToOpt)

  override def deleteRenewal(userId: Long): Future[Option[Int]] =
    db.run(MembershipsQuery.findForUser(userId).filter(_.isRenewal).delete).map(deletedRowsToOpt)

  override def deleteActive(userId: Long): Future[Option[Int]] = db run (for {
    deletedActive <- MembershipsQuery.findForUser(userId).filter(_.isActive).delete.map(deletedRowsToOpt)
    _ <- MembershipsQuery.findForUser(userId).filter(_.isRequest).delete
    _ <- MembershipsQuery.findForUser(userId).filter(_.isRenewal).delete
  } yield deletedActive).transactionally


  override def countActiveByType(time: Timestamp, language: Language): Future[Seq[MembershipStatCount]] = {
    val q = for {
      (mt, mtT) <- MembershipTypeQuery.joinedT(language)
      c = MembershipsQuery.filter(m => m.membershipTypeId === mt.id && m.isActiveAt(time)).length
    } yield (mt.id, mtT.map(_.name), c)

    db.run(q.result).map(_.map {
      case (id, optName, count) =>
        MembershipStatCount(
          membershipType = MembershipTypeSlim(id, optName),
          count = count)
    })
  }

}
