package auth

import javax.inject.Inject

import models.{AdminRole, NormalRole}
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}
import scalaoauth2.provider._


class ApiTokenEndpoint @Inject() (configuration: Configuration) extends TokenEndpoint {

  override val handlers: Map[String, GrantHandler] = Map(
    OAuthGrantType.IMPLICIT -> new Implicit {
      override def clientCredentialRequired: Boolean = true

      override def handleRequest[U](maybeValidatedClientCred: Option[ClientCredential], request: AuthorizationRequest, handler: AuthorizationHandler[U])(implicit ctx: ExecutionContext): Future[GrantHandlerResult[U]] = {
        val implicitRequest = ImplicitRequest(request)
        val clientCredential = maybeValidatedClientCred
          .getOrElse(throw new InvalidRequest("Client credentials are required"))

        handler.findUser(Some(clientCredential), implicitRequest).flatMap {
          case Some(user) =>
            val scope = user.asInstanceOf[auth.DefaultEnv#I].userRole match {
              case NormalRole => "user"
              case AdminRole => "admin"
            }

            val redirectUri = if (clientCredential.clientId == "admin" && scope == "admin")
              configuration.getOptional[String]("api.clients.admin.redirect_uri")
            else if (clientCredential.clientId == "user")
              configuration.getOptional[String]("api.clients.sos-ui.redirect_uri")
            else
              throw new InvalidRequest("Wrong client credential")

            val authInfo = AuthInfo(user, Some(clientCredential.clientId), Some(scope), redirectUri)
            issueAccessToken(handler, authInfo)
          case None => throw new InvalidGrant("user cannot be authenticated")
        }
      }
    }
  )

}
