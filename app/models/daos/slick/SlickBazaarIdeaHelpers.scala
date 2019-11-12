package models.daos.slick

import models._
import models.daos.slick.tables.SlickBazaarTeachLearnTable.DBBazaarTeachLearnS
import models.daos.slick.tables.SlickTopicTable._
import models.daos.slick.tables.SlickUserTable.DBUser

import scala.concurrent.Future


private[slick] trait SlickBazaarIdeaHelpers extends SlickDAO {

  import Implicits.DefaultWithId._
  import profile.api._


  private def ideaIdOnGuestTable(isTeachLearn: Boolean)(t: BazaarIdeaGuestTable) =
    if (isTeachLearn) t.bazaarTeachLearnId else t.bazaarEventId

  protected def guestsQuery(ideaId: Long, tutor: Boolean = false, isTeachLearn: Boolean = false) = {
    def fk = ideaIdOnGuestTable(isTeachLearn)(_)
    BazaarIdeaGuestQuery.filter { t =>
      fk(t) === ideaId && t.tutor === tutor && t.userId.isEmpty
    }
  }

  protected def guestUsersQuery(ideaId: Long, tutor: Boolean = false, isTeachLearn: Boolean = false) = {
    def id = ideaIdOnGuestTable(isTeachLearn)(_)
    BazaarIdeaGuestQuery.join(UsersQuery).on(_.userId === _.id).filter {
      case (t, _) => id(t) === ideaId && t.tutor === tutor && t.userId.isDefined
    }
  }

  protected def transformGuests(extGuests: Seq[DBBazaarIdeaGuest], guests: Seq[(DBBazaarIdeaGuest, DBUser)]) = {
    extGuests.map { guest =>
      BazaarIdeaGuest(guest.id, guest.userId, guest.firstName.get, guest.lastName.get, guest.title.get)
    } ++
      guests.map {
        case (t, u) => BazaarIdeaGuest(t.id, t.userId, u.firstName, u.lastName, u.title.getOrElse(""))
      }
  }

  protected def getIdeaIdOnTopic(ideaType: BazaarIdeaType)(t: BazaarIdeaTopicTable) = ideaType match {
    case BazaarLearnType | BazaarTeachType    => t.bazaarTeachLearnId
    case BazaarEventType                      => t.bazaarEventId
    case BazaarResearchType                   => t.bazaarResearchId
  }

  protected def topicsQuery(ideaId: Long, ideaType: BazaarIdeaType) = TopicQuery.filter {
    _.id in BazaarIdeaTopicQuery.filter(t => {
      getIdeaIdOnTopic(ideaType)(t) === ideaId
    }).map(_.topicId)
  }

  protected def enhanceBazaarLearn(idea: DBBazaarTeachLearnS): Future[BazaarLearn] = {
    val queries = for {
      dbUser <- UsersQuery.filter(_.id === idea.creatorId).result.head
      topics <- topicsQuery(idea.id, BazaarLearnType).result
      extTeachers <- guestsQuery(idea.id, isTeachLearn = true).result
      teachers <- guestUsersQuery(idea.id, isTeachLearn = true).result
      extTutors <- guestsQuery(idea.id, tutor = true, isTeachLearn = true).result
      tutors <- guestUsersQuery(idea.id, tutor = true, isTeachLearn = true).result
      activityId <- ActivityTeachEventQuery.filter(_.bazaarTeachLearnId === idea.id).map(_.id).result.headOption
    } yield (dbUser, topics, extTeachers, teachers, extTutors, tutors, activityId)

    db run queries map {
      case (dbUser, topics, extTeachers, teachers, extTutors, tutors, activityId) =>
        BazaarLearn(
          id = idea.id,
          title = idea.title,
          creator = dbUser,
          topics = topics.map(dbTopicToTopic),
          valueDetails = idea.valueDetails,
          motivation = idea.motivation,
          location = idea.location match {
            case "SOS" => AtSOS
            case custom => CustomLocation(custom)
          },
          teachers = transformGuests(extTeachers, teachers),
          tutors = transformGuests(extTutors, tutors),
          costs = idea.costs,
          activityId = activityId,
          createdAt = idea.createdAt,
          updatedAt = idea.updatedAt,
          score = idea.score,
          counts = getPreferenceCounts(idea.views, idea.agrees, idea.wishes, idea.comments, idea.favorites)
        )
    }
  }

  protected def getMeetings(abstractIdea: DBBazaarAbstractIdea, meetings: Seq[DBBazaarIdeaMeetingDuration]) = {
    if (meetings.isEmpty) {
      RecurringMeetings(
        abstractIdea.recurringDays.get,
        abstractIdea.recurringEvery.get,
        abstractIdea.recurringEntity.get,
        abstractIdea.hoursPerMeeting.get)
    } else {
      FixedDaysMeetings(meetings.map(meeting => SingleFixedDaysMeetings(meeting.id, meeting.numberDays, meeting.numberHours)))
    }
  }

  protected def insertTopics(topics: Seq[Topic], ideaId: Long, ideaType: BazaarIdeaType) = {
    def getDBTopic(topicId: Long): DBBazaarIdeaTopic = ideaType match {
      case BazaarLearnType | BazaarTeachType    => DBBazaarIdeaTopic(0, topicId, None, Some(ideaId), None)
      case BazaarEventType                      => DBBazaarIdeaTopic(0, topicId, Some(ideaId), None, None)
      case BazaarResearchType                   => DBBazaarIdeaTopic(0, topicId, None, None, Some(ideaId))
    }

    val topicsFromDB = getExisting(topics)
    for {
      _ <- BazaarIdeaTopicQuery ++= topicsFromDB.map(t => getDBTopic(t.id))
      newTopics = getCreatable(topics)
      newTopicsId <- (TopicQuery returning TopicQuery.map(_.id)) ++= newTopics.map(t => DBTopic(0, t.topic))
      _ <- BazaarIdeaTopicQuery ++= newTopicsId.map(getDBTopic)
    } yield zipWithIds(newTopics, newTopicsId) ++ topicsFromDB
  }

  protected def insertGuests(guests: Seq[BazaarIdeaGuest], ideaId: Long, isTeachLearn: Boolean, tutor: Boolean = false) =
    for {
      newIds <- (BazaarIdeaGuestQuery returning BazaarIdeaGuestQuery.map(_.id)) ++= guests.map { t =>
        val dbGuest = bazaarGuestToDB(t).copy(tutor = tutor)
        if (isTeachLearn) dbGuest.copy(bazaarTeachLearnId = Some(ideaId))
        else              dbGuest.copy(bazaarEventId = Some(ideaId))
      }
    } yield zipWithIds(guests, newIds)

  protected def getAbstractIdea(meetings: BazaarIdeaMeetingsType, resources: Option[String], maxParticipants: Int, program: String) = {
    val (fixDays, recurringDays, recurringEvery, recurringEntity, recurringHours) = meetings match {
      case RecurringMeetings(days, every, entity, hours) =>
        (None, Some(days), Some(every), Some(entity: Int), Some(hours))
      case meetings: FixedDaysMeetings =>
        (Some(meetings.totalDays), None, None, None, None)
    }

    DBBazaarAbstractIdea(
      id = 0,
      requiredResources = resources,
      maxParticipants = maxParticipants,
      programDetails = program,
      days = fixDays,
      recurringDays = recurringDays,
      recurringEvery = recurringEvery,
      recurringEntity = recurringEntity,
      hoursPerMeeting = recurringHours
    )
  }

  protected val insertAbstractIdea: ((BazaarIdeaMeetingsType, Option[String], Int, String)) => SingleInsertResult[DBBazaarAbstractIdea] =
    (getAbstractIdea _).tupled.andThen(insertDBAbstractIdea)

  protected def insertDBAbstractIdea(abstractIdea: DBBazaarAbstractIdea): SingleInsertResult[DBBazaarAbstractIdea] =
    (BazaarAbstractIdeaQuery returning BazaarAbstractIdeaQuery.map(_.id)) += abstractIdea

  protected def insertAudience(audience: Seq[Audience], abstractIdeaId: Long) = {
    BazaarIdeaAudienceQuery ++= audience.map { audience =>
      DBBazaarIdeaAudience(abstractIdeaId, audience)
    }
  }

  protected def insertMeetings(meetings: BazaarIdeaMeetingsType, abstractIdeaId: Long) = meetings match {
    case meetings: FixedDaysMeetings =>
      // TODO: return new ids
      BazaarIdeaMeetingDurationQuery ++= meetings.asInstanceOf[FixedDaysMeetings].schedules.map { meeting =>
        DBBazaarIdeaMeetingDuration(0, abstractIdeaId, meeting.numberDays, meeting.numberHours)
      }
    case _ => DBIO.successful(())
  }

  protected def insertDates(dates: Seq[SOSDate], abstractIdeaId: Long) =
    // TODO: return new ids
    BazaarIdeaDateQuery ++= dates.map { date =>
      DBBazaarIdeaDate(0, abstractIdeaId, date.date, date.startTime, date.endTime)
    }

  protected def insertFunding(funding: Seq[BazaarIdeaFunding], abstractIdeaId: Long) =
    BazaarIdeaFundingQuery ++= funding.map(_.copy(bazaarAbstractIdeaId = abstractIdeaId))

  protected def updateTopics(topics: Seq[Topic], ideaId: Long, ideaType: BazaarIdeaType) = {
    def id = getIdeaIdOnTopic(ideaType)(_)
    for {
      existingTopicIds <- BazaarIdeaTopicQuery.filter(t => id(t) === ideaId).map(_.topicId).result
      newTopics = topics.filter(t => !t.toDelete && !existingTopicIds.contains(t.id))
      newTopics <- insertTopics(newTopics, ideaId, ideaType)

      delTopicIds = getDeletableIds(topics)
      _ <- BazaarIdeaTopicQuery.filter(t => id(t) === ideaId && (t.topicId inSet delTopicIds)).delete
    } yield newTopics ++ topics.filter(t => !t.toDelete && existingTopicIds.contains(t.id))
  }

  protected def updateGuests(guests: Seq[BazaarIdeaGuest], ideaId: Long, isTeachLearn: Boolean, tutor: Boolean = false) = {
    // TODO: handle update of custom guests data
    val newGuests = getCreatable(guests)
    for {
      newGuests <- insertGuests(newGuests, ideaId, isTeachLearn, tutor)
      delGuestIds = getDeletableIds(guests)
      _ <- BazaarIdeaGuestQuery.filter(t => t.id inSet delGuestIds).delete
    } yield newGuests ++ getExisting(guests)
  }

  protected def updateAbstractIdea(ideaId: Long, abstractIdea: DBBazaarAbstractIdea, isTeach: Boolean) = {
    val query: Query[Rep[Long], Long, Seq] =
      if (isTeach) BazaarTeachLearnQuery.filter(_.id === ideaId).map(_.bazaarAbstractIdeaId.get)
      else         BazaarEventQuery.filter(_.id === ideaId).map(_.bazaarAbstractIdeaId)

    for {
      abstractIdeaIdOpt <- query.result.headOption
      abstractIdeaId = abstractIdeaIdOpt.get
      _ <- BazaarAbstractIdeaQuery.filter(_.id === abstractIdeaId).update(abstractIdea.copy(id = abstractIdeaId))
    } yield abstractIdeaId
  }

  protected def updateAudience(audience: Seq[Audience], abstractIdeaId: Long) = {
    for {
      existingAudienceDB <- BazaarIdeaAudienceQuery.filter(_.bazaarAbstractIdeaId === abstractIdeaId).result
      audienceDB = audience.map(DBBazaarIdeaAudience(abstractIdeaId, _))
      newAudience = audienceDB.filterNot(existingAudienceDB.contains(_))
      _ <- BazaarIdeaAudienceQuery ++= newAudience

      _ <- DBIO sequence existingAudienceDB.filterNot(audienceDB.contains(_)).map { audience =>
        BazaarIdeaAudienceQuery.filter { a =>
          a.bazaarAbstractIdeaId === abstractIdeaId && a.audienceType === audience.audienceType
        }.delete
      }
    } yield audience
  }

  protected def updateMeetings(meetings: BazaarIdeaMeetingsType, abstractIdeaId: Long) = meetings match {
    case meetings: FixedDaysMeetings =>
      def getScheduleDB(schedule: SingleFixedDaysMeetings) =
        DBBazaarIdeaMeetingDuration(schedule.id, abstractIdeaId, schedule.numberDays, schedule.numberHours)
      val newSchedules = getCreatable(meetings.schedules)
      val updateSchedules = getExisting(meetings.schedules)
      for {
        newIds <- (BazaarIdeaMeetingDurationQuery returning BazaarIdeaMeetingDurationQuery.map(_.id)) ++= (newSchedules map getScheduleDB)
        _ <- DBIO sequence updateSchedules.map { schedule =>
          BazaarIdeaMeetingDurationQuery.filter(_.id === schedule.id).update(getScheduleDB(schedule))
        }
        delIds = getDeletableIds(meetings.schedules)
        _ <- BazaarIdeaMeetingDurationQuery.filter(_.id inSet delIds).delete
      } yield FixedDaysMeetings(zipWithIds(newSchedules, newIds) ++ updateSchedules)
    case _ =>
      for {
        c <- BazaarIdeaMeetingDurationQuery.filter(_.bazaarAbstractIdeaId === abstractIdeaId).map(_.id).length.result
        _ <- if (c > 0) BazaarIdeaMeetingDurationQuery.filter(_.bazaarAbstractIdeaId === abstractIdeaId).delete
             else DBIO.successful(())
      } yield meetings
  }

  protected def updateDates(dates: Seq[SOSDate], abstractIdeaId: Long) = {
    def getDateDB(date: SOSDate) = DBBazaarIdeaDate(date.id, abstractIdeaId, date.date, date.startTime, date.endTime)
    val newDates = getCreatable(dates)
    val existingDates = getExisting(dates)
    for {
      newIds <- (BazaarIdeaDateQuery returning BazaarIdeaDateQuery.map(_.id)) ++= (newDates map getDateDB)
      _ <- DBIO sequence existingDates.map { date =>
        BazaarIdeaDateQuery.filter(_.id === date.id).update(getDateDB(date))
      }
      delIds = getDeletableIds(dates)
      _ <- BazaarIdeaDateQuery.filter(_.id inSet delIds).delete
    } yield zipWithIds(newDates, newIds) ++ existingDates
  }

  protected def updateFunding(funding: Seq[BazaarIdeaFunding], abstractIdeaId: Long) = {
    for {
      existingFundingDB <- BazaarIdeaFundingQuery.filter(_.bazaarAbstractIdeaId === abstractIdeaId).result
      fundingDB = funding.map(_.copy(bazaarAbstractIdeaId = abstractIdeaId))
      newFunding = fundingDB.filterNot(existingFundingDB.contains(_))
      _ <- BazaarIdeaFundingQuery ++= newFunding
      _ <- DBIO sequence existingFundingDB.filterNot(fundingDB.contains(_)).map { funding =>
        BazaarIdeaFundingQuery.filter { f =>
          f.bazaarAbstractIdeaId === abstractIdeaId && f.fundingType === funding.fundingType
        }.delete
      }
    } yield funding
  }

  protected def getPreferenceCounts(
    views: Option[Int],
    agrees: Option[Int],
    wishes: Option[Int],
    favorites: Option[Int],
    comments: Option[Int]) =
    (views, agrees, wishes, comments, favorites) match {
      case (Some(v), Some(a), Some(w), Some(c), Some(f)) =>
        Some(BazaarPreferenceCounts(v, a, w, c, f))
      case _ => None
    }

}
