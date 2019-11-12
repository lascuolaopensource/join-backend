package services

import scala.concurrent.{ExecutionContext, Future}


trait Service {

  protected def optionToFutureOption[A](option: Option[A], f: A => Future[A])
    (implicit ec: ExecutionContext): Future[Option[A]] =
    option match {
      case Some(value) => f(value).map(Some(_))
      case None => Future.successful(None)
    }

  // FIXME: why is a disambiguation needed?
  protected def optionToFutureOption2[A](f: A => Future[A])(option: Option[A])
    (implicit ec: ExecutionContext): Future[Option[A]] = optionToFutureOption(option, f)

}


sealed trait ServiceError
final case class NotAuthorized() extends ServiceError
final case class NotFound() extends ServiceError
final case class Exists[T](t: T) extends ServiceError
final case class BadRequest(message: String) extends ServiceError
final case class GenericError(message: String) extends ServiceError

sealed trait ServiceReply[+T, +E <: ServiceError] {
  private def fmap[T2, R](f: T => T2, bt: T2 => R, be: E => R): R = this match {
    case Ok(t) => bt(f(t))
    case Error(e) => be(e)
  }

  private def fmapErr[E2, R](f: E => E2, bt: T => R, be: E2 => R): R = this match {
    case Ok(t) => bt(t)
    case Error(e) => be(f(e))
  }

  final def map[A](f: T => A): ServiceReply[A, E] = fmap(f, Ok[A, E], Error[A, E])
  final def mapErr[U <: ServiceError](f: E => U): ServiceReply[T, U] = fmapErr(f, Ok[T, U], Error[T, U])

  final def isSuccess: Boolean = this.isInstanceOf[Ok[_, _]]
  final def isFailure: Boolean = !isSuccess

  final def onSuccess[A](f: T => A): Unit = fmap(f, return, return)
  final def onFailure[A](f: E => A): Unit = fmapErr(f, return, return)
}

final case class Ok[T, E <: ServiceError](value: T) extends ServiceReply[T, E]
final case class Error[T, E <: ServiceError](error: E) extends ServiceReply[T, E]


object Service {

  type ServiceRep[T] = ServiceReply[T, ServiceError]

  def successful[T, E <: ServiceError](t: T): ServiceReply[T, E] = Ok(t)
  def failed[T, E <: ServiceError](e: E): ServiceReply[T, E] = Error(e)

  def futureSuccessful[T, E <: ServiceError](t: T): Future[ServiceReply[T, E]] = Future.successful(successful(t))
  def futureFailed[T, E <: ServiceError](e: E): Future[ServiceReply[T, E]] = Future.successful(failed(e))


  object Ops {

    implicit class OptionFuture[A](f: Option[Future[A]])(implicit ec: ExecutionContext) {
      def toFutureOption: Future[Option[A]] = Future.sequence[A, Iterable](f).map(_.headOption)
    }


    sealed trait ReplyConverter[T] {
      protected def mapIt[E <: ServiceError](e: E): ServiceReply[T, E]
      final def toNotFound: ServiceReply[T, NotFound] = mapIt(NotFound())
      final def toNotAuthorized: ServiceReply[T, NotAuthorized] = mapIt(NotAuthorized())
      final def toExists[Ex](ex: Ex): ServiceReply[T, Exists[Ex]] = mapIt(Exists(ex))
      final def toBadRequest(message: String): ServiceReply[T, BadRequest] = mapIt(BadRequest(message))
    }

    implicit class ServiceReplyOption[T](r: Option[T]) extends ReplyConverter[T] {
      override protected def mapIt[E <: ServiceError](e: E) = r match {
        case Some(x) => successful(x)
        case None => failed(e)
      }
    }

    implicit class ServiceReplyBoolean(r: Boolean) extends ReplyConverter[Unit] {
      override protected def mapIt[E <: ServiceError](e: E) = if (r) successful(()) else failed(e)
    }

    def toNotFound[T, R](implicit getConverter: T => ReplyConverter[R]): T => ServiceReply[R, NotFound] =
      getConverter andThen (_.toNotFound)

    def toNotAuthorized[T, R](implicit getConverter: T => ReplyConverter[R]): T => ServiceReply[R, NotAuthorized] =
      getConverter andThen (_.toNotAuthorized)

  }

}


trait StatService extends Service {

  val defaultLimit = 3
  val defaultLimitLarge = defaultLimit * 2

}
