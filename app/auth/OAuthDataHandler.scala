package auth

import java.security.SecureRandom

import javax.inject.Inject
import models.User
import models.daos.AccessTokenDAO
import org.joda.time.DateTime
import play.api.Configuration

import scala.concurrent.Future
import scalaoauth2.provider._

import scala.concurrent.duration.Duration


sealed trait OAuthDataHandler extends DataHandler[User]


class OAuthDataHandlerImpl @Inject() (
  config: Configuration,
  secureRandom: SecureRandom,
  accessTokenDAO: AccessTokenDAO,
  user: Option[User]) extends OAuthDataHandler {

  val accessTokenExpire = Some(config.getOptional[Duration]("oauth2.tokenExpire")
    .map(_.toMillis).getOrElse(60 * 60L * 1000) / 1000)

  override def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[User]]] =
    accessTokenDAO.find(accessToken)

  override def findAccessToken(token: String): Future[Option[AccessToken]] = accessTokenDAO.find(token)

  override def validateClient(maybeCredential: Option[ClientCredential], request: AuthorizationRequest): Future[Boolean] =
    Future.successful(maybeCredential.isDefined)

  override def findUser(maybeCredential: Option[ClientCredential], request: AuthorizationRequest): Future[Option[User]] =
    Future.successful(user)

  override def createAccessToken(authInfo: AuthInfo[User]): Future[AccessToken] = {
    val refreshToken = Some(generateToken())
    val accessToken = generateToken()
    val now = DateTime.now().toDate

    val tokenObject = AccessToken(accessToken, refreshToken, authInfo.scope, accessTokenExpire, now)
    accessTokenDAO.save(authInfo, tokenObject)

    Future.successful(tokenObject)
  }

  override def getStoredAccessToken(authInfo: AuthInfo[User]): Future[Option[AccessToken]] =
    accessTokenDAO.find(authInfo)

  override def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): Future[AccessToken] = {
    accessTokenDAO.delete(authInfo)
    createAccessToken(authInfo)
  }

  override def findAuthInfoByCode(code: String): Future[Option[AuthInfo[User]]] =
    Future.failed(new NotImplementedError())

  override def deleteAuthCode(code: String): Future[Unit] = Future.failed(new NotImplementedError())

  override def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[User]]] =
    accessTokenDAO.findByRefreshToken(refreshToken)


  private def generateToken(length: Int = 256): String = BigInt(length, secureRandom).toString(32)

}
