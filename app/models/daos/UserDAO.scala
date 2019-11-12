package models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import models.{SlimSkill, User, UserRole}

import scala.concurrent.Future


trait UserDAO {

  def all: Future[Seq[User]]
  def find(id: Long, userId: Option[Long] = None): Future[Option[User]]
  def find(loginInfo: LoginInfo): Future[Option[User]]
  def save(user: User, optSkills: Option[Seq[SlimSkill]] = None): Future[User]
  def delete(id: Long): Future[Boolean]
  def search(userId: Long, value: Option[String], skillIds: Seq[Long], matchAllSkills: Boolean): Future[Seq[User]]
  def favorite(userId: Long, otherId: Long, favorite: Boolean): Future[Boolean]
  def favorites(userId: Long): Future[Seq[User]]
  def updateRole(userId: Long, userRole: UserRole): Future[Boolean]

}
