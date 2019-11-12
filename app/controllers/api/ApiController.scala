package controllers.api

import java.sql.{Date, Timestamp}

import auth.{DefaultEnv, OAuthDataHandler}
import controllers.api.ApiController.{ApiControllerComponents, RequestType}
import javax.inject.{Inject, Singleton}
import models.Language
import play.api.Logger
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._
import scalaoauth2.provider._
import services.Service.ServiceRep
import services.{Error, ServiceError, ServiceReply}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{implicitConversions, reflectiveCalls}
import scala.util.Try


object ApiController {

  @Singleton
  case class ApiControllerComponents @Inject() (
    dataHandler: OAuthDataHandler,
    executionContext: ExecutionContext,
    actionBuilder: DefaultActionBuilder)

  type RequestType = AuthInfoRequest[AnyContent, DefaultEnv#I]

}


abstract class ApiController extends ControllerHelpers {

  protected val controllerComponents: ApiControllerComponents

  protected val Action: DefaultActionBuilder = controllerComponents.actionBuilder

  implicit val dataHandler: OAuthDataHandler = controllerComponents.dataHandler
  implicit val ec: ExecutionContext = controllerComponents.executionContext

  protected type AuthorizedActionWithScopeType = (RequestType => Future[Result]) => Action[AnyContent]
  protected val AdminScope: AuthorizedActionWithScopeType =
    f => AuthorizedActionWithScope("admin").async(f)
  protected val UserScope: AuthorizedActionWithScopeType =
    f => AuthorizedActionWithScope("user").async(f)
  protected val AllScope: AuthorizedActionWithScopeType =
    f => AuthorizedActionWithScope("user", "admin").async(f)


  protected def jsonOrBadRequest(action: JsValue => Future[Result])(implicit request: RequestType): Future[Result] = {
    request.body.asJson match {
      case Some(json) => action(json)
      case None => Future.successful(BadRequest(""))
    }
  }


  protected def AuthorizedActionWithScope[U](scopes: String*)
    (implicit protectedResourceHandler: ProtectedResourceHandler[U]):
    ActionBuilder[({ type L[A] = AuthInfoRequest[A, U] })#L, AnyContent] =
  {
    AuthorizedActionFunctionWithScope(protectedResourceHandler, scopes: _*) compose Action
  }


  private case class AuthorizedActionFunctionWithScope[U](handler: ProtectedResourceHandler[U], scopes: String*)(implicit ctx: ExecutionContext)
    extends ActionFunction[Request, ({ type L[A] = AuthInfoRequest[A, U] })#L] with OAuth2Provider {

    override protected def executionContext: ExecutionContext = ctx

    override def invokeBlock[A](request: Request[A], block: AuthInfoRequest[A, U] => Future[Result]): Future[Result] = {
      authorize(handler) {
        case authInfo@AuthInfo(_, _, Some(authScope), _) if scopes.contains(authScope) =>
          block(AuthInfoRequest(authInfo, request))
        case _ => Future.successful(Unauthorized)
      }(request, ctx)
    }

  }


  protected def defaultJsonWithErrorsAction[A](formWithErrors: Form[A]): Future[Result] = {
    Logger.debug(formWithErrors.errors.map(_.message).mkString("\n"))
    Future.successful(BadRequest(""))
  }

  protected def defaultErrorHandler(invalid: Seq[(JsPath, Seq[JsonValidationError])]): Future[Result] = {
    val errors = invalid.map {
      case (jsPath, validationErrors) =>
        Json.obj(
          "path" -> jsPath.toJsonString,
          "messages" -> validationErrors.map(_.message)
        )
    }
    Future.successful(BadRequest(Json.toJson(Map("errors" -> errors))))
  }


  protected def validateOrBadRequest[T : Reads]()(f: T => Future[Result])(implicit request: RequestType) =
    jsonOrBadRequest { json =>
      json.validate[T].fold(defaultErrorHandler, f)
    }

  protected def validateOrBadRequest[T](form: Form[T])(f: T => Future[Result])(implicit request: RequestType) =
    jsonOrBadRequest { json =>
      form.bind(json).fold(defaultJsonWithErrorsAction, f)
    }


  protected def deleteResult(result: Boolean) = if (result) NoContent else NotFound

  protected implicit def serviceErrorToResult(serviceError: ServiceError): Result = serviceError match {
    case services.NotAuthorized() => Unauthorized
    case services.NotFound() => NotFound
    case services.BadRequest(message) => BadRequest(Json.obj("message" -> message))
    case services.Exists(_) => BadRequest
    case services.GenericError(message) => InternalServerError(Json.obj("message" -> message))
  }

  protected def resultJsonFromServiceRep[T: Writes](reply: ServiceRep[T]): Result =
    resultJsonFromServiceReply[T, ServiceError](reply, None)

  protected def resultJsonFromServiceRep[T: Writes](reply: ServiceRep[T], prop: Option[String]): Result =
    resultJsonFromServiceReply[T, ServiceError](reply, prop)

  protected def resultJsonFromServiceReply[T: Writes, E <: ServiceError](reply: ServiceReply[T, E]): Result =
    resultJsonFromServiceReply(reply, None)

  protected def resultJsonFromServiceReply[T: Writes, E <: ServiceError](reply: ServiceReply[T, E], prop: Option[String]): Result =
    reply match {
      case Error(e) => e.asInstanceOf[ServiceError]
      case services.Ok(t) if t.isInstanceOf[Unit] => NoContent
      case services.Ok(t) => prop match {
        case Some(p) => Ok(Json.obj(p -> t))
        case None => Ok(Json.toJson(t))
      }
    }

  // this is actually never used, but it placates the compiler when using it in `resultJsonFromServiceReply`
  protected implicit def unitWrites: Writes[Unit] = Writes[Unit] { _ => JsNull }

  protected def resultFromServiceReply(result: Result)(reply: ServiceRep[_]): Result = reply match {
    case Error(e) => e
    case services.Ok(_) => result
  }

  protected def toOk[T: Writes](t: T): Result = Ok(Json.toJson(t))
  protected def toOk[T: Writes](key: String)(t: T): Result = Ok(Json.obj(key -> t))

  def getLanguage(implicit request: AuthInfoRequest[_, auth.DefaultEnv#I]) =
    request.getQueryString("lang").map(Language.stringLanguage)
      .getOrElse(request.authInfo.user.preferredLang)

}


trait StatsController { self: ApiController =>

  import StatsController._

  protected def okStats[S: Writes](s: S) = Ok(Json.obj("stats" -> s))

  protected lazy val nokStats = Future.successful(BadRequest)

  protected def processStatsRequest[T: Writes](
    key: String, f: Timestamp => Future[T]
  )(implicit r: RequestType) =
    getTimestampFromReq(key)
      .map(from => f(from).map(okStats(_)))
      .getOrElse(nokStats)

  protected def processStatsRequest[T: Writes](
    key1: String, key2: String, f: (Timestamp, Timestamp) => Future[T]
  )(implicit r: RequestType) =
    getTimestampFromReq(key1, key2)
      .map { case (from, to) => f(from, to).map(okStats(_)) }
      .getOrElse(nokStats)

}

object StatsController {

  def getTimestampFromReq(key: String)(implicit request: RequestType): Option[Timestamp] =
    request.getQueryString(key).flatMap(s => Try(s.toLong).toOption.map(new Timestamp(_)))

  def getTimestampFromReq(key1: String, key2: String)(implicit request: RequestType): Option[(Timestamp, Timestamp)] =
    getTimestampFromReq(key1).zip(getTimestampFromReq(key2)).headOption

  def getDateFromReq(key: String)(implicit request: RequestType): Option[Date] =
    request.getQueryString(key).flatMap(s => Try(s.toLong).toOption.map(new Date(_)))

  def getDateFromReq(key1: String, key2: String)(implicit request: RequestType): Option[(Date, Date)] =
    getDateFromReq(key1).zip(getDateFromReq(key2)).headOption

}
