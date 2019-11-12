package services

import java.sql.Timestamp
import java.util.concurrent.TimeUnit

import javax.inject.Inject
import models._
import models.daos.BazaarIdeaDAO
import play.api.Configuration
import services.Service.ServiceRep

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}


trait BazaarIdeaService extends Service {

  def all(userId: Long, disabled: Boolean): Future[(Seq[BazaarTeach], Seq[BazaarLearn], Seq[BazaarEvent], Seq[BazaarResearch])]

  def allMixed(userId: Long, disabled: Boolean): Future[Seq[BazaarIdea]]

  def allSlim(userId: Long, disabled: Boolean): Future[Seq[BazaarIdeaSlim]]

  def allTeach(userId: Long, disabled: Boolean): Future[Seq[BazaarTeach]]
  def allLearn(userId: Long, disabled: Boolean): Future[Seq[BazaarLearn]]
  def allEvent(userId: Long, disabled: Boolean): Future[Seq[BazaarEvent]]
  def allResearch(userId: Long, disabled: Boolean): Future[Seq[BazaarResearch]]

  def findTeach(id: Long, userId: Long, disabled: Boolean): Future[ServiceReply[BazaarTeach, NotFound]]
  def findLearn(id: Long, userId: Long, disabled: Boolean): Future[ServiceReply[BazaarLearn, NotFound]]
  def findEvent(id: Long, userId: Long, disabled: Boolean): Future[ServiceReply[BazaarEvent, NotFound]]
  def findResearch(id: Long, userId: Long, disabled: Boolean): Future[ServiceReply[BazaarResearch, NotFound]]

  def create(bazaarTeach: BazaarTeach): Future[BazaarTeach]
  def create(bazaarLearn: BazaarLearn): Future[BazaarLearn]
  def create(bazaarEvent: BazaarEvent): Future[BazaarEvent]
  def create(bazaarResearch: BazaarResearch): Future[BazaarResearch]

  def update(bazaarTeach: BazaarTeach, disabled: Boolean): Future[ServiceRep[BazaarTeach]]
  def update(bazaarLearn: BazaarLearn, disabled: Boolean): Future[ServiceRep[BazaarLearn]]
  def update(bazaarEvent: BazaarEvent, disabled: Boolean): Future[ServiceRep[BazaarEvent]]
  def update(bazaarResearch: BazaarResearch, disabled: Boolean): Future[ServiceRep[BazaarResearch]]

  def deleteTeach(id: Long, requestingUser: User, disabled: Boolean): Future[ServiceRep[Boolean]]
  def deleteLearn(id: Long, requestingUser: User, disabled: Boolean): Future[ServiceRep[Boolean]]
  def deleteEvent(id: Long, requestingUser: User, disabled: Boolean): Future[ServiceRep[Boolean]]
  def deleteResearch(id: Long, requestingUser: User, disabled: Boolean): Future[ServiceRep[Boolean]]

  def enhanceWithPreference(userId: Long, idea: BazaarLearn): Future[BazaarLearn]
  def enhanceWithPreference(userId: Long, idea: BazaarTeach): Future[BazaarTeach]
  def enhanceWithPreference(userId: Long, idea: BazaarEvent): Future[BazaarEvent]
  def enhanceWithPreference(userId: Long, idea: BazaarResearch): Future[BazaarResearch]

  def search(userId: Long, value: String, disabled: Boolean): Future[Seq[BazaarIdea]]

  def favorites(userId: Long, disabled: Boolean): Future[Seq[BazaarIdea]]

  def byUser(userId: Long): Future[Seq[BazaarIdeaMini]]

}


class BazaarIdeaServiceImpl @Inject()(
  bazaarIdeaDAO: BazaarIdeaDAO,
  bazaarPreferenceService: BazaarPreferenceService,
  bazaarCommentService: BazaarCommentService,
  configuration: Configuration
)(implicit ec: ExecutionContext) extends BazaarIdeaService {

  import Service.Ops._


  override def all(userId: Long, disabled: Boolean): Future[(Seq[BazaarTeach], Seq[BazaarLearn], Seq[BazaarEvent], Seq[BazaarResearch])] = for {
    t <- allTeach(userId, disabled)
    l <- allLearn(userId, disabled)
    e <- allEvent(userId, disabled)
    r <- allResearch(userId, disabled)
  } yield (t, l, e, r)

  override def allMixed(userId: Long, disabled: Boolean): Future[Seq[BazaarIdea]] =
    all(userId, disabled).map { case (t, l, e, r) => t ++ l ++ e ++ r }

  override def allSlim(userId: Long, disabled: Boolean) =
    bazaarIdeaDAO.allSlim(userId, disabled)

  private def allForType[T <: BazaarIdea](
    userId: Long, disabled: Boolean, all: Boolean => Future[Seq[T]],
    enhanceWithPreference: (Long, T) => Future[T]
  ) = all(disabled).flatMap(ideas => {
    Future.sequence(ideas.map(enhanceWithPreference(userId, _)))
  })

  override def allTeach(userId: Long, disabled: Boolean) =
    allForType[BazaarTeach](userId, disabled, bazaarIdeaDAO.allTeach, enhanceWithPreference)
  override def allLearn(userId: Long, disabled: Boolean) =
    allForType[BazaarLearn](userId, disabled, bazaarIdeaDAO.allLearn, enhanceWithPreference)
  override def allEvent(userId: Long, disabled: Boolean) =
    allForType[BazaarEvent](userId, disabled, bazaarIdeaDAO.allEvent, enhanceWithPreference)
  override def allResearch(userId: Long, disabled: Boolean) =
    allForType[BazaarResearch](userId, disabled, bazaarIdeaDAO.allResearch, enhanceWithPreference)

  private def find[T <: BazaarIdea](
    id: Long, userId: Long, disabled: Boolean,
    find: (Long, Boolean) => Future[Option[T]], markView: Long => T => Future[T],
    enhanceWithPreference: (Long, T) => Future[T]
  ) = {
    for {
      optIdea <- find(id, disabled)
      viewed <- optionToFutureOption[T](optIdea, markView(userId))
      withPreference <- optionToFutureOption[T](viewed, enhanceWithPreference(userId, _))
    } yield withPreference.toNotFound
  }

  override def findTeach(id: Long, userId: Long, disabled: Boolean) =
    find[BazaarTeach](id, userId, disabled, bazaarIdeaDAO.findTeach, markViewTeach, enhanceWithPreference)
  override def findLearn(id: Long, userId: Long, disabled: Boolean) =
    find[BazaarLearn](id, userId, disabled, bazaarIdeaDAO.findLearn, markViewLearn, enhanceWithPreference)
  override def findEvent(id: Long, userId: Long, disabled: Boolean) =
    find[BazaarEvent](id, userId, disabled, bazaarIdeaDAO.findEvent, markViewEvent, enhanceWithPreference)
  override def findResearch(id: Long, userId: Long, disabled: Boolean) =
    find[BazaarResearch](id, userId, disabled, bazaarIdeaDAO.findResearch, markViewResearch, enhanceWithPreference)


  override def create(bazaarTeach: BazaarTeach) = bazaarIdeaDAO.create(bazaarTeach)
  override def create(bazaarLearn: BazaarLearn) = bazaarIdeaDAO.create(bazaarLearn)
  override def create(bazaarEvent: BazaarEvent) = bazaarIdeaDAO.create(bazaarEvent)
  override def create(bazaarResearch: BazaarResearch) = bazaarIdeaDAO.create(bazaarResearch)


  private def update[T <: BazaarIdea](
    idea: T, disabled: Boolean, find: (Long, Boolean) => Future[Option[T]], update: T => Future[T]
  ): Future[ServiceRep[T]] = {
    val requestingUser = idea.creator

    for {
      optDbIdea <- find(idea.id, disabled)
      result <- optDbIdea match {
        case Some(dbIdea) if requestingUser.isUser && dbIdea.creator.id != requestingUser.id =>
          Service.futureFailed[T, ServiceError](NotAuthorized())
        case Some(_) =>
          update(idea).map(Service.successful[T, ServiceError])
        case None =>
          Service.futureFailed[T, ServiceError](NotFound())
      }
    } yield result
  }

  override def update(bazaarTeach: BazaarTeach, disabled: Boolean) =
    update(bazaarTeach, disabled, bazaarIdeaDAO.findTeach, bazaarIdeaDAO.update)
  override def update(bazaarLearn: BazaarLearn, disabled: Boolean) =
    update(bazaarLearn, disabled, bazaarIdeaDAO.findLearn, bazaarIdeaDAO.update)
  override def update(bazaarEvent: BazaarEvent, disabled: Boolean) =
    update(bazaarEvent, disabled, bazaarIdeaDAO.findEvent, bazaarIdeaDAO.update)
  override def update(bazaarResearch: BazaarResearch, disabled: Boolean) =
    update(bazaarResearch, disabled, bazaarIdeaDAO.findResearch, bazaarIdeaDAO.update)


  private def delete[T <: BazaarIdea](
    ideaId: Long, requestingUser: User, disabled: Boolean,
    find: (Long, Boolean) => Future[Option[T]], delete: Long => Future[Boolean]
  ): Future[ServiceRep[Boolean]] = {
    for {
      optDbIdea <- find(ideaId, disabled)
      result <- optDbIdea match {
        case Some(dbIdea) if requestingUser.isUser && dbIdea.creator.id != requestingUser.id =>
          Service.futureFailed[Boolean, ServiceError](NotAuthorized())
        case Some(_) =>
          delete(ideaId).map(Service.successful[Boolean, ServiceError])
        case None =>
          Service.futureFailed[Boolean, ServiceError](NotFound())
      }
    } yield result
  }

  override def deleteTeach(id: Long, requestingUser: User, disabled: Boolean) =
    delete(id, requestingUser, disabled, bazaarIdeaDAO.findTeach, bazaarIdeaDAO.deleteTeach)
  override def deleteLearn(id: Long, requestingUser: User, disabled: Boolean) =
    delete(id, requestingUser, disabled, bazaarIdeaDAO.findLearn, bazaarIdeaDAO.deleteLearn)
  override def deleteEvent(id: Long, requestingUser: User, disabled: Boolean) =
    delete(id, requestingUser, disabled, bazaarIdeaDAO.findEvent, bazaarIdeaDAO.deleteEvent)
  override def deleteResearch(id: Long, requestingUser: User, disabled: Boolean) =
    delete(id, requestingUser, disabled, bazaarIdeaDAO.findResearch, bazaarIdeaDAO.deleteResearch)


  private val scoreCoefficients = {
    val c = configuration.underlying.getConfig("bazaar.score")
    immutable.Map(
      'alpha -> c.getDouble("alpha"),
      'beta -> c.getDouble("beta"),
      'gamma -> c.getDouble("gamma"),
      'delta -> c.getDouble("delta"),
      't1 -> c.getDouble("t1"),
      'tH -> c.getDouble("tH"),
      'deadline -> c.getDouble("deadline")
    )
  }

  private def score(
    preferenceCount: (Int, Int, Int, Int), commentCount: Int, creation: Timestamp, deadline: Double
  ): Double = {
    val alpha = scoreCoefficients('alpha)
    val beta = scoreCoefficients('beta)
    val gamma = scoreCoefficients('gamma)
    val delta = scoreCoefficients('delta)

    val (views, agree, wish, _) = preferenceCount
    val comments = commentCount - wish

    val fTheta = {
      val t1 = scoreCoefficients('t1)
      val tH =  scoreCoefficients('tH)
      val theta = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - creation.getTime)

      val fraction = (deadline + 1 + t1 * tH / (3 * (1 - tH))) / (deadline * (1 + tH / (3 * (1 - tH))))
      val ro = (tH - 1) / (fraction * deadline - t1)
      val epsilon = tH / (fraction * deadline - (deadline + 1))

      if (theta <= t1) 1
      else if (t1 < theta && theta <= fraction * deadline) 1 - ro * t1 + ro * theta
      else if (fraction * deadline < theta && theta <= deadline) epsilon * theta - epsilon * (deadline + 1)
      else 0
    }

    ((alpha * views) + (delta * agree) + (gamma * wish) + (beta * comments)) * fTheta
  }

  private def scoreAndCounts(
    ideaType: BazaarIdeaType, ideaId: Long,
    createdAt: Timestamp, deadline: Option[Int] = None
  ): Future[(Double, BazaarPreferenceCounts)] =
    for {
      prefCount <- bazaarPreferenceService.count(ideaId, ideaType)
      commCount <- bazaarCommentService.count(ideaId, ideaType)
    } yield (score(prefCount, commCount, createdAt, deadline.map(_.toDouble).getOrElse(scoreCoefficients('deadline))),
            BazaarPreferenceCounts(
              views = prefCount._1,
              agrees = prefCount._2,
              wishes = prefCount._3,
              favorites = prefCount._4,
              comments = commCount
            ))

  private def enhanceWithScore(idea: BazaarIdeaSlim): Future[BazaarIdeaSlim] =
    scoreAndCounts(idea.ideaType, idea.id, idea.createdAt, idea.deadline) map {
      case (s, c) => idea.copy(score = Some(s), counts = Some(c))
    }

  private def enhanceWithScore(idea: BazaarLearn): Future[BazaarLearn] =
    scoreAndCounts(BazaarLearnType, idea.id, idea.createdAt) map {
      case (s, c) => idea.copy(score = Some(s), counts = Some(c))
    }

  private def enhanceWithScore(idea: BazaarTeach): Future[BazaarTeach] =
    scoreAndCounts(BazaarTeachType, idea.id, idea.createdAt) map {
      case (s, c) => idea.copy(score = Some(s), counts = Some(c))
    }

  private def enhanceWithScore(idea: BazaarEvent): Future[BazaarEvent] =
    scoreAndCounts(BazaarEventType, idea.id, idea.createdAt) map {
      case (s, c) => idea.copy(score = Some(s), counts = Some(c))
    }

  private def enhanceWithScore(idea: BazaarResearch): Future[BazaarResearch] =
    scoreAndCounts(BazaarResearchType, idea.id, idea.createdAt, Some(idea.deadline)) map {
      case (s, c) => idea.copy(score = Some(s), counts = Some(c))
    }


  override def enhanceWithPreference(userId: Long, idea: BazaarLearn) =
    bazaarPreferenceService.find(userId, idea.id, BazaarLearnType) map (p => idea.copy(preference = p))

  override def enhanceWithPreference(userId: Long, idea: BazaarTeach) =
    bazaarPreferenceService.find(userId, idea.id, BazaarTeachType) map (p => idea.copy(preference = p))

  override def enhanceWithPreference(userId: Long, idea: BazaarEvent) =
    bazaarPreferenceService.find(userId, idea.id, BazaarEventType) map (p => idea.copy(preference = p))

  override def enhanceWithPreference(userId: Long, idea: BazaarResearch) =
    bazaarPreferenceService.find(userId, idea.id, BazaarResearchType) map (p => idea.copy(preference = p))

  private def markView(bazaarIdeaType: BazaarIdeaType, userId: Long, idea: BazaarIdea): Future[BazaarPreference] =
    for {
      pref <- bazaarPreferenceService.find(userId, idea.id, bazaarIdeaType)
      updatedPref <- pref match {
        case Some(preference) if !preference.viewed =>
          bazaarPreferenceService.upsertFlags(preference.copy(viewed = true))
        case Some(preference) =>
          Future.successful(preference)
        case None =>
          val preference = BazaarPreference(0, userId, idea.id, bazaarIdeaType, agree = false, None, favorite = false, viewed = true)
          bazaarPreferenceService.upsertFlags(preference)
      }
    } yield updatedPref

  private def markViewLearn(userId: Long)(idea: BazaarLearn) =
    markView(BazaarLearnType, userId, idea).map(p => idea.copy(preference = Some(p)))

  private def markViewTeach(userId: Long)(idea: BazaarTeach) =
    markView(BazaarTeachType, userId, idea).map(p => idea.copy(preference = Some(p)))

  private def markViewEvent(userId: Long)(idea: BazaarEvent) =
    markView(BazaarEventType, userId, idea).map(p => idea.copy(preference = Some(p)))

  private def markViewResearch(userId: Long)(idea: BazaarResearch) =
    markView(BazaarResearchType, userId, idea).map(p => idea.copy(preference = Some(p)))


  override def search(userId: Long, value: String, disabled: Boolean) =
    bazaarIdeaDAO.search(value, disabled).flatMap { ideas =>
      Future.sequence(ideas.map {
        case idea: BazaarTeach => enhanceWithPreference(userId, idea)
        case idea: BazaarLearn => enhanceWithPreference(userId, idea)
        case idea: BazaarEvent => enhanceWithPreference(userId, idea)
        case idea: BazaarResearch => enhanceWithPreference(userId, idea)
      })
    }

  override def favorites(userId: Long, disabled: Boolean) = bazaarIdeaDAO.favorites(userId, disabled)

  override def byUser(userId: Long) = bazaarIdeaDAO.byUser(userId)

}
