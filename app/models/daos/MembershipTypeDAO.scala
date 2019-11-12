package models.daos

import models.{Language, MembershipType}

import scala.concurrent.Future


trait MembershipTypeDAO {

  def all(language: Language): Future[Seq[MembershipType]]
  def create(membershipType: MembershipType): Future[MembershipType]
  def update(membershipType: MembershipType): Future[Option[MembershipType]]
  def delete(id: Long): Future[Boolean]

}
