import javax.inject.{Inject, Provider, Singleton}
import org.webjars.play.WebJarsUtil
import play.api.http.DefaultHttpErrorHandler
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, NOT_FOUND, INTERNAL_SERVER_ERROR}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.{NotFound, Status}
import play.api.mvc.{RequestHeader, Result}
import play.api.routing.Router
import play.api._

import scala.concurrent.Future


@Singleton
class ErrorHandler @Inject() (
  env: Environment,
  configuration: Configuration,
  sourceMapper: OptionalSourceMapper,
  routerProvider: Provider[Router],
  val messagesApi: MessagesApi
)(implicit webJarsUtil: WebJarsUtil
) extends DefaultHttpErrorHandler(env, configuration, sourceMapper, routerProvider)
  with I18nSupport {

  protected val showDevErrors = env.mode != Mode.Prod

  protected def genericErrorView(statusCode: Int)(implicit requestHeader: RequestHeader) =
    views.html.errors.generic(statusCode)

  protected def genericErrorResult(statusCode: Int)(implicit requestHeader: RequestHeader) =
    Status(statusCode)(genericErrorView(statusCode))

  override protected def onBadRequest(request: RequestHeader, message: String): Future[Result] =
    Future.successful(genericErrorResult(BAD_REQUEST)(request))

  override protected def onForbidden(request: RequestHeader, message: String): Future[Result] =
    Future.successful(genericErrorResult(FORBIDDEN)(request))

  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] =
    Future.successful(NotFound(
      if (showDevErrors) views.html.defaultpages.devNotFound(request.method, request.uri, Some(routerProvider.get()))
      else genericErrorView(NOT_FOUND)(request)))

  override protected def onOtherClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    Future.successful(genericErrorResult(statusCode)(request))

  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] =
    Future.successful(genericErrorResult(INTERNAL_SERVER_ERROR)(request))

}
