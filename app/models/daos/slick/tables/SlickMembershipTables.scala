package models.daos.slick.tables

import java.sql.Timestamp

import models._

import scala.language.implicitConversions


private[slick] object SlickMembershipTables {

  case class DBMembershipType(
    id: Long,
    price: Double,
    position: Int,
    createdAt: Timestamp)

  case class DBMembershipTypeT(
    membershipTypeId: Long,
    language: String,
    name: String,
    offer: String,
    bottom: String)

  case class DBMembership(
    id: Long,
    membershipTypeId: Long,
    userId: Long,
    requestedAt: Timestamp,
    acceptedAt: Option[Timestamp],
    startsAt: Option[Timestamp],
    endsAt: Option[Timestamp])

  val dbMembershipToMembership = {
    val f =
      (dbMembership: DBMembership, dBMembershipType: DBMembershipType, dBMembershipTypeT: Option[DBMembershipTypeT]) =>
        Membership(
          id = dbMembership.id,
          membershipType = MembershipTypeSlim(dBMembershipType.id, dBMembershipTypeT.map(_.name)),
          userId = dbMembership.userId,
          requestedAt = dbMembership.requestedAt,
          acceptedAt = dbMembership.acceptedAt,
          startsAt = dbMembership.startsAt,
          endsAt = dbMembership.endsAt)
    f.tupled
  }

  implicit def membershipToDBMembership(membership: Membership): DBMembership = DBMembership(
    id = membership.id,
    membershipTypeId = membership.membershipType.id,
    userId = membership.userId,
    requestedAt = membership.requestedAt,
    acceptedAt = membership.acceptedAt,
    startsAt = membership.startsAt,
    endsAt = membership.endsAt
  )

  val dbMembershipTypeToMembershipType = {
    val f = (m: DBMembershipType, mT: DBMembershipTypeT) =>
      MembershipType(
        id = m.id,
        language = mT.language,
        name = mT.name,
        offer = mT.offer,
        bottom = mT.bottom,
        price = m.price,
        position = m.position,
        createdAt = m.createdAt)
    f.tupled
  }

  def membershipTypeToDB(membershipType: MembershipType, optId: Option[Long] = None) =
    DBMembershipType(
      id = optId.getOrElse(membershipType.id),
      price = membershipType.price,
      position = membershipType.position,
      createdAt = membershipType.createdAt)

  def membershipTypeTToDB(membershipType: MembershipType, optId: Option[Long] = None) =
    DBMembershipTypeT(
      membershipTypeId = optId.getOrElse(membershipType.id),
      language = membershipType.language.language,
      name = membershipType.name,
      offer = membershipType.offer,
      bottom = membershipType.bottom)

}


trait SlickMembershipTables extends SlickTable {

  import profile.api._
  import SlickMembershipTables._

  protected class MembershipTypeTable(tag: Tag) extends Table[DBMembershipType](tag, "membership_type") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def price: Rep[Double] = column("price")
    def position: Rep[Int] = column("position")
    def createdAt: Rep[Timestamp] = column("created_at")
    override def * =
      (id, price, position, createdAt) <> (DBMembershipType.tupled, DBMembershipType.unapply)
  }

  protected object MembershipTypeQuery extends TableQuery(new MembershipTypeTable(_)) {

    def joinedT(language: Language, optF: Option[MembershipTypeTable => Rep[Boolean]] = None) =
      optF.map(filter(_)).getOrElse(this)
        .joinLeft(MembershipTypeTQuery.byLanguage(language))
        .on(_.id === _.membershipTypeId)

    def find(id: Rep[Long], language: Language) =
      joinedT(language, Some(_.id === id))

  }

  protected class MembershipTypeTTable(tag: Tag) extends Table[DBMembershipTypeT](tag, "membership_type_t") {
    def membershipTypeId: Rep[Long] = column("membership_type_id")
    def language: Rep[String] = column("language")
    def name: Rep[String] = column("name")
    def offer: Rep[String] = column("offer")
    def bottom: Rep[String] = column("bottom")
    def pk = primaryKey("membership_type_t_pkey", (membershipTypeId, language))
    def membershipType =
      foreignKey("membership_type_id_fkey", membershipTypeId, MembershipTypeQuery)(_.id)
    override def * =
      (membershipTypeId, language, name, offer, bottom) <> (DBMembershipTypeT.tupled, DBMembershipTypeT.unapply)
  }

  protected object MembershipTypeTQuery extends TableQuery(new MembershipTypeTTable(_)) {

    def byLanguage(language: Language) = filter(_.language === language.language)

  }

  protected class MembershipTable(tag: Tag) extends Table[DBMembership](tag, "membership") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def membershipTypeId: Rep[Long] = column("membership_type_id")
    def requestedAt: Rep[Timestamp] = column("requested_at")
    def acceptedAt: Rep[Option[Timestamp]] = column("accepted_at")
    def startsAt: Rep[Option[Timestamp]] = column("starts_at")
    def endsAt: Rep[Option[Timestamp]] = column("ends_at")
    def userId: Rep[Long] = column("user_id")

    def isActive = isActiveAt(currentTimestamp())

    def isActiveAt(time: Timestamp) = {
      acceptedAt.isDefined && startsAt <= time && endsAt > time
    }

    def isRenewal =
      acceptedAt.isDefined && startsAt > currentTimestamp()

    def isRequest = acceptedAt.isEmpty

    override def * =
      (id, membershipTypeId, userId, requestedAt, acceptedAt, startsAt, endsAt) <>
        (DBMembership.tupled, DBMembership.unapply)
  }

  protected object MembershipsQuery extends TableQuery(new MembershipTable(_)) {

    private def find(language: Language, f: MembershipTable => Rep[Boolean]) =
      filter(f)
        .join(MembershipTypeQuery.joinedT(language))
        .on(_.membershipTypeId === _._1.id)
        .map { case (membership, (mt, mtT)) => (membership, mt, mtT) }

    def findById(id: Long, language: Language) =
      find(language, _.id === id)

    def findById(id: Long) = filter(_.id === id)

    def findForUser(userId: Long, language: Language) =
      find(language, _.userId === userId)

    def findForUser(userId: Long) = filter(_.userId === userId)

  }

}
