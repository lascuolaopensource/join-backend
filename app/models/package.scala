import java.sql.{Date, Time, Timestamp}
import java.util.{Calendar, Locale, UUID}

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import org.joda.time.DateTime
import play.api.i18n.Lang

import scala.concurrent.duration._
import scala.language.implicitConversions


package object models {

  sealed trait Deletable {
    protected def delete: Option[Boolean]
    val toDelete: Boolean = delete.isDefined && delete.get
  }

  sealed trait Favorable {
    def favorite: Option[Boolean]
    final def favorable: Boolean = favorite.isDefined && favorite.get
  }


  sealed trait Language {
    def lang: Lang
    final def language: String = lang.language
    override def toString: String = language
  }

  final object Language {
    def unapply(arg: Language): Option[String] = Some(arg.language)

    implicit def stringLanguage(s: String) = new Language {
      override def lang: Lang = Lang(s)
    }

    implicit def localeLanguage(locale: Locale) = new Language {
      override def lang: Lang = Lang(locale)
    }

    implicit def langLanguage(playLang: Lang) = new Language {
      override def lang: Lang = playLang
    }
  }

  private def langUnapply(language: Language)(that: Language): Option[String] =
    if (language.language == that.language) Some(language.language) else None

  final object Italian extends Language {
    override def lang: Lang = Lang(Locale.ITALIAN)
    def unapply = langUnapply(this) _
  }

  final object English extends Language {
    override def lang: Lang = Lang(Locale.ENGLISH)
    def unapply = langUnapply(this) _
  }


  val dummyTimestamp = new Timestamp(System.currentTimeMillis())


  sealed trait UserRole
  case object NormalRole extends UserRole
  case object AdminRole extends UserRole


  case class MembershipType(
    id: Long,
    language: Language,
    name: String,
    offer: String,
    bottom: String,
    price: Double,
    position: Int,
    createdAt: Timestamp)

  case class MembershipTypeSlim(id: Long, name: Option[String])

  case class Membership(
    id: Long,
    membershipType: MembershipTypeSlim,
    requestedAt: Timestamp,
    acceptedAt: Option[Timestamp],
    startsAt: Option[Timestamp],
    endsAt: Option[Timestamp],
    userId: Long)

  case class MembershipRequest(
    id: Long,
    membershipType: MembershipTypeSlim,
    user: UserShort,
    requestedAt: Timestamp)

  case class MembershipStatCount(
    membershipType: MembershipTypeSlim,
    count: Int)


  private def currentTimestamp = new Timestamp(System.currentTimeMillis())


  case class UserMemberships(
    active: Option[Membership],
    renewal: Option[Membership],
    request: Option[Membership])

  object UserMemberships {
    def empty: UserMemberships = UserMemberships(None, None, None)
  }

  sealed trait City {
    def cityOpt: Option[String]
    def cityOtherOpt: Option[Boolean]
  }
  final case class GivenCity(city: String) extends City {
    override val cityOpt = Some(city)
    override val cityOtherOpt = Some(false)
  }
  final case object OtherCity extends City {
    override val cityOpt = None
    override val cityOtherOpt = Some(true)
  }

  case class User(
    id: Long,
    email: String,
    emailConfirmed: Boolean,
    firstName: String,
    lastName: String,
    loginInfo: Seq[LoginInfo],
    createdAt: Timestamp = currentTimestamp,
    skills: Seq[UserSkill] = Seq(),
    userRole: UserRole = NormalRole,
    preferredLang: Language = Italian,
    agreement: Boolean = true,
    telephone: Option[String] = None,
    bio: Option[String] = None,
    memberships: UserMemberships = UserMemberships.empty,
    title: Option[String] = None,
    city: Option[City] = None,
    favorite: Option[Boolean] = None,
    ideasCount: Option[Int] = None,
    dummy: Boolean = false
  ) extends Identity with Favorable
  {
    val isAdmin: Boolean = userRole == AdminRole
    val isUser: Boolean = !isAdmin

    def toShort = UserShort(id, email, firstName, lastName)
  }

  final case class UserShort(
    id: Long,
    email: String,
    firstName: String,
    lastName: String
  )

  final case class UserContacts(
    id: Long,
    email: String,
    telephone: Option[String],
    firstName: String,
    lastName: String)


  case class Skill(
    id: Long,
    name: String,
    parentId: Option[Long] = None,
    request: Boolean = false,
    path: Option[Seq[Skill]] = None,
    delete: Option[Boolean] = None
  ) extends Deletable

  case class SkillAdmin(
    id: Long,
    name: String,
    parent: Option[SlimSkill],
    request: Boolean,
    usersCount: Int)

  case class SlimSkill(id: Long, name: String)

  case class SkillTopStat(
    id: Long,
    name: String,
    count: Int,
    trend: StatTrend)


  case class SkillWithLinked(skill: Skill, users: Seq[UserShort])


  case class UserSkill(
    id: Long,
    name: String,
    skillId: Long,
    userId: Long,
    request: Boolean,
    createdAt: Timestamp = currentTimestamp)


  case class Topic(
    id: Long,
    topic: String,
    delete: Option[Boolean] = None
  ) extends Deletable


  sealed trait SOSLevel
  final case object EntryLevel extends SOSLevel
  final case object IntermediateLevel extends SOSLevel
  final case object AdvancedLevel extends SOSLevel


  sealed trait Audience
  final case object Kids extends Audience
  final case object Teenagers extends Audience
  final case object Students extends Audience
  final case object Researchers extends Audience
  final case object Professionals extends Audience
  final case object Companies extends Audience
  final case object PublicAdministrations extends Audience
  final case object Seniors extends Audience
  final case object Immigrants extends Audience
  final case object Unemployed extends Audience


  sealed trait ActivityType

  sealed trait TeachActivityType extends ActivityType
  final case object LecturePerformance extends TeachActivityType
  final case object LabsSeminary extends TeachActivityType
  final case object XYZFormat extends TeachActivityType
  final case object CulturalEvent extends TeachActivityType
  final case object VerticalFormat extends TeachActivityType
  final case object MachineUsage extends TeachActivityType

  sealed trait EventActivityType extends ActivityType
  final case object Talk extends EventActivityType
  final case object Projection extends EventActivityType
  final case object Exposition extends EventActivityType
  final case object Workshop extends EventActivityType
  final case object Performance extends EventActivityType


  case class SOSDate(
    id: Long,
    date: Date,
    startTime: Time,
    endTime: Time,
    delete: Option[Boolean] = None
  ) extends Deletable with Ordered[SOSDate] {
    override def compare(that: SOSDate): Int = {
      val dt1 = date.getTime + startTime.getTime
      val dt2 = that.date.getTime + that.startTime.getTime
      dt1.compare(dt2)
    }
  }


  sealed trait BazaarIdeaMeetingsType
  sealed trait ActivitySchedule

  final case class SingleFixedDaysMeetings(
    id: Long, numberDays: Int, numberHours: Int, delete: Option[Boolean] = None
  ) extends Deletable {
    def totalHours = numberDays * numberHours
  }

  final case class FixedDaysMeetings(schedules: Seq[SingleFixedDaysMeetings]) extends BazaarIdeaMeetingsType {
    def totalDays = schedules.foldLeft(0)(_ + _.numberDays)
    def totalHours = schedules.foldLeft(0)(_ + _.totalHours)
  }

  sealed trait RecurringEntity
  final case object Weekly extends RecurringEntity
  final case object Monthly extends RecurringEntity
  final case object Yearly extends RecurringEntity

  final case class RecurringMeetings(
    days: Int,
    every: Int,
    entity: RecurringEntity,
    hours: Int
  ) extends BazaarIdeaMeetingsType
    with ActivitySchedule

  final case class FiniteMeetings(
    totalDays: Int,
    totalHours: Int
  ) extends ActivitySchedule


  sealed trait BazaarIdeaLocation
  final case object AtSOS extends BazaarIdeaLocation
  final case class CustomLocation(location: String) extends BazaarIdeaLocation

  sealed trait BazaarIdeaFunding
  final case object TuitionFee extends BazaarIdeaFunding
  final case object Sponsor extends BazaarIdeaFunding
  final case object Grant extends BazaarIdeaFunding
  final case object Crowdfunding extends BazaarIdeaFunding
  final case object SelfFinanced extends BazaarIdeaFunding

  case class BazaarIdeaGuest(
    id: Long,
    userId: Option[Long],
    firstName: String,
    lastName: String,
    title: String,
    delete: Option[Boolean] = None
  ) extends Deletable


  sealed trait BazaarIdea extends Favorable {
    def id: Long
    def title: String
    def creator: User
    def topics: Seq[Topic]
    def valueDetails: String
    def motivation: String
    def score: Option[Double]
    def preference: Option[BazaarPreference]
    def counts: Option[BazaarPreferenceCounts]
    def activityId: Option[Long]
    def createdAt: Timestamp
    def updatedAt: Timestamp
    override def favorite = preference.map(_.favorite)
    def isPastDeadline(defaultDeadline: Int): Boolean = {
      val deadline = this match {
        case r: BazaarResearch => r.deadline
        case _ => defaultDeadline
      }
      new org.joda.time.DateTime(createdAt).plusDays(deadline).isBeforeNow
    }
  }

  case class BazaarIdeaMini(
    id: Long,
    title: String,
    ideaType: BazaarIdeaType)

  case class BazaarIdeaSlim(
    id: Long,
    title: String,
    creator: UserShort,
    topics: Seq[Topic],
    preference: Option[BazaarPreferenceSlim],
    createdAt: Timestamp,
    updatedAt: Timestamp,
    ideaType: BazaarIdeaType,
    score: Option[Double] = None,
    counts: Option[BazaarPreferenceCounts] = None,
    deadline: Option[Int] = None,
    skills: Seq[SlimSkill] = Seq(),
    activityId: Option[Long] = None)
  {
    def isPastDeadline(defaultDeadline: Int): Boolean =
      new org.joda.time.DateTime(createdAt).plusDays(deadline.getOrElse(defaultDeadline)).isBeforeNow
  }

  sealed trait BazaarIdeaType
  final case object BazaarLearnType extends BazaarIdeaType
  final case object BazaarTeachType extends BazaarIdeaType
  final case object BazaarEventType extends BazaarIdeaType
  final case object BazaarResearchType extends BazaarIdeaType

  case class BazaarLearn(
    id: Long,
    title: String,
    creator: User,
    topics: Seq[Topic],
    valueDetails: String,
    motivation: String,
    location: BazaarIdeaLocation,
    teachers: Seq[BazaarIdeaGuest],
    tutors: Seq[BazaarIdeaGuest],
    costs: Option[String],
    score: Option[Double] = None,
    preference: Option[BazaarPreference] = None,
    counts: Option[BazaarPreferenceCounts] = None,
    activityId: Option[Long] = None,
    createdAt: Timestamp = currentTimestamp,
    updatedAt: Timestamp = currentTimestamp
  ) extends BazaarIdea

  case class BazaarTeach(
    id: Long,
    title: String,
    creator: User,
    topics: Seq[Topic],
    activityType: TeachActivityType,
    audience: Seq[Audience],
    meetings: BazaarIdeaMeetingsType,
    dates: Seq[SOSDate],
    maxParticipants: Int,
    programDetails: String,
    meetingDetails: String,
    outputDetails: String,
    valueDetails: String,
    motivation: String,
    location: BazaarIdeaLocation,
    teachers: Seq[BazaarIdeaGuest],
    tutors: Seq[BazaarIdeaGuest],
    level: SOSLevel,
    requiredResources: Option[String],
    funding: Seq[BazaarIdeaFunding],
    costs: Option[String],
    score: Option[Double] = None,
    preference: Option[BazaarPreference] = None,
    counts: Option[BazaarPreferenceCounts] = None,
    activityId: Option[Long] = None,
    createdAt: Timestamp = currentTimestamp,
    updatedAt: Timestamp = currentTimestamp
  ) extends BazaarIdea

  object BazaarTeach {
    def apply(learn: BazaarLearn)(
      activityType: TeachActivityType,
      audience: Seq[Audience],
      meetings: BazaarIdeaMeetingsType,
      dates: Seq[SOSDate],
      maxParticipants: Int,
      programDetails: String,
      meetingDetails: String,
      outputDetails: String,
      level: SOSLevel,
      requiredResources: Option[String],
      funding: Seq[BazaarIdeaFunding]
    ): BazaarTeach = BazaarTeach(
      id = learn.id,
      title = learn.title,
      creator = learn.creator,
      topics = learn.topics,
      activityType = activityType,
      audience = audience,
      meetings = meetings,
      dates = dates,
      maxParticipants = maxParticipants,
      programDetails = programDetails,
      meetingDetails = meetingDetails,
      outputDetails = outputDetails,
      valueDetails = learn.valueDetails,
      motivation = learn.motivation,
      location = learn.location,
      teachers = learn.teachers,
      tutors = learn.tutors,
      level = level,
      requiredResources = requiredResources,
      funding = funding,
      costs = learn.costs,
      score = learn.score,
      counts = learn.counts,
      preference = learn.preference,
      activityId = learn.activityId,
      createdAt = learn.createdAt,
      updatedAt = learn.updatedAt)
  }

  case class BazaarEvent(
    id: Long,
    title: String,
    creator: User,
    topics: Seq[Topic],
    activityType: EventActivityType,
    audience: Seq[Audience],
    meetings: BazaarIdeaMeetingsType,
    dates: Seq[SOSDate],
    maxParticipants: Int,
    programDetails: String,
    valueDetails: String,
    motivation: String,
    requiredResources: Option[String],
    requiredSpaces: Option[String],
    funding: Seq[BazaarIdeaFunding],
    isOrganizer: Boolean,
    guests: Seq[BazaarIdeaGuest],
    bookingRequired: Boolean,
    score: Option[Double] = None,
    preference: Option[BazaarPreference] = None,
    counts: Option[BazaarPreferenceCounts] = None,
    activityId: Option[Long] = None,
    createdAt: Timestamp = currentTimestamp,
    updatedAt: Timestamp = currentTimestamp
  ) extends BazaarIdea


  case class BazaarResearchRole(
    id: Long,
    people: Int,
    skills: Seq[Skill],
    delete: Option[Boolean] = None
  ) extends Deletable

  case class BazaarResearch(
    id: Long,
    title: String,
    creator: User,
    topics: Seq[Topic],
    organizationName: Option[String],
    valueDetails: String,
    motivation: String,
    requiredResources: String,
    positions: Seq[BazaarResearchRole],
    deadline: Int,
    duration: Int,
    score: Option[Double] = None,
    preference: Option[BazaarPreference] = None,
    counts: Option[BazaarPreferenceCounts] = None,
    activityId: Option[Long] = None,
    createdAt: Timestamp = currentTimestamp,
    updatedAt: Timestamp = currentTimestamp
  ) extends BazaarIdea


  case class BazaarPreference(
    id: Long,
    userId: Long,
    ideaId: Long,
    ideaType: BazaarIdeaType,
    agree: Boolean,
    wish: Option[BazaarComment],
    favorite: Boolean,
    viewed: Boolean)

  object BazaarPreference {
    def apply(id: Long, userId: Long): BazaarPreference =
      BazaarPreference(id, userId, 0, BazaarLearnType, agree = false, None, favorite = false, viewed = false)
  }

  case class BazaarPreferenceSlim(
    id: Long,
    agree: Boolean,
    wish: Boolean,
    favorite: Boolean,
    viewed: Boolean)

  case class BazaarPreferenceCounts(
    views: Int, agrees: Int, wishes: Int, comments: Int, favorites: Int)


  case class BazaarComment(
    id: Long,
    userId: Long,
    firstName: Option[String],
    lastName: Option[String],
    comment: String,
    createdAt: Option[Timestamp])

  object BazaarComment {
    def apply(id: Long, userId: Long): BazaarComment = BazaarComment(id, userId, None, None, "", None)
  }


  sealed trait ImageWithData {
    def extension: String
    def url: Option[String]
    def data: Option[String]
  }

  case class DataImage(
    extension: String,
    url: Option[String] = None,
    data: Option[String] = None
  ) extends ImageWithData {
    def fromData = DataImage.fromData(data.get)
  }

  object DataImage {
    def fromData(raw: String): Option[DataImage] = {
      raw.split("data:image/").toList match {
        case (_ :: x :: _) => x.split(",") match {
          case Array(fileType, picBase64) => Some(DataImage(fileType.split(";")(0), None, Some(picBase64)))
          case _ => None
        }
        case _ => None
      }
    }
  }


  case class Image(
    id: Long,
    extension: String,
    url: Option[String] = None,
    data: Option[String] = None,
    delete: Option[Boolean] = None
  ) extends ImageWithData
    with Deletable
  {
    val filename: String = s"$id.$extension"
  }

  case class ImageGallery(
    id: Long,
    images: Seq[Image],
    name: String)


  sealed trait TeachCategory
  final object CategoryX extends TeachCategory
  final object CategoryY extends TeachCategory
  final object CategoryZ extends TeachCategory


  case class ActivityGuest(
    id: Long,
    userId: Option[Long],
    firstName: String,
    lastName: String,
    title: String,
    bio: String,
    delete: Option[Boolean] = None
  ) extends Deletable

  sealed trait Activity[A <: Activity[A]] extends Favorable { this: A =>
    def id: Long
    def language: Language
    def title: String
    def coverPic: DataImage
    def gallery: ImageGallery
    def topics: Seq[Topic]
    def description: String
    def optDeadline: Option[Date]
    def bazaarIdeaId: Option[Long]
    def createdAt: Timestamp
    def updatedAt: Timestamp

    def deadlineToCheck: Option[Long] = optDeadline map { deadline =>
      val cal = Calendar.getInstance()
      cal.setTime(deadline)
      cal.add(Calendar.DATE, 1)
      cal.getTime.getTime
    }

    def updateActivity(
      coverPic: DataImage,
      gallery: ImageGallery
    ): A

    final def get: A = this
  }

  sealed trait ActivitySlim extends Favorable {
    def id: Long
    def title: String
    def topics: Seq[Topic]
    def bazaarIdeaId: Option[Long]
    def participantsCount: Option[Int]
    def minParticipants: Option[Int]
    def totalHours: Option[Int]
    def createdAt: Timestamp
    def updatedAt: Timestamp
  }

  case class ActivityMini(
    id: Long,
    title: String)

  final case class ActivityDeadline(date: Date, closed: Boolean)

  final case class ActivitySubscription(
    createdAt: Timestamp,
    paymentMethod: Option[PaymentMethod] = None,
    verified: Option[Boolean] = None,
    cro: Option[String] = None,
    transactionId: Option[String] = None,
    amount: Option[Double] = None)

  final case class AdminActivitySubscription(
    user: UserShort,
    subscription: ActivitySubscription)

  case class ActivityGuestSlim(
    id: Long,
    firstName: String,
    lastName: String,
    userId: Option[Long])

  sealed trait ActivityTeachEventSlim extends ActivitySlim

  sealed trait ActivityTeachEvent[A <: ActivityTeachEvent[A]] extends Activity[A] { this: A =>
    def level: Option[SOSLevel]
    def audience: Seq[Audience]
    def outputType: String
    def program: String
    def activityType: ActivityType
    def costs: Option[Double]
    def payments: Boolean
    def minParticipants: Option[Int]
    def maxParticipants: Option[Int]
    def schedule: ActivitySchedule
    def dates: Seq[SOSDate]
    def startTime: Option[Timestamp]
    def guests: Seq[ActivityGuest]
    def requiredSkills: Seq[Skill]
    def acquiredSkills: Seq[Skill]

    def subscription: Option[ActivitySubscription]
    def subscriptions: Option[Int]

    def updateTeachEvent(
      id: Long = id,
      coverPic: DataImage = coverPic,
      gallery: ImageGallery = gallery,
      requiredSkills: Seq[Skill] = requiredSkills,
      acquiredSkills: Seq[Skill] = acquiredSkills,
      guests: Seq[ActivityGuest] = guests,
      dates: Seq[SOSDate] = dates,
      topics: Seq[Topic] = topics,
      createdAt: Timestamp = createdAt,
      updatedAt: Timestamp = updatedAt
    ): A

    override final def updateActivity(
      coverPic: DataImage = coverPic,
      gallery: ImageGallery = gallery
    ): A = updateTeachEvent(
      coverPic = coverPic,
      gallery = gallery
    )
  }

  case class ActivityTeach(
    id: Long,
    language: Language,
    title: String,
    coverPic: DataImage,
    gallery: ImageGallery,
    level: Option[SOSLevel],
    audience: Seq[Audience],
    topics: Seq[Topic],
    description: String,
    outputType: String,
    outputDescription: String,
    program: String,
    activityType: TeachActivityType,
    costs: Option[Double],
    payments: Boolean,
    deadline: Date,
    minParticipants: Option[Int],
    maxParticipants: Option[Int],
    schedule: ActivitySchedule,
    dates: Seq[SOSDate],
    startTime: Option[Timestamp],
    teachCategory: TeachCategory,
    guests: Seq[ActivityGuest],
    requiredSkills: Seq[Skill],
    acquiredSkills: Seq[Skill],
    bazaarIdeaId: Option[Long],
    createdAt: Timestamp,
    updatedAt: Timestamp,
    favorite: Option[Boolean] = None,
    subscription: Option[ActivitySubscription] = None,
    subscriptions: Option[Int] = None
  ) extends ActivityTeachEvent[ActivityTeach] {
    override val optDeadline: Option[Date] = Some(deadline)
    override def updateTeachEvent(
      id: Long,
      coverPic: DataImage,
      gallery: ImageGallery,
      reqSkills: Seq[Skill],
      acqSkills: Seq[Skill],
      guests: Seq[ActivityGuest],
      dates: Seq[SOSDate],
      topics: Seq[Topic],
      createdAt: Timestamp,
      updatedAt: Timestamp): ActivityTeach =
      copy(
        id = id,
        coverPic = coverPic,
        gallery = gallery,
        requiredSkills = reqSkills,
        acquiredSkills = acqSkills,
        guests = guests,
        dates = dates,
        topics = topics,
        createdAt = createdAt,
        updatedAt = updatedAt)
  }

  case class ActivityTeachSlim(
    id: Long,
    title: String,
    topics: Seq[Topic],
    deadline: ActivityDeadline,
    startTime: Option[Timestamp],
    guests: Seq[ActivityGuestSlim],
    requiredSkills: Seq[Skill],
    acquiredSkills: Seq[Skill],
    bazaarIdeaId: Option[Long],
    participantsCount: Option[Int],
    minParticipants: Option[Int],
    totalHours: Option[Int],
    createdAt: Timestamp,
    updatedAt: Timestamp,
    favorite: Option[Boolean]
  ) extends ActivityTeachEventSlim

  case class ActivityEvent(
    id: Long,
    language: Language,
    title: String,
    coverPic: DataImage,
    gallery: ImageGallery,
    level: Option[SOSLevel],
    audience: Seq[Audience],
    topics: Seq[Topic],
    description: String,
    outputType: String,
    program: String,
    activityType: EventActivityType,
    costs: Option[Double],
    payments: Boolean,
    deadline: Option[Date],
    minParticipants: Option[Int],
    maxParticipants: Option[Int],
    schedule: ActivitySchedule,
    dates: Seq[SOSDate],
    startTime: Option[Timestamp],
    guests: Seq[ActivityGuest],
    requiredSkills: Seq[Skill],
    acquiredSkills: Seq[Skill],
    bazaarIdeaId: Option[Long],
    createdAt: Timestamp,
    updatedAt: Timestamp,
    favorite: Option[Boolean] = None,
    subscription: Option[ActivitySubscription] = None,
    subscriptions: Option[Int] = None
  ) extends ActivityTeachEvent[ActivityEvent] {
    override val optDeadline: Option[Date] = deadline
    override def updateTeachEvent(
      id: Long,
      coverPic: DataImage,
      gallery: ImageGallery,
      reqSkills: Seq[Skill],
      acqSkills: Seq[Skill],
      guests: Seq[ActivityGuest],
      dates: Seq[SOSDate],
      topics: Seq[Topic],
      createdAt: Timestamp,
      updatedAt: Timestamp
    ): ActivityEvent =
      copy(
        id = id,
        coverPic = coverPic,
        gallery = gallery,
        requiredSkills = reqSkills,
        acquiredSkills = acqSkills,
        guests = guests,
        dates = dates,
        topics = topics,
        createdAt = createdAt,
        updatedAt = updatedAt)
  }

  case class ActivityEventSlim(
    id: Long,
    title: String,
    topics: Seq[Topic],
    deadline: Option[ActivityDeadline],
    startTime: Option[Timestamp],
    guests: Seq[ActivityGuestSlim],
    requiredSkills: Seq[Skill],
    acquiredSkills: Seq[Skill],
    bazaarIdeaId: Option[Long],
    participantsCount: Option[Int],
    minParticipants: Option[Int],
    totalHours: Option[Int],
    createdAt: Timestamp,
    updatedAt: Timestamp,
    favorite: Option[Boolean]
  ) extends ActivityTeachEventSlim


  case class ActivityResearchRole(
    id: Long,
    people: Int,
    skills: Seq[Skill],
    applications: Seq[ActivityResearchAppFull] = Seq(),
    application: Option[ActivityResearchApp] = None,
    delete: Option[Boolean] = None
  ) extends Deletable

  case class ActivityResearchApp(
    userId: Long,
    motivation: Option[String],
    createdAt: Timestamp)

  case class ActivityResearchAppFull(
    user: UserShort,
    motivation: Option[String],
    createdAt: Timestamp)

  case class ActivityResearchTeam(
    id: Long,
    userId: Option[Long],
    firstName: String,
    lastName: String,
    title: String,
    delete: Option[Boolean] = None
  ) extends Deletable

  case class ActivityResearch(
    id: Long,
    language: Language,
    title: String,
    coverPic: DataImage,
    gallery: ImageGallery,
    topics: Seq[Topic],
    organizationName: Option[String],
    motivation: String,
    valueDetails: String,
    deadline: Date,
    startDate: Date,
    duration: Int,
    projectLink: Option[String],
    roles: Seq[ActivityResearchRole],
    team: Seq[ActivityResearchTeam],
    bazaarIdeaId: Option[Long],
    createdAt: Timestamp,
    updatedAt: Timestamp,
    favorite: Option[Boolean] = None,
    userHasAccess: Option[Boolean] = None
  ) extends Activity[ActivityResearch] {
    override val optDeadline: Option[Date] = Some(deadline)
    override val description: String = motivation

    override def updateActivity(
      coverPic: DataImage = coverPic,
      gallery: ImageGallery = gallery
    ): ActivityResearch = copy(
      coverPic = coverPic,
      gallery = gallery
    )
  }

  case class ActivityResearchSlim(
    id: Long,
    title: String,
    topics: Seq[Topic],
    owner: Option[UserShort],
    deadline: ActivityDeadline,
    startDate: Date,
    rolesCount: Int,
    bazaarIdeaId: Option[Long],
    participantsCount: Option[Int],
    skills: Option[Set[SlimSkill]],
    createdAt: Timestamp,
    updatedAt: Timestamp,
    favorite: Option[Boolean]
  ) extends ActivitySlim
  {
    val minParticipants = None
    val totalHours = None
  }


  sealed trait PaymentMethod
  final case object PayPal extends PaymentMethod
  final case object CreditCard extends PaymentMethod
  final case object WireTransfer extends PaymentMethod

  final case class PaymentInfo(
    id: Long,
    paymentMethod: PaymentMethod,
    transactionId: Option[String],
    cro: Option[String],
    verified: Option[Boolean],
    amount: Double,
    paymentNonce: Option[String] = None)

  case class FablabMachine(
    id: Long,
    name: String,
    workArea: Option[String],
    maxHeight: Option[String],
    cutsMetal: Option[Boolean],
    cutsNonMetal: Option[Boolean],
    cutsMaterials: Option[String],
    engravesMetal: Option[Boolean],
    engravesNonMetal: Option[Boolean],
    engravesMaterials: Option[String],
    priceHour: Double,
    operator: Boolean,
    operatorCost: Option[Double],
    createdAt: Timestamp)

  case class FablabReservationTime(
    date: Date,
    hour: Int)

  case class FablabReservation(
    id: Long,
    user: UserShort,
    machine: SlimMachine,
    times: Seq[FablabReservationTime],
    operator: Boolean,
    createdAt: Timestamp)
  {
    private val sortedTimes = times.sortBy(t => t.date.getTime + t.hour.hours.toMillis)

    val startTime = {
      val m = sortedTimes.head
      new Timestamp(m.date.getTime + m.hour.hours.toMillis)
    }

    val endTime = {
      val m = sortedTimes.last
      new Timestamp(m.date.getTime + m.hour.hours.toMillis)
    }
  }

  case class FablabReservationFlat(
    id: Long,
    user: UserContacts,
    machine: SlimMachine,
    time: FablabReservationTime,
    operator: Boolean)

  case class SlimMachine(id: Long, name: String)

  case class FablabQuotation(
    id: Long,
    userId: Long,
    realizationOf: String,
    machines: Seq[SlimMachine],
    undertaken: Boolean,
    createdAt: Timestamp,
    user: Option[UserContacts] = None)


  sealed trait MailTokenType
  final case object SignUpToken extends MailTokenType
  final case object ChangeEmailToken extends MailTokenType
  final case object ResetPasswordToken extends MailTokenType

  case class MailToken(
    token: String, email: String, userId: Long,
    expiration: Timestamp, tokenType: MailTokenType)
  {
    def isExpired: Boolean = new DateTime(expiration.getTime).isBeforeNow
    lazy val isSignUp = tokenType == SignUpToken
    lazy val isChangeEmail = tokenType == ChangeEmailToken
    lazy val isResetPassword = tokenType == ResetPasswordToken
  }

  object MailToken {
    private def generate(email: String, userId: Long, tokenType: MailTokenType) = MailToken(
      email = email,
      userId = userId,
      token = UUID.randomUUID().toString,
      expiration = new Timestamp(new DateTime().plusHours(24).getMillis),
      tokenType = tokenType)

    def signUp(user: User) = generate(user.email, user.id, SignUpToken)
    def changeEmail(email: String, userId: Long) = generate(email, userId, ChangeEmailToken)
    def resetPassword(user: User) = generate(user.email, user.id, ResetPasswordToken)
  }


  sealed trait StatTrend
  final case object UpTrend extends StatTrend
  final case object DownTrend extends StatTrend
  final case object EqTrend extends StatTrend

  case class BazaarIdeaStatRow(
    ideaId: Long,
    title: String,
    score: Double,
    createdAt: Timestamp,
    userId: Long,
    firstName: String,
    lastName: String,
    ideaType: BazaarIdeaType,
    trend: Option[StatTrend])

  case class BazaarIdeasCount(
    teaching: Long,
    event: Long,
    research: Long)
  {
    val total = teaching + event + research
  }

  case class BazaarTopCreator(
    id: Long,
    firstName: String,
    lastName: String,
    average: Double,
    ideasCount: Long,
    trend: Option[StatTrend])

  sealed trait ActivityClass
  final case object ActivityEventClass extends ActivityClass
  final case object ActivityTeachClass extends ActivityClass
  final case object ActivityResearchClass extends ActivityClass

  case class ActivityStat(
    id: Long,
    title: String,
    subCount: Long,
    startDate: Date,
    activityClass: ActivityClass,
    trend: Option[StatTrend])

  case class ActivityStatCount(
    programmed: Int,
    teaching: Int,
    event: Int,
    research: Int,
    ideasCount: Int)
  {
    val done = teaching + event + research
    val success = if (ideasCount == 0) 0 else done / ideasCount.asInstanceOf[Double]
  }

  case class ActivityProjectStat(
    id: Long,
    title: String,
    subCount: Long,
    owner: Option[UserShort],
    trend: StatTrend)

  case class ActivityProjectStatCount(
    ongoing: Int,
    ideas: Int,
    users: Int)

  case class FablabMachineStat(
    id: Long,
    name: String,
    usage: Int,
    trend: StatTrend)

  case class FablabStatCount(
    quotations: Int,
    reservations: Int,
    machines: Int)

  case class UserStat(
    id: Long,
    firstName: String,
    lastName: String,
    quantity: Int,
    trend: StatTrend)

}
