package tasks

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import models.User
import models.daos.UserDAO
import play.api.Application

import scala.concurrent.{ExecutionContext, Future}



object SeedData extends AppTask {

  override protected def task(app: Application)(implicit ec: ExecutionContext): Future[Unit] = {
    val userDAO = app.injector.instanceOf[UserDAO]
    val passwordHasher = app.injector.instanceOf[PasswordHasher]
    val authInfoRepository = app.injector.instanceOf[AuthInfoRepository]

    val emailPath = "seeding.user.email"
    val email = app.configuration.getOptional[String](emailPath) match {
      case Some(m) => m
      case None =>
        logger.error(s"No email was found in configuration file under '$emailPath'")
        return Future.successful(())
    }

    val pwdPath = "seeding.user.password"
    val pwd = app.configuration.getOptional[String](pwdPath) match {
      case Some(p) => p
      case None =>
        logger.error(s"No password was found in configuration file under '$pwdPath'")
        return Future.successful(())
    }

    val user = User(
      id = 0,
      firstName = "Dummy",
      lastName = "User",
      email = email,
      emailConfirmed = true,
      loginInfo = Seq(),
      title = Some("Dummy user @ La Scuola Open Source"),
      dummy = true)

    val loginInfo = LoginInfo(CredentialsProvider.ID, email)
    val authInfo = passwordHasher.hash(pwd)

    val future = for {
      optUser <- userDAO.find(loginInfo)
      res <- optUser match {
        case Some(u) =>
          userDAO.save(user.copy(id = u.id)).map((false, _))
        case None => for {
          user <- userDAO.save(user)
          _ <- authInfoRepository.add(loginInfo, authInfo)
        } yield (true, user)
      }
    } yield res

    future.map { case (created, updatedUser) =>
      logger.info(s"${if (created) "Created" else "Updated"} $updatedUser")
    }
  }

}
