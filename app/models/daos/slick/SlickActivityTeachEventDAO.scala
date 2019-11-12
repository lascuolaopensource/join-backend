package models.daos.slick


import javax.inject.Inject
import models._
import models.daos.ActivityTeachEventDAO
import models.daos.slick.tables.SlickActivityTeachEventTables._
import models.daos.slick.tables.SlickImageGalleryTables._
import models.daos.slick.tables.SlickPaymentInfoTables._
import models.daos.slick.tables.SlickSkillTable.DBSkill
import models.daos.slick.tables.SlickTopicTable.DBTopic
import models.daos.slick.tables.SlickUserTable.DBUser
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext
import scala.language.{existentials, implicitConversions, postfixOps}


private object SlickActivityTeachEventDAO {

  implicit def categoryToInt(category: TeachCategory): Int = category match {
    case CategoryX => 0
    case CategoryY => 1
    case CategoryZ => 2
  }

  implicit def intToCategory(int: Int): TeachCategory = int match {
    case 0 => CategoryX
    case 1 => CategoryY
    case 2 => CategoryZ
  }

  private val dbToGuest = (guest: DBActivityTeachEventGuest, guestT: DBActivityTeachEventGuestT) =>
    ActivityGuest(
      id = guest.id,
      userId = guest.userId,
      firstName = guest.firstName,
      lastName = guest.lastName,
      title = guestT.title,
      bio = guestT.bio
    )

  private def getDBActivityT(activity: ActivityTeachEvent[_], activityId: Long) =
    DBActivityTeachEventT(
      activityId = activityId,
      language = activity.language.language,
      title = activity.title,
      description = activity.description,
      outputType = activity.outputType,
      outputDescription = activity match {
        case t: ActivityTeach => Some(t.outputDescription)
        case _ => None
      },
      program = activity.program)

  private def getActivitySub(paymentInfo: (DBActivityTeachEventSub, Option[DBPaymentInfo])) = paymentInfo match {
    case (sub, Some(pi)) => ActivitySubscription(
      createdAt = sub.createdAt,
      paymentMethod = Some(pi.paymentMethod),
      verified = pi.verified,
      cro = pi.cro,
      transactionId = pi.transactionId,
      amount = Some(pi.amount))
    case (sub, None) => ActivitySubscription(sub.createdAt)
  }


  private type BuildModelFn[A] = Language => (DBActivityTeachEvent, DBActivityTeachEventT,
    DBImageGallery, Seq[DBImageGalleryImage], Seq[Int], Seq[DBTopic],
    Seq[DBActivityTeachEventDate], Seq[(DBActivityTeachEventGuest, DBActivityTeachEventGuestT)],
    Seq[(DBActivityTeachEventSkill, DBSkill)], Option[Boolean],
    Option[(DBActivityTeachEventSub, Option[DBPaymentInfo])], Option[Int]) => A

}


class SlickActivityTeachEventDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends ActivityTeachEventDAO with SlickActivityHelpers {

  import SlickActivityHelpers._
  import SlickActivityTeachEventDAO._
  import models.Implicits.DefaultWithId._
  import profile.api._

  override protected val ActivityTopicQuery = ActivityTeachEventTopicQuery
  override protected val ActivityFavoriteQuery = ActivityTeachEventFavoriteQuery


  private def getActivitySchedule(activity: DBActivityTeachEvent): ActivitySchedule =
    if (activity.recurringDays.isDefined) {
      RecurringMeetings(
        days = activity.recurringDays.get,
        every = activity.recurringEvery.get,
        entity = activity.recurringEntity.get,
        hours = activity.recurringHours.get
      )
    } else {
      FiniteMeetings(activity.totalDays.get, activity.totalHours.get)
    }

  private def dbTeachToTeach(language: Language)(activity: DBActivityTeachEvent, translation: DBActivityTeachEventT,
    gallery: DBImageGallery, images: Seq[DBImageGalleryImage], audience: Seq[Int], topics: Seq[DBTopic],
    dates: Seq[DBActivityTeachEventDate], guests: Seq[(DBActivityTeachEventGuest, DBActivityTeachEventGuestT)],
    skills: Seq[(DBActivityTeachEventSkill, DBSkill)], favorite: Option[Boolean],
    paymentInfo: Option[(DBActivityTeachEventSub, Option[DBPaymentInfo])],
    subscriptions: Option[Int]
  ) =
    ActivityTeach(
      id = activity.id,
      language = language,
      title = translation.title,
      coverPic = DataImage(activity.coverExt),
      gallery = dbToImageGallery(gallery, images),
      level = activity.level.map(l => l: SOSLevel),
      audience = audience.map(a => a: Audience),
      topics = topics.map(t => t: Topic),
      description = translation.description,
      outputType = translation.outputType,
      outputDescription = translation.outputDescription.get,
      program = translation.program,
      activityType = activity.activityType,
      costs = activity.costs,
      payments = activity.payments,
      deadline = activity.deadline.get,
      minParticipants = activity.minParticipants,
      maxParticipants = activity.maxParticipants,
      schedule = getActivitySchedule(activity),
      dates = dates.map(d => SOSDate(d.id, d.date, d.startTime, d.endTime)),
      startTime = activity.startTime,
      teachCategory = activity.teachCategory.get,
      guests = guests.map(dbToGuest.tupled),
      requiredSkills = skills.filter(_._1.required).map(_._2: Skill),
      acquiredSkills = skills.filter(_._1.acquired).map(_._2: Skill),
      bazaarIdeaId = activity.bazaarTeachLearnId,
      createdAt = activity.createdAt,
      updatedAt = activity.updatedAt,
      favorite = favorite,
      subscription = paymentInfo.map(getActivitySub),
      subscriptions = subscriptions)

  private def dbTeachToTeachS(language: Language, now: Long)(
    activity: DBActivityTeachEvent, translation: DBActivityTeachEventT,
    topics: Seq[DBTopic], guests: Seq[DBActivityTeachEventGuest],
    skills: Seq[(DBActivityTeachEventSkill, DBSkill)], favorite: Option[Boolean],
    subscriptions: Option[Int]
  ) =
    ActivityTeachSlim(
      id = activity.id,
      title = translation.title,
      topics = topics.map(t => t: Topic),
      deadline = ActivityDeadline(
        date = activity.deadline.get,
        closed = deadlineClosed(activity.deadline.get, now)),
      startTime = activity.startTime,
      guests = guests.map { g =>
        ActivityGuestSlim(g.id, g.firstName, g.lastName, g.userId)
      },
      requiredSkills = skills.filter(_._1.required).map(_._2: Skill),
      acquiredSkills = skills.filter(_._1.acquired).map(_._2: Skill),
      bazaarIdeaId = activity.bazaarTeachLearnId,
      participantsCount = subscriptions,
      minParticipants = activity.minParticipants,
      totalHours = activity.totalHours,
      createdAt = activity.createdAt,
      updatedAt = activity.updatedAt,
      favorite = favorite
    )


  private def dbEventToEvent(language: Language)(activity: DBActivityTeachEvent, translation: DBActivityTeachEventT,
    gallery: DBImageGallery, images: Seq[DBImageGalleryImage], audience: Seq[Int], topics: Seq[DBTopic],
    dates: Seq[DBActivityTeachEventDate], guests: Seq[(DBActivityTeachEventGuest, DBActivityTeachEventGuestT)],
    skills: Seq[(DBActivityTeachEventSkill, DBSkill)], favorite: Option[Boolean],
    paymentInfo: Option[(DBActivityTeachEventSub, Option[DBPaymentInfo])],
    subscriptions: Option[Int]
  ): ActivityEvent =
    ActivityEvent(
      id = activity.id,
      language = language,
      title = translation.title,
      coverPic = DataImage(activity.coverExt),
      gallery = dbToImageGallery(gallery, images),
      level = activity.level.map(l => l: SOSLevel),
      audience = audience.map(a => a: Audience),
      topics = topics.map(t => t: Topic),
      description = translation.description,
      outputType = translation.outputType,
      program = translation.program,
      activityType = activity.activityType,
      costs = activity.costs,
      payments = activity.payments,
      deadline = activity.deadline,
      minParticipants = activity.minParticipants,
      maxParticipants = activity.maxParticipants,
      schedule = getActivitySchedule(activity),
      dates = dates.map(d => SOSDate(d.id, d.date, d.startTime, d.endTime)),
      startTime = activity.startTime,
      guests = guests.map(dbToGuest.tupled),
      requiredSkills = skills.filter(_._1.required).map(_._2: Skill),
      acquiredSkills = skills.filter(_._1.acquired).map(_._2: Skill),
      bazaarIdeaId = activity.bazaarEventId,
      createdAt = activity.createdAt,
      updatedAt = activity.updatedAt,
      favorite = favorite,
      subscription = paymentInfo.map(getActivitySub),
      subscriptions = subscriptions)

  private def dbEventToEventS(language: Language, now: Long)(
    activity: DBActivityTeachEvent, translation: DBActivityTeachEventT,
    topics: Seq[DBTopic], guests: Seq[DBActivityTeachEventGuest],
    skills: Seq[(DBActivityTeachEventSkill, DBSkill)], favorite: Option[Boolean],
    subscriptions: Option[Int]
  ) =
    ActivityEventSlim(
      id = activity.id,
      title = translation.title,
      topics = topics.map(t => t: Topic),
      deadline = activity.deadline.map { deadline =>
        ActivityDeadline(
          date = deadline,
          closed = deadlineClosed(deadline, now))
      },
      startTime = activity.startTime,
      guests = guests.map { g =>
        ActivityGuestSlim(g.id, g.firstName, g.lastName, g.userId)
      },
      requiredSkills = skills.filter(_._1.required).map(_._2: Skill),
      acquiredSkills = skills.filter(_._1.acquired).map(_._2: Skill),
      bazaarIdeaId = activity.bazaarTeachLearnId,
      participantsCount = subscriptions,
      minParticipants = activity.minParticipants,
      totalHours = activity.totalHours,
      createdAt = activity.createdAt,
      updatedAt = activity.updatedAt,
      favorite = favorite
    )


  private def queryT(language: Language, future: Boolean) = {
    val q = ActivityTeachEventQuery.join(ActivityTeachEventTQuery).on(_.id === _.activityId)
      .filter(_._2.language === language.language)
    (if (future) q.filter(a => a._1.startTime.isDefined && a._1.startTime > currentTimestamp())
     else q).sortBy(_._1.startTime.desc.nullsLast)
  }


  private def queryAll(language: Language, userId: Long, fromAdmin: Boolean, queryFilter: GenericQueryFilter) = for {
    activitiesT <- (queryFilter match {
      case QueryBySearch(optS, skillIds, matchAll, future) =>
        val q = queryT(language, future)
        val q1 = optS match {
          case Some(s) => q.filter {
            case (a, aT) =>
              val v = s"%${s.toLowerCase}%"
              aT.title.toLowerCase.like(v) || a.id.in {
                ActivityTeachEventGuestQuery
                  .filter(g => (g.firstName ++ " " ++ g.lastName).toLowerCase like v)
                  .map(_.activityId).distinct
              }
          }
          case None => q
        }
        if (skillIds.nonEmpty) {
          q1.filter(_._1.id in {
            val subQ = ActivityTeachEventSkillQuery.filter(_.skillId inSet skillIds)
            if (matchAll)
              subQ.groupBy(_.activityId)
                .map { case (aId, g) => (aId, g.length) }
                .filter(_._2 === skillIds.length).map(_._1)
            else subQ.map(_.activityId)
          })
        } else q1
      case QueryByIds(ids) => queryT(language, future = false).filter(_._1.id inSet ids)
      case QueryTeach(future) => teachQuery(language, future)
      case QueryEvent(future) => eventQuery(language, future)
      case QueryNoFilter => queryT(language, future = false)
    }).result

    activityIds = activitiesT.map(_._1.id).toSet

    topics <- {
      val q = ActivityTeachEventTopicQuery.join(TopicQuery).on(_.topicId === _.id)
      if (queryFilter.applyFilter)
        q.filter(_._1.activityId inSet activityIds)
      else q
    }.result

    guests <- {
      val q = ActivityTeachEventGuestQuery
      if (queryFilter.applyFilter)
        q.filter(_.activityId inSet activityIds)
      else q
    }.result

    skills <- {
      val q = ActivityTeachEventSkillQuery.join(SkillQuery).on(_.skillId === _.id)
      if (queryFilter.applyFilter)
        q.filter(_._1.activityId inSet activityIds)
      else q
    }.result

    favorites <- {
      if (fromAdmin)
        DBIO.successful(None)
      else {
        val q = ActivityTeachEventFavoriteQuery.filter(_.userId === userId)
        (if (queryFilter.applyFilter)
          q.filter(_.activityId inSet activityIds)
        else q).result.map(seq => Some(seq.toMap))
      }
    }

    subscriptions <- {
      if (fromAdmin) {
        (if (queryFilter.applyFilter)
          ActivityTeachEventSubQuery.filter(_.activityId.inSet(activityIds))
        else ActivityTeachEventSubQuery)
          .groupBy(_.activityId).map { case (aId, g) => (aId, g.length) }
          .result.map(seq => Some(seq.toMap))
      } else
        DBIO.successful(None)
    }
  } yield (activitiesT, topics, guests, skills, favorites, subscriptions)


  private def getCompositionMaps(
    topics: Seq[((Long, Long), DBTopic)],
    guests: Seq[DBActivityTeachEventGuest],
    skills: Seq[(DBActivityTeachEventSkill, DBSkill)]
  ) = {
    val topicsMap = topics.groupBy(_._1._1).mapValues(_.map(_._2))
    val guestsMap = guests.groupBy(_.activityId)
    val skillsMap = skills.groupBy(_._1.activityId)
    (topicsMap, guestsMap, skillsMap)
  }

  private def composeAll(language: Language)(
    tpl: (Seq[(DBActivityTeachEvent, DBActivityTeachEventT)], Seq[((Long, Long), DBTopic)],
      Seq[DBActivityTeachEventGuest], Seq[(DBActivityTeachEventSkill, DBSkill)],
      Option[Map[Long, Long]], Option[Map[Long, Int]])
  ) = tpl match {
    case (activitiesT, topics, guests, skills, favorites, subscriptions) =>
      val now = System.currentTimeMillis()
      val teachFn = dbTeachToTeachS _ apply (language, now) tupled
      val eventFn = dbEventToEventS _ apply (language, now) tupled

      val (topicsMap, guestsMap, skillsMap) = getCompositionMaps(topics, guests, skills)

      activitiesT.map { case (activity, translations) =>
        val topics_ = topicsMap.getOrElse(activity.id, Seq())
        val guests_ = guestsMap.getOrElse(activity.id, Seq())
        val skills_ = skillsMap.getOrElse(activity.id, Seq())
        val favorite = favorites.map(_.contains(activity.id))
        val subsCount = subscriptions.map(_.getOrElse(activity.id, 0))

        val args = (activity, translations, topics_, guests_, skills_, favorite, subsCount)

        if (activity.isTeach) teachFn(args)
        else eventFn(args)
      }
  }


  override def all(language: Language, userId: Long, fromAdmin: Boolean, search: Option[String],
                   searchSkillIds: Seq[Long], matchAll: Boolean, future: Boolean) = {
    val q = if (search.isEmpty && searchSkillIds.isEmpty && !future) QueryNoFilter
            else QueryBySearch(search, searchSkillIds, matchAll, future)
    db run queryAll(language, userId, fromAdmin, q) map composeAll(language)
  }

  override def allTeach(language: Language, userId: Long, future: Boolean) =
    db run queryAll(language, userId, fromAdmin = false, QueryTeach(future)) map {
      case (activitiesT, topics, guests, skills, favorites, _) =>
        val now = System.currentTimeMillis()
        val (topicsMap, guestsMap, skillsMap) = getCompositionMaps(topics, guests, skills)

        activitiesT.map { case (activity, translations) =>
          val topics_ = topicsMap.getOrElse(activity.id, Seq())
          val guests_ = guestsMap.getOrElse(activity.id, Seq())
          val skills_ = skillsMap.getOrElse(activity.id, Seq())
          val favorite = favorites.map(_.contains(activity.id))

          dbTeachToTeachS(language, now)(activity, translations, topics_, guests_, skills_, favorite, None)
        }
    }

  override def allEvent(language: Language, userId: Long, future: Boolean) =
    db run queryAll(language, userId, fromAdmin = false, QueryEvent(future)) map {
      case (activitiesT, topics, guests, skills, favorites, _) =>
        val now = System.currentTimeMillis()
        val (topicsMap, guestsMap, skillsMap) = getCompositionMaps(topics, guests, skills)

        activitiesT.map { case (activity, translations) =>
          val topics_ = topicsMap.getOrElse(activity.id, Seq())
          val guests_ = guestsMap.getOrElse(activity.id, Seq())
          val skills_ = skillsMap.getOrElse(activity.id, Seq())
          val favorite = favorites.map(_.contains(activity.id))

          dbEventToEventS(language, now)(activity, translations, topics_, guests_, skills_, favorite, None)
        }
    }


  private def querySubscription(activityId: Long, userId: Long) =
    ActivityTeachEventSubQuery.filter(a => a.activityId === activityId && a.userId === userId)
      .joinLeft(PaymentInfoQuery).on(_.paymentInfoId === _.id)

  private def queryTables(activityId: Long, galleryId: Long, language: Language, userId: Long, searchSubs: Boolean) = for {
    gallery <- ImageGalleryQuery.filter(_.id === galleryId).result.head
    galleryImages <- ImageGalleryImageQuery.filter(_.galleryId === gallery.id).result

    audience <- ActivityTeachEventAudienceQuery.filter(_.activityId === activityId).map(_.audience).result

    topics <- ActivityTeachEventTopicQuery.filter(_.activityId === activityId)
      .join(TopicQuery).on(_.topicId === _.id).map(_._2).result

    dates <- ActivityTeachEventDateQuery.filter(_.activityId === activityId).result

    guests <- ActivityTeachEventGuestQuery.filter(_.activityId === activityId)
      .join(ActivityTeachEventGuestTQuery.filter(_.language === language.language))
      .on(_.id === _.guestId).result

    skills <- ActivityTeachEventSkillQuery.filter(_.activityId === activityId)
      .join(SkillQuery).on(_.skillId === _.id).result

    favorite <- ActivityTeachEventFavoriteQuery.filter(a => a.activityId === activityId && a.userId === userId).exists.result

    paymentInfo <- querySubscription(activityId, userId).result.headOption

    subscriptions <- {
      if (searchSubs)
        ActivityTeachEventSubQuery.filter(_.activityId === activityId)
          .map(_.userId).distinct.length.result.map(Some(_))
      else DBIO.successful(None)
    }
  } yield (gallery, galleryImages, audience, topics, dates, guests, skills, favorite, paymentInfo, subscriptions)


  // NOTE: why cannot we use A <: Table[_] and then A#TableElementType?
  private type QueryT[A, AT <: Table[A], B, BT <: Table[B]] = Query[(AT, BT), (A, B), Seq]
  private type QueryActivity = QueryT[DBActivityTeachEvent, ActivityTeachEventTable, DBActivityTeachEventT, ActivityTeachEventTTable]


  private def maybeEnhance(language: Language, userId: Long, q: QueryActivity) = q.result.headOption.flatMap { optActivity =>
    val optionalAction = optActivity.map { case (activity, translation) =>
      for {
        (gallery, galleryImages, audience, topics, dates, guests, skills,
          favorite, paymentInfo, subscriptions) <- queryTables(activity.id, activity.galleryId, language,
                                                               userId, activity.deadline.isDefined)
      } yield (activity, translation, gallery, galleryImages, audience, topics, dates, guests, skills,
                Some(favorite), paymentInfo, subscriptions)
    }

    DBIO.sequenceOption(optionalAction)
  }


  private def find[A](id: Long, language: Language, userId: Long, q: Language => QueryActivity, build: BuildModelFn[A]) = {
    val activityQuery = q(language).filter(_._1.id === id)
    val query = maybeEnhance(language, userId, activityQuery)

    db run query map(_.map(build(language).tupled))
  }


  private def teachQuery(language: Language, future: Boolean) = queryT(language, future).filter(_._1.isTeach)

  override def findTeach(id: Long, language: Language, userId: Long, future: Boolean) =
    find(id, language, userId, l => teachQuery(l, future), dbTeachToTeach)


  private def eventQuery(language: Language, future: Boolean) = queryT(language, future).filterNot(_._1.isTeach)


  override def findEvent(id: Long, language: Language, userId: Long, future: Boolean) =
    find(id, language, userId, l => eventQuery(l, future), dbEventToEvent)


  private def getDBActivity(activity: ActivityTeachEvent[_], galleryId: Long) = {
    val isTeach = activity.isInstanceOf[ActivityTeach]
    val (rDays, rEvery, rEntity, rHours, tDays, tHours) = activity.schedule match {
      case RecurringMeetings(d, ev, en, h) => (Some(d), Some(ev), Some(en: Int), Some(h), None, None)
      case FiniteMeetings(d, h) => (None, None, None, None, Some(d), Some(h))
    }

    DBActivityTeachEvent(
      id = 0,
      isTeach = isTeach,
      coverExt = activity.coverPic.extension,
      galleryId = galleryId,
      level = activity.level.map(l => l: Int),
      activityType = activity.activityType,
      costs = activity.costs,
      payments = activity.payments,
      deadline = activity.optDeadline,
      minParticipants = activity.minParticipants,
      maxParticipants = activity.maxParticipants,
      recurringDays = rDays,
      recurringEvery = rEvery,
      recurringEntity = rEntity,
      recurringHours = rHours,
      totalDays = tDays,
      totalHours = tHours,
      startTime = activity.startTime,
      teachCategory = if (isTeach) Some(activity.asInstanceOf[ActivityTeach].teachCategory: Int) else None,
      bazaarTeachLearnId = if (isTeach) activity.bazaarIdeaId else None,
      bazaarEventId = if (isTeach) None else activity.bazaarIdeaId,
      createdAt = activity.createdAt,
      updatedAt = activity.updatedAt)
  }

  private def updateSkills(activityId: Long, skills: Seq[Skill], required: Boolean = false, acquired: Boolean = false) =
    for {
      _ <- getDeletableIds(skills) match {
        case Seq() => DBIO.successful(())
        case deletableIds => ActivityTeachEventSkillQuery
          .filter(as => as.activityId === activityId && as.skillId.inSet(deletableIds)).delete
      }

      creatable = getCreatable(skills)
      newSkillIds <- (SkillQuery returning SkillQuery.map(_.id)) ++=
        creatable.map(s => DBSkill(0, s.name.toLowerCase, None, request = true))

      existing = getExisting(skills)
      existingLinks <- ActivityTeachEventSkillQuery.filter(_.activityId === activityId).map(_.skillId).result

      // those that are saved and have not been linked + new ones
      creatableWithIds = zipWithIds(creatable, newSkillIds)
      skillsToLink = existing.filterNot(s => existingLinks.contains(s.id)) ++ creatableWithIds
      _ <- ActivityTeachEventSkillQuery ++= skillsToLink.map(s => DBActivityTeachEventSkill(activityId, s.id, required, acquired))
    } yield creatableWithIds ++ existing


  private def updateGuests(activityId: Long, language: Language, guests: Seq[ActivityGuest]) =
    for {
      _ <- getDeletableIds(guests) match {
        case Seq() => DBIO.successful(())
        case deletableIds => for {
          _ <- ActivityTeachEventGuestTQuery.filter(_.guestId inSet deletableIds).delete
          _ <- ActivityTeachEventGuestQuery.filter(_.id inSet deletableIds).delete
        } yield ()
      }

      creatable = getCreatable(guests)
      guestIds <- (ActivityTeachEventGuestQuery returning ActivityTeachEventGuestQuery.map(_.id)) ++= creatable.map { guest =>
        DBActivityTeachEventGuest(0, activityId, guest.userId, guest.firstName, guest.lastName)
      }
      newWithId = zipWithIds(creatable, guestIds)
      _ <- ActivityTeachEventGuestTQuery ++= newWithId.map { guest =>
        DBActivityTeachEventGuestT(guest.id, language.language, guest.title, guest.bio)
      }

      existing = getExisting(guests)
      _ <- DBIO.sequence {
        existing.map { guest =>
          for {
            _ <- ActivityTeachEventGuestQuery.filter(_.id === guest.id)
              .update(DBActivityTeachEventGuest(guest.id, activityId, guest.userId, guest.firstName, guest.lastName))
            _ <- ActivityTeachEventGuestTQuery
              .insertOrUpdate(DBActivityTeachEventGuestT(guest.id, language.language, guest.title, guest.bio))
          } yield ()
        }
      }
    } yield existing ++ newWithId


  private def updateDates(activityId: Long, dates: Seq[SOSDate]) =
    for {
      _ <- getDeletableIds(dates) match {
        case Seq() => DBIO.successful(())
        case deletableIds => ActivityTeachEventDateQuery.filter(_.id inSet deletableIds).delete
      }

      creatable = getCreatable(dates)
      newIds <- (ActivityTeachEventDateQuery returning ActivityTeachEventDateQuery.map(_.id)) ++= creatable.map { date =>
        DBActivityTeachEventDate(0, activityId, date.date, date.startTime, date.endTime)
      }

      existing = getExisting(dates)
      _ <- DBIO.sequence {
        existing.map { date =>
          ActivityTeachEventDateQuery.filter(_.id === date.id)
            .update(DBActivityTeachEventDate(date.id, activityId, date.date, date.startTime, date.endTime))
        }
      }
    } yield existing ++ zipWithIds(creatable, newIds)


  private def updateAudience(activityId: Long, audience: Seq[Audience]) =
    for {
      existingInt <- ActivityTeachEventAudienceQuery.filter(_.activityId === activityId).map(_.audience).result
      existingIntS = existingInt.toSet
      audienceIntS = audience.map(a => a: Int).toSet

      // remove existing that are not in audience
      toDeleteIntS = existingIntS -- audienceIntS
      _ <- ActivityTeachEventAudienceQuery.filter(a => a.activityId === activityId && a.audience.inSet(toDeleteIntS)).delete

      // add those that are not in existing \ deleted
      toAdd = audienceIntS -- (existingIntS -- toDeleteIntS)
      _ <- ActivityTeachEventAudienceQuery ++= toAdd.map((activityId, _))
    } yield ()


  override def create[A <: ActivityTeachEvent[A]](activity: A) = {
    val now = currentTimestamp()
    val activityWithTime = activity.updateTeachEvent(createdAt = now, updatedAt = now)

    val action = for {
      gallery <- {
        if (activity.gallery.id == 0) {
          for {
            gId <- (ImageGalleryQuery returning ImageGalleryQuery.map(_.id)) += DBImageGallery(0, activity.gallery.name)
            images <- updateGalleryImages(gId, activity.gallery.images)
          } yield activity.gallery.copy(id = gId, images = images)
        } else {
          for {
            images <- updateGalleryImages(activity.gallery.id, activity.gallery.images)
          } yield  activity.gallery.copy(images = images)
        }
      }

      activityId <- (ActivityTeachEventQuery returning ActivityTeachEventQuery.map(_.id)) +=
        getDBActivity(activityWithTime, gallery.id)
      _ <- ActivityTeachEventTQuery += getDBActivityT(activity, activityId)

      reqSkills <- updateSkills(activityId, activity.requiredSkills, required = true)
      acqSkills <- updateSkills(activityId, activity.acquiredSkills, acquired = true)
      guests <- updateGuests(activityId, activity.language, activity.guests)
      dates <- updateDates(activityId, activity.dates)
      _ <- updateAudience(activityId, activity.audience)
      topics <- updateTopics(activityId, activity.topics)

      _ <- activity.bazaarIdeaId match {
        case Some(ideaId) =>
          activity match {
            case _: ActivityEvent =>
              BazaarEventQueryAll.filter(_.id === ideaId).map(_.disabled).update(true)
            case _: ActivityTeach =>
              BazaarTeachLearnQuery.filter(_.id === ideaId).map(_.disabled).update(true)
          }
        case None =>
          DBIO.successful(0)
      }
    } yield (activityId, gallery, reqSkills, acqSkills, guests, dates, topics)

    db run action.transactionally map {
      case (activityId, gallery, reqSkills, acqSkills, guests, dates, topics) =>
        activityWithTime.updateTeachEvent(
          id = activityId,
          gallery = gallery,
          requiredSkills = reqSkills,
          acquiredSkills = acqSkills,
          guests = guests,
          dates = dates,
          topics = topics
        )
    }
  }


  override def update[A <: ActivityTeachEvent[A]](activity: A) = {
    val activityWithTime = activity.updateTeachEvent(updatedAt = currentTimestamp())
    val dbActivity = getDBActivity(activityWithTime, activity.gallery.id).copy(id = activity.id)
    val dbActivityT = getDBActivityT(activity, activity.id)

    val action = for {
      exists <- ActivityTeachEventQuery.filter(_.id === activity.id).exists.result

      optTuple <- {
        if (exists)
          for {
            _ <- ActivityTeachEventQuery.filter(_.id === activity.id).map(_.updatableFields).update(dbActivity.updatableFields)
            _ <- ActivityTeachEventTQuery.insertOrUpdate(dbActivityT)

            gallery <- for {
              images <- updateGalleryImages(activity.gallery.id, activity.gallery.images)
            } yield activity.gallery.copy(images = images)

            reqSkills <- updateSkills(activity.id, activity.requiredSkills, required = true)
            acqSkills <- updateSkills(activity.id, activity.acquiredSkills, acquired = true)
            guests <- updateGuests(activity.id, activity.language, activity.guests)
            dates <- updateDates(activity.id, activity.dates)
            _ <- updateAudience(activity.id, activity.audience)
            topics <- updateTopics(activity.id, activity.topics)
          } yield Some((gallery, reqSkills, acqSkills, guests, dates, topics))
        else
          DBIO.successful(None)
      }

    } yield optTuple

    db run action.transactionally map (_.map {
      case (gallery, reqSkills, acqSkills, guests, dates, topics) =>
        activityWithTime.updateTeachEvent(
          gallery = gallery,
          requiredSkills = reqSkills,
          acquiredSkills = acqSkills,
          guests = guests,
          dates = dates,
          topics = topics
        )
    })
  }


  override def delete(id: Long) = {
    val action = for {
      _ <- ActivityTeachEventFavoriteQuery.filter(_.activityId === id).delete

      subsQ = ActivityTeachEventSubQuery.filter(_.activityId === id)
      paymentInfoIds <- subsQ.map(_.paymentInfoId).result
      _ <- subsQ.delete
      _ <- PaymentInfoQuery.filter(_.id inSet paymentInfoIds.flatten).delete

      _ <- ActivityTeachEventSkillQuery.filter(_.activityId === id).delete

      guestsQ = ActivityTeachEventGuestQuery.filter(_.activityId === id)
      _ <- ActivityTeachEventGuestTQuery.filter(_.guestId in guestsQ.map(_.id)).delete
      _ <- guestsQ.delete

      _ <- ActivityTeachEventDateQuery.filter(_.activityId === id).delete
      _ <- ActivityTeachEventTopicQuery.filter(_.activityId === id).delete
      _ <- ActivityTeachEventAudienceQuery.filter(_.activityId === id).delete
      _ <- ActivityTeachEventTQuery.filter(_.activityId === id).delete
      r <- ActivityTeachEventQuery.filter(_.id === id).delete
    } yield r == 1

    db run action.transactionally
  }


  override def getCoverPic(id: Long) = {
    val query = ActivityTeachEventQuery.filter(_.id === id).map(a => (a.isTeach, a.coverExt))
    db run query.result.headOption
  }


  override def favorites(userId: Long, language: Language) = {
    val query = for {
      activityIds <- ActivityTeachEventFavoriteQuery.filter(_.userId === userId).map(_.activityId).result
      r <- queryAll(language, userId, fromAdmin = false, activityIds)
    } yield r

    db run query map composeAll(language)
  }


  override def findSubscription(id: Long, userId: Long) =
    db run querySubscription(id, userId).result.headOption map(_.map(getActivitySub))


  override def subscribe(id: Long, userId: Long, paymentInfo: Option[PaymentInfo]) = {
    val action = for {
      optSub <- querySubscription(id, userId).result.headOption
      activitySub <- optSub match {
        case Some(dbSubPaymentInfo) => DBIO.successful(getActivitySub(dbSubPaymentInfo))
        case None => for {
          piId <- paymentInfo match {
            case Some(pi) => ((PaymentInfoQuery returning PaymentInfoQuery.map(_.id)) += pi).map(Some(_))
            case None => DBIO.successful(None)
          }
          createdAt = currentTimestamp()
          _ <- ActivityTeachEventSubQuery += DBActivityTeachEventSub(id, userId, piId, createdAt)
        } yield ActivitySubscription(
          createdAt = createdAt,
          paymentMethod = paymentInfo.map(_.paymentMethod),
          verified = paymentInfo.flatMap(_.verified),
          cro = paymentInfo.flatMap(_.cro),
          transactionId = paymentInfo.flatMap(_.transactionId),
          amount = paymentInfo.map(_.amount)
        )
      }
    } yield activitySub

    db run action.transactionally
  }

  override def deleteSubscription(id: Long, userId: Long) = {
    val query = ActivityTeachEventSubQuery.filter(a => a.activityId === id && a.userId === userId)

    val action = for {
      paymentInfoIds <- query.map(_.paymentInfoId).result
      n <- query.delete
      _ <- PaymentInfoQuery.filter(_.id inSet paymentInfoIds.flatten).delete
    } yield n == 1

    db run action.transactionally
  }

  private def adminActivitySubQuery(activityId: Long, optUserId: Option[Long] = None) = {
    val q = ActivityTeachEventSubQuery.filter(_.activityId === activityId)
    val qu = optUserId match {
      case Some(userId) => q.filter(_.userId === userId)
      case None => q
    }
    qu.join(UsersQuery).on(_.userId === _.id)
      .joinLeft(PaymentInfoQuery).on(_._1.paymentInfoId === _.id)
  }

  private def buildAdminActivitySub(tpl: ((DBActivityTeachEventSub, DBUser), Option[DBPaymentInfo])) = tpl match {
    case ((sub, user), optPi) => AdminActivitySubscription(
      user = user.toShort,
      subscription = getActivitySub((sub, optPi))
    )
  }

  override def subscriptions(id: Long) = {
    db run (for {
      joined <- adminActivitySubQuery(id).result
    } yield joined map buildAdminActivitySub)
  }

  override def verifySubscription(id: Long, userId: Long, success: Boolean) = {
    val action = for {
      _ <- PaymentInfoQuery.filter(_.id in {
        ActivityTeachEventSubQuery.filter(a => a.activityId === id && a.userId === userId).map(_.paymentInfoId)
      }).map(_.verified).update(Some(success))
      joined <- adminActivitySubQuery(id, Some(userId)).result.head
    } yield buildAdminActivitySub(joined)

    db run action.transactionally
  }

}
