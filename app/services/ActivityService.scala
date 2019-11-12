package services

import javax.inject.Inject
import com.braintreegateway.Transaction
import com.braintreegateway.Transaction.Status
import fly.play.s3.{BucketFile, S3Exception}
import models._
import models.daos.ActivityResearchDAO.{ApplicationReply, DeadlinePassed, RoleNotFound}
import models.daos.{ActivityResearchDAO, ActivityTeachEventDAO}
import play.api.Logger
import services.Service._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{existentials, implicitConversions}


object ActivityService {

  sealed trait ApplicationsFilter
  final case class ApplicationsFilterUser(userId: Long) extends ApplicationsFilter
  final case object ApplicationsFilterAdmin extends ApplicationsFilter

  implicit def optLongToApplicationsFilter(opt: Option[Long]): ApplicationsFilter = opt match {
    case Some(userId) => ApplicationsFilterUser(userId)
    case None => ApplicationsFilterAdmin
  }

}

trait ActivityService extends Service {

  import ActivityService._

  def all(language: Language, userId: Long, fromAdmin: Boolean,
          search: Option[String] = None,
          searchSkillIds: Seq[Long] = Seq(),
          matchAll: Boolean = false,
          future: Boolean = false): Future[Seq[ActivitySlim]]

  def allTeach(language: Language, userId: Long, future: Boolean = true): Future[Seq[ActivityTeachSlim]]
  def allEvent(language: Language, userId: Long, future: Boolean = true): Future[Seq[ActivityEventSlim]]
  def allResearch(language: Language, userId: Long, future: Boolean = true): Future[Seq[ActivityResearchSlim]]

  def findTeach(id: Long, language: Language, userId: Long, future: Boolean): Future[ServiceReply[ActivityTeach, NotFound]]
  def findEvent(id: Long, language: Language, userId: Long, future: Boolean): Future[ServiceReply[ActivityEvent, NotFound]]
  def findResearch(id: Long, language: Language, userId: Long, future: Boolean): Future[ServiceReply[ActivityResearch, NotFound]]

  def create[A <: ActivityTeachEvent[A]](activity: A): Future[A]
  def create(activity: ActivityResearch): Future[ActivityResearch]

  def update[A <: ActivityTeachEvent[A]](activity: A): Future[ServiceReply[A, NotFound]]
  def update(activity: ActivityResearch): Future[ServiceReply[ActivityResearch, NotFound]]

  def deleteTeachEvent(id: Long): Future[ServiceReply[Unit, NotFound]]
  def deleteResearch(id: Long): Future[ServiceReply[Unit, NotFound]]

  def favoriteTeachEvent(activityId: Long, userId: Long, favorite: Boolean): Future[ServiceReply[Unit, NotFound]]
  def favoriteResearch(activityId: Long, userId: Long, favorite: Boolean): Future[ServiceReply[Unit, NotFound]]
  def favorites(userId: Long, language: Language): Future[Seq[ActivitySlim]]

  def subscribe(id: Long, userId: Long, language: Language, paymentInfo: Option[PaymentInfo]): Future[ServiceRep[ActivitySubscription]]
  def subscriptions(id: Long): Future[Seq[AdminActivitySubscription]]
  def verifySubscription(id: Long, userId: Long, success: Boolean): Future[ServiceRep[AdminActivitySubscription]]
  def deleteSubscription(id: Long, userId: Long): Future[ServiceReply[Unit, NotFound]]

  def changeApplication(roleId: Long, applied: Boolean, app: ActivityResearchApp)
    : Future[ServiceReply[Option[ActivityResearchApp], _ >: NotFound with BadRequest <: ServiceError]]
  def applications(activityId: Long, applicationsFilter: ApplicationsFilter): Future[ServiceRep[Seq[ActivityResearchRole]]]

  def researchByUser(userId: Long, language: Language): Future[Seq[ActivityMini]]

}


private object ActivityServiceImpl {

  sealed trait ActivityObjectType
  final object ActivityObjectEvent extends ActivityObjectType
  final object ActivityObjectTeach extends ActivityObjectType
  final object ActivityObjectResearch extends ActivityObjectType

  type AnyTeachEvent = A forSome { type A <: ActivityTeachEvent[A] }

}


class ActivityServiceImpl @Inject()(
  activityTeachEventDAO: ActivityTeachEventDAO,
  activityResearchDAO: ActivityResearchDAO,
  s3Service: S3Service,
  imageGalleryService: ImageGalleryService,
  braintreeService: BraintreeService
)(implicit ec: ExecutionContext) extends ActivityService {

  import ActivityService._
  import ActivityServiceImpl._
  import Service.Ops._

  private def getCoverS3Filename(folder: String, activityId: Long, extension: String) =
    s"covers/$folder/$activityId.$extension"

  private def getCoverS3Filename[A <: Activity[A]](activity: A): String =
    getCoverS3Filename(getCoverS3Folder(activity), activity.id, activity.coverPic.extension)

  private def getCoverS3FilenameWoExt[A <: Activity[A]](activity: A): String =
    s"covers/${getCoverS3Folder(activity)}/${activity.id}"

  private def getCoverS3Folder(activityObjectType: ActivityObjectType): String = activityObjectType match {
    case ActivityObjectTeach => "teach"
    case ActivityObjectEvent => "event"
    case ActivityObjectResearch => "research"
  }

  private implicit def activityToObjectType(activity: Activity[_]): ActivityObjectType = activity match {
    case _: ActivityTeach => ActivityObjectTeach
    case _: ActivityEvent => ActivityObjectEvent
    case _: ActivityResearch => ActivityObjectResearch
  }

  private def getCoverUrl[A <: Activity[A]](activity: A): String = {
    val s3path = getCoverS3Filename(getCoverS3Folder(activity), activity.id, activity.coverPic.extension)
    s3Service.getActivityImagesBucket.url(s3path)
  }

  private def enhance[A <: Activity[A]](activity: A): A = {
    val gallery = imageGalleryService.fillGalleryUrls(activity.gallery)
    val coverPic = activity.coverPic.copy(url = Some(getCoverUrl(activity)))
    activity.updateActivity(coverPic = coverPic, gallery = gallery)
  }

  private def zipActivities(
    teachEvent: Future[Seq[ActivityTeachEventSlim]],
    research: Future[Seq[ActivityResearchSlim]]
  ): Future[Seq[ActivitySlim]] =
    teachEvent zip research map { case (tes, rs) =>
      Seq.empty[ActivitySlim] ++ tes ++ rs
    }

  override def all(language: Language, userId: Long, fromAdmin: Boolean,
                   search: Option[String], searchSkillIds: Seq[Long],
                   matchAll: Boolean, future: Boolean) = {
    val teachEvent = activityTeachEventDAO.all(language, userId, fromAdmin, search, searchSkillIds, matchAll, future)
    val research = activityResearchDAO.all(language, userId, fromAdmin, search, searchSkillIds, matchAll, future)
    zipActivities(teachEvent, research)
  }

  override def allTeach(language: Language, userId: Long, future: Boolean) =
    activityTeachEventDAO.allTeach(language, userId, future)

  override def allEvent(language: Language, userId: Long, future: Boolean) =
    activityTeachEventDAO.allEvent(language, userId, future)

  override def allResearch(language: Language, userId: Long, future: Boolean) =
    activityResearchDAO.all(language, userId, fromAdmin = false, None, Seq.empty, matchAll = false, future = future)

  override def findTeach(id: Long, language: Language, userId: Long, future: Boolean) =
    activityTeachEventDAO.findTeach(id, language, userId, future).map(_.map(enhance).toNotFound)

  override def findEvent(id: Long, language: Language, userId: Long, future: Boolean) =
    activityTeachEventDAO.findEvent(id, language, userId, future).map(_.map(enhance).toNotFound)

  override def findResearch(id: Long, language: Language, userId: Long, future: Boolean) =
    activityResearchDAO.find(id, language, userId, future).map(_.map(enhance).toNotFound)

  private def findTeachEvent(id: Long, language: Language, userId: Long): Future[Option[AnyTeachEvent]] = {
    for {
      optTeach <- activityTeachEventDAO.findTeach(id, language, userId, future = true)
        .map(_.map(a => a: AnyTeachEvent))
      optActivity <- optTeach match {
        case Some(a) => Future.successful(Some(a))
        case None => activityTeachEventDAO.findEvent(id, language, userId, future = true)
          .map(_.map(a => a: AnyTeachEvent))
      }
    } yield optActivity
  }

  private def uploadCoverPic[A <: Activity[A]](activity: A, maxSize: Int = 2048): Future[Unit] = {
    val deletionFuture = try {
      val path = getCoverS3FilenameWoExt(activity)
      s3Service.getActivityImagesBucket.list(path).flatMap { list =>
        Future.sequence(list.map(s3Service.getActivityImagesBucket - _.name))
      }
    } catch {
      case e: S3Exception =>
        Logger.warn(s"failed to delete cover pics: ${e.message} (code: ${e.code})")
        Future.successful(Seq.empty)
    }

    deletionFuture flatMap { _ =>
      val resizedPic = s3Service.resizePic(activity.coverPic.data.get, maxSize)
      s3Service.getActivityImagesBucket +
        BucketFile(getCoverS3Filename(activity), s"image/${activity.coverPic.extension}", resizedPic)
    }
  }

  override def create[A <: ActivityTeachEvent[A]](activity: A): Future[A] = {
    for {
      activity <- activityTeachEventDAO.create(activity)
      _ <- uploadCoverPic(activity)
    } yield enhance(activity)
  }

  override def create(activity: ActivityResearch): Future[ActivityResearch] = {
    for {
      activity <- activityResearchDAO.create(activity)
      _ <- uploadCoverPic(activity)
    } yield enhance(activity)
  }

  override def update[A <: ActivityTeachEvent[A]](activity: A): Future[ServiceReply[A, NotFound]] = {
    for {
      optActivity <- activityTeachEventDAO.update(activity)
      _ <- {
        if (optActivity.isDefined && activity.coverPic.data.isDefined)
          uploadCoverPic(activity)
        else Future.successful(())
      }
    } yield optActivity.map(enhance).toNotFound
  }

  override def update(activity: ActivityResearch): Future[ServiceReply[ActivityResearch, NotFound]] = {
    for {
      optActivity <- activityResearchDAO.update(activity)
      _ <- {
        if (optActivity.isDefined && activity.coverPic.data.isDefined)
          uploadCoverPic(activity)
        else Future.successful(())
      }
    } yield optActivity.map(enhance).toNotFound
  }

  override def deleteTeachEvent(id: Long) = for {
    optCover <- activityTeachEventDAO.getCoverPic(id)
    _ <- optCover.map { case (isTeach, extension) =>
      val folder = if (isTeach) getCoverS3Folder(ActivityObjectTeach) else getCoverS3Folder(ActivityObjectEvent)
      s3Service.getActivityImagesBucket - getCoverS3Filename(folder, id, extension)
    }.toFutureOption
    r <- if (optCover.isDefined) activityTeachEventDAO.delete(id) else Future.successful(false)
  } yield r.toNotFound

  override def deleteResearch(id: Long) = for {
    optCover <- activityResearchDAO.getCoverPic(id)
    _ <- optCover.map { extension =>
      val folder = getCoverS3Folder(ActivityObjectResearch)
      s3Service.getActivityImagesBucket - getCoverS3Filename(folder, id, extension)
    }.toFutureOption
    r <- if (optCover.isDefined) activityResearchDAO.delete(id) else Future.successful(false)
  } yield r.toNotFound

  override def favoriteTeachEvent(activityId: Long, userId: Long, favorite: Boolean) =
    activityTeachEventDAO.favorite(activityId, userId, favorite).map(_.toNotFound)

  override def favoriteResearch(activityId: Long, userId: Long, favorite: Boolean) =
    activityResearchDAO.favorite(activityId, userId, favorite).map(_.toNotFound)

  override def favorites(userId: Long, language: Language) = {
    val teachEvent = activityTeachEventDAO.favorites(userId, language)
    val research = activityResearchDAO.favorites(userId, language)
    zipActivities(teachEvent, research)
  }

  private def createPayedSubscription[A <: ActivityTeachEvent[A]](activity: A, userId: Long, paymentInfo: PaymentInfo) = {
    braintreeService.createTransaction(paymentInfo.amount, paymentInfo.paymentNonce.get) match {
      case Ok(transaction) =>
        val piWithTransId = paymentInfo.copy(transactionId = Some(transaction.getId))

        val subFuture = activityTeachEventDAO.subscribe(activity.id, userId, Some(piWithTransId))

        def voidTransaction(transaction: Transaction) = braintreeService.voidTransaction(transaction) onFailure {
          case GenericError(voidMsg) =>
            Logger.error(s"failed to void transaction ${transaction.getId}: $voidMsg")
        }

        subFuture.onFailure {
          case throwable =>
            Logger.error(s"failed to create subscription for user $userId on ${activity.id}: ${throwable.getMessage}")
            voidTransaction(transaction)
        }

        subFuture flatMap { sub =>
          transaction.getStatus match {
            case Status.AUTHORIZED =>
              braintreeService.submitTransaction(transaction) match {
                case Ok(_) => futureSuccessful(sub)
                case Error(GenericError(submitMsg)) =>
                  voidTransaction(transaction)
                  val errorMsg = s"failed to submit transaction ${transaction.getId} for settlement: $submitMsg"
                  activityTeachEventDAO.deleteSubscription(activity.id, userId)
                    .map(_ => failed(GenericError(errorMsg)))
              }
            case Status.SETTLED | Status.SETTLING =>
              futureSuccessful(sub)
            case status =>
              val errorMsg = s"unexpected transaction status $status for ${transaction.getId}"
              voidTransaction(transaction)
              futureFailed(GenericError(errorMsg))
          }
        }
      case Error(GenericError(msg)) =>
        futureFailed(GenericError(s"failed to create transaction: $msg"))
    }
  }

  private def createSubscription(id: Long, userId: Long, language: Language, paymentInfo: Option[PaymentInfo]) =
    for {
      optActivity <- findTeachEvent(id, language, userId)
      res <- optActivity match {
        case Some(activity) if activity.optDeadline.isEmpty =>
          futureFailed(BadRequest("activity is not subscribable"))
        case Some(activity) if activity.deadlineToCheck.get < System.currentTimeMillis() =>
          futureFailed(BadRequest("activity subscription deadline has passed"))
        case Some(activity) if activity.payments =>
          paymentInfo match {
            case Some(pi) if pi.amount != activity.costs.get =>
              futureFailed(BadRequest("payment amount does not match activity's cost"))
            case Some(pi @ PaymentInfo(_, method, None, None, None, _, Some(_))) if method == PayPal || method == CreditCard =>
              createPayedSubscription(activity, userId, pi)
            case Some(pi @ PaymentInfo(_, WireTransfer, None, Some(_), None, _, None)) =>
              activityTeachEventDAO.subscribe(id, userId, Some(pi.copy(verified = Some(false)))).map(successful)
            case Some(_) => futureFailed(BadRequest("invalid payment info"))
            case None => futureFailed(BadRequest("activity requires payment"))
          }
        case Some(_) =>
          activityTeachEventDAO.subscribe(id, userId, None).map(successful)
        case None =>
          futureFailed(NotFound())
      }
    } yield res

  override def subscribe(id: Long, userId: Long, language: Language, paymentInfo: Option[PaymentInfo]) = {
    for {
      optSub <- activityTeachEventDAO.findSubscription(id, userId)
      res <- optSub match {
        case Some(sub) => futureFailed(Exists(sub))
        case None => createSubscription(id, userId, language, paymentInfo)
      }
    } yield res
  }

  override def subscriptions(id: Long) = activityTeachEventDAO.subscriptions(id)

  override def verifySubscription(id: Long, userId: Long, success: Boolean) = {
    for {
      optSub <- activityTeachEventDAO.findSubscription(id, userId)
      adminActivitySub <- optSub match {
        case Some(ActivitySubscription(_, Some(WireTransfer), _, Some(_), None, Some(_))) =>
          activityTeachEventDAO.verifySubscription(id, userId, success).map(successful)
        case Some(ActivitySubscription(_, Some(_), _, _, _, _)) =>
          futureFailed(BadRequest("payment method does not support verification"))
        case Some(ActivitySubscription(_, _, _, None, _, _)) =>
          futureFailed(BadRequest("CRO is missing from payment info"))
        case Some(_) =>
          futureFailed(BadRequest("invalid payment info"))
        case None =>
          futureFailed(NotFound())
      }
    } yield adminActivitySub
  }

  override def deleteSubscription(id: Long, userId: Long) =
    activityTeachEventDAO.deleteSubscription(id, userId).map(_.toNotFound)

  override def changeApplication(roleId: Long, applied: Boolean, app: ActivityResearchApp) =
    activityResearchDAO.changeApplication(roleId, applied, app).map {
      case RoleNotFound => failed(NotFound())
      case DeadlinePassed => failed(BadRequest("The deadline for this activity has passed"))
      case ApplicationReply(reply) => successful(reply)
    }

  override def applications(activityId: Long, applicationsFilter: ApplicationsFilter) = applicationsFilter match {
    case ApplicationsFilterUser(userId) =>
      activityResearchDAO.userHasAccess(activityId, userId).flatMap { hasAccess =>
        if (hasAccess)
          activityResearchDAO.applications(activityId).map(_.toNotFound)
        else
          futureFailed(NotAuthorized())
      }
    case ApplicationsFilterAdmin =>
      activityResearchDAO.applications(activityId).map(_.toNotFound)
  }

  override def researchByUser(userId: Long, language: Language) =
    activityResearchDAO.byUser(userId, language)

}
