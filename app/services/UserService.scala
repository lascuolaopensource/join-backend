package services

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import javax.inject.Inject
import models.{SlimSkill, User, UserRole}
import models.daos.UserDAO

import scala.concurrent.{ExecutionContext, Future}


trait UserService extends IdentityService[User] with Service {

  def all: Future[Seq[User]]
  def save(user: User, optSkills: Option[Seq[SlimSkill]] = None): Future[User]
  def find(id: Long, userId: Option[Long] = None): Future[Option[User]]
  def delete(id: Long): Future[Boolean]
  def search(userId: Long, value: Option[String], skillIds: Seq[Long], matchAllSkills: Boolean): Future[Seq[User]]
  def favorite(userId: Long, otherId: Long, favorite: Boolean): Future[ServiceReply[Unit, NotFound]]
  def favorites(userId: Long): Future[Seq[User]]
  def updateRole(userId: Long, userRole: UserRole): Future[ServiceReply[Unit, NotFound]]

}


class UserServiceImpl @Inject() (userDAO: UserDAO)(implicit ec: ExecutionContext) extends UserService {

  import Service.Ops._

  override def all: Future[Seq[User]] = userDAO.all

  override def save(user: User, optSkills: Option[Seq[SlimSkill]]): Future[User] = userDAO.save(user, optSkills)

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userDAO.find(loginInfo)

  override def find(id: Long, userId: Option[Long] = None): Future[Option[User]] = userDAO.find(id, userId)

  override def delete(id: Long) = userDAO.delete(id)

  override def search(userId: Long, value: Option[String], skillIds: Seq[Long], matchAllSkills: Boolean): Future[Seq[User]] =
    userDAO.search(userId, value, skillIds, matchAllSkills)

  override def favorite(userId: Long, otherId: Long, favorite: Boolean) =
    userDAO.favorite(userId, otherId, favorite).map(_.toNotFound)

  override def favorites(userId: Long) = userDAO.favorites(userId)

  override def updateRole(userId: Long, userRole: UserRole) =
    userDAO.updateRole(userId, userRole).map(_.toNotFound)

}

