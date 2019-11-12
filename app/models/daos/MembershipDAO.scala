package models.daos

import java.sql.Timestamp

import models.{Language, Membership, MembershipRequest, MembershipStatCount}

import scala.concurrent.Future


trait MembershipDAO {

  def all(userId: Long, language: Language): Future[Seq[Membership]]
  def allRequests(language: Language, limit: Int): Future[Seq[MembershipRequest]]
  def find(id: Long, language: Language): Future[Option[Membership]]
  def findActive(userId: Long, language: Language): Future[Option[Membership]]
  def findRenewal(userId: Long, language: Language): Future[Option[Membership]]
  def findRequest(userId: Long, language: Language): Future[Option[Membership]]
  def requestNew(userId: Long, membershipTypeId: Long, language: Language): Future[Option[Membership]]
  def requestRenewal(userId: Long, language: Language): Future[Option[Membership]]
  def acceptRequest(userId: Long, language: Language): Future[Option[Membership]]
  def deleteRequest(userId: Long): Future[Option[Int]]
  def deleteRenewal(userId: Long): Future[Option[Int]]
  def deleteActive(userId: Long): Future[Option[Int]]

  def countActiveByType(time: Timestamp, language: Language): Future[Seq[MembershipStatCount]]

}
