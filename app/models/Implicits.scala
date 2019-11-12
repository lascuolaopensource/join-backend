package models

import java.sql
import java.sql.Timestamp
import java.text.SimpleDateFormat

import play.api.libs.functional.syntax._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.collection.mutable


object Implicits {

  implicit val sqlTimeReads: Reads[sql.Time] =
    JsPath.read[String].map(s => sql.Time.valueOf(if (s.length == 5) s"$s:00" else s))

  private val jsDateTimeZoneFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
  private val jsDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm")

  implicit val sqlTimestampReads: Reads[sql.Timestamp] = JsPath.read[String].map { s =>
    val parsedDate = try {
      jsDateTimeZoneFormat.parse(s)
    } catch {
      case _: java.text.ParseException =>
        jsDateTimeFormat.parse(s)
    }
    new Timestamp(parsedDate.getTime)
  }


  implicit val statTrendWrites = Writes[StatTrend] {
    case UpTrend => JsString("up")
    case DownTrend => JsString("down")
    case EqTrend => JsString("eq")
  }


  implicit val skillWrites: Writes[Skill] = Writes[Skill] { o =>
    Json.obj(
      "id" -> o.id,
      "name" -> o.name,
      "parentId" -> o.parentId,
      "request" -> o.request,
      "path" -> Json.toJson(o.path)
    )
  }

  implicit val slimSkillWrites = Json.writes[SlimSkill]
  implicit val skillAdminWrites = Json.writes[SkillAdmin]
  implicit val skillTopStatWrites = Json.writes[SkillTopStat]

  implicit val skillReads: Reads[Skill] = (
    (JsPath \ "id").read[Long] and
    (JsPath \ "name").read[String](minLength[String](3) keepAnd maxLength[String](255)) and
    (JsPath \ "parentId").readNullable[Long] and
    (JsPath \ "delete").readNullable[Boolean]
  )((id, name, parent, delete) => Skill(id, name, parent, delete = delete))

  implicit val userSkillWrites: Writes[UserSkill] = Writes[UserSkill] { o =>
    Json.obj(
      "id" -> o.id,
      "skillId" -> o.skillId,
      "name" -> o.name,
      "request" -> o.request,
      "createdAt" -> o.createdAt
    )
  }

  implicit val userShortWrites = Json.writes[UserShort]
  implicit val userContactsWrites = Json.writes[UserContacts]

  implicit val languageWrites = Writes[Language] { o => JsString(o.language) }
  implicit val languageReads = JsPath.read[String].map(Language.stringLanguage)

  implicit val membershipTypeFormat = Json.format[MembershipType]
  implicit val membershipTypeSlimWrites = Json.writes[MembershipTypeSlim]

  implicit val membershipRequestWrites = Json.writes[MembershipRequest]
  implicit val membershipStatCountWrites = Json.writes[MembershipStatCount]

  implicit val membershipWrites = Json.writes[Membership]

  implicit val userMembershipsWrites = Json.writes[UserMemberships]

  implicit val cityWrites: Writes[City] = Writes {
    case GivenCity(city) => Json.obj("city" -> city, "other" -> false)
    case OtherCity => Json.obj("city" -> JsNull, "other" -> true)
  }

  implicit val userWrites = OWrites[User] { o =>
    Json.obj(
      "id" -> o.id,
      "firstName" -> o.firstName,
      "lastName" -> o.lastName,
      "email" -> o.email,
      "createdAt" -> o.createdAt,
      "skills" -> o.skills,
      "preferredLang" -> o.preferredLang.language,
      "telephone" -> o.telephone,
      "bio" -> o.bio,
      "memberships" -> o.memberships,
      "title" -> o.title,
      "city" -> o.city,
      "favorite" -> o.favorite
    )
  }

  val userFullWrites = Writes[User] { o =>
    Json.toJsObject(o)(userWrites) ++ Json.obj(
      "isAdmin" -> o.isAdmin,
      "ideasCount" -> o.ideasCount
    )
  }

  implicit val skillWithUsersWrites: Writes[SkillWithLinked] = Writes[SkillWithLinked] { o =>
    Json.obj(
      "skill" -> o.skill,
      "users" -> o.users
    )
  }


  implicit val teachActivityTypeWrites: Writes[TeachActivityType] = Writes[TeachActivityType] { o =>
    Json.toJson(o match {
      case LecturePerformance => "lecture_performance"
      case LabsSeminary => "labs_seminary"
      case XYZFormat => "xyz_format"
      case CulturalEvent => "cultural_event"
      case VerticalFormat => "vertical_format"
      case MachineUsage => "machine_usage"
    })
  }

  implicit val teachActivityTypeReads: Reads[TeachActivityType] = JsPath.read[String].map {
    case "lecture_performance" => LecturePerformance
    case "labs_seminary" => LabsSeminary
    case "xyz_format" => XYZFormat
    case "cultural_event" => CulturalEvent
    case "vertical_format" => VerticalFormat
    case "machine_usage" => MachineUsage
  }

  implicit val eventActivityTypeWrites: Writes[EventActivityType] = Writes[EventActivityType] { o =>
    Json.toJson(o match {
      case Talk => "talk"
      case Projection => "projection"
      case Exposition => "exposition"
      case Workshop => "workshop"
      case Performance => "performance"
    })
  }

  implicit val eventActivityTypeReads: Reads[EventActivityType] = JsPath.read[String].map {
    case "talk" => Talk
    case "projection" => Projection
    case "exposition" => Exposition
    case "workshop" => Workshop
    case "performance" => Performance
  }

  implicit val activityTypeWrites: Writes[ActivityType] = Writes[ActivityType] {
    case o: TeachActivityType => Json.toJson(o)(teachActivityTypeWrites)
    case o: EventActivityType => Json.toJson(o)(eventActivityTypeWrites)
  }


  implicit val audienceWrites: Writes[Audience] = Writes[Audience] { o =>
    Json.toJson(o match {
      case Kids => "kids"
      case Teenagers => "teenagers"
      case Students => "students"
      case Researchers => "researchers"
      case Professionals => "professionals"
      case Companies => "companies"
      case PublicAdministrations => "public_administrations"
      case Seniors => "seniors"
      case Immigrants => "immigrants"
      case Unemployed => "unemployed"
    })
  }

  implicit val audienceReads: Reads[Audience] = JsPath.read[String].map {
    case "kids" => Kids
    case "teenagers" => Teenagers
    case "students" => Students
    case "researchers" => Researchers
    case "professionals" => Professionals
    case "companies" => Companies
    case "public_administrations" => PublicAdministrations
    case "seniors" => Seniors
    case "immigrants" => Immigrants
    case "unemployed" => Unemployed
  }


  implicit val levelWrites: Writes[SOSLevel] = Writes[SOSLevel] { o =>
    Json.toJson(o match {
      case EntryLevel => "entry"
      case IntermediateLevel => "intermediate"
      case AdvancedLevel => "advanced"
    })
  }

  implicit val levelReads: Reads[SOSLevel] = JsPath.read[String].map {
    case "entry" => EntryLevel
    case "intermediate" => IntermediateLevel
    case "advanced" => AdvancedLevel
  }


  implicit val topicWrites: Writes[Topic] = Writes[Topic] { o =>
    Json.obj(
      "id" -> o.id,
      "topic" -> o.topic
    )
  }

  implicit val topicReads: Reads[Topic] = (
    (JsPath \ "id").read[Long] and
    (JsPath \ "topic").read[String](minLength[String](3) keepAnd maxLength[String](255)) and
    (JsPath \ "delete").readNullable[Boolean]
  )(Topic.apply _)


  implicit val sosDateWrites: Writes[SOSDate] = Writes[SOSDate] { o =>
    Json.obj(
      "id" -> o.id,
      "date" -> o.date,
      "startTime" -> o.startTime,
      "endTime" -> o.endTime
    )
  }

  implicit val sosDateReads: Reads[SOSDate] = (
    (JsPath \ "id").read[Long] and
      (JsPath \ "date").read[sql.Date] and
      (JsPath \ "startTime").read[sql.Time] and
      (JsPath \ "endTime").read[sql.Time] and
      (JsPath \ "delete").readNullable[Boolean]
    )(SOSDate.apply _)


  implicit val singleFixedDaysMeetingsWrites: Writes[SingleFixedDaysMeetings] = Writes[SingleFixedDaysMeetings] { o =>
    Json.obj(
      "id" -> o.id,
      "numberDays" -> o.numberDays,
      "numberHours" -> o.numberHours
    )
  }

  implicit val singleFixedDaysMeetingsReads: Reads[SingleFixedDaysMeetings] = (
    (JsPath \ "id").read[Long] and
    (JsPath \ "numberDays").read[Int] and
    (JsPath \ "numberHours").read[Int] and
    (JsPath \ "delete").readNullable[Boolean]
  )(SingleFixedDaysMeetings.apply _)

  implicit val recurringEntityWrites: Writes[RecurringEntity] = Writes[RecurringEntity] { o =>
    Json.toJson(o match {
      case Weekly => "weekly"
      case Monthly => "monthly"
      case Yearly => "yearly"
    })
  }

  implicit val recurringEntityReads: Reads[RecurringEntity] = JsPath.read[String].map {
    case "weekly" => Weekly
    case "monthly" => Monthly
    case "yearly" => Yearly
  }

  implicit val recurringMeetingsWrites: Writes[RecurringMeetings] = Writes[RecurringMeetings] { o =>
    Json.obj(
      "type" -> "recurring",
      "days" -> o.days,
      "every" -> o.every,
      "entity" -> o.entity,
      "hours" -> o.hours
    )
  }

  implicit val bazaarIdeaMeetingsTypeWrites: Writes[BazaarIdeaMeetingsType] = Writes[BazaarIdeaMeetingsType] {
    case FixedDaysMeetings(schedules) => Json.obj(
      "type" -> "fixed_days",
      "schedules" -> schedules
    )
    case o: RecurringMeetings => Json.toJson(o)(recurringMeetingsWrites)
  }

  implicit val bazaarIdeaMeetingsTypeReads: Reads[BazaarIdeaMeetingsType] = (JsPath \ "type").read[String].flatMap {
    case "fixed_days" =>
      (JsPath \ "schedules")
        .read[Seq[SingleFixedDaysMeetings]](minLength[Seq[SingleFixedDaysMeetings]](1)).map(FixedDaysMeetings)
    case "recurring" =>
      (
        (JsPath \ "days").read[Int] and
        (JsPath \ "every").read[Int] and
        (JsPath \ "entity").read[RecurringEntity] and
        (JsPath \ "hours").read[Int]
      )(RecurringMeetings.apply _)
  }


  implicit val bazaarIdeaLocationWrites = Writes[BazaarIdeaLocation] { o =>
    Json.toJson(o match {
      case AtSOS => "SOS"
      case CustomLocation(location) => location
    })
  }

  implicit val bazaarIdeaLocationReads: Reads[BazaarIdeaLocation] =
    JsPath.read[String](minLength[String](1) keepAnd maxLength[String](255)).map {
      case "SOS" => AtSOS
      case s => CustomLocation(s)
    }

  implicit val bazaarIdeaGuestWrites: Writes[BazaarIdeaGuest] = Writes[BazaarIdeaGuest] { o =>
    Json.obj(
      "id" -> o.id,
      "userId" -> o.userId,
      "firstName" -> o.firstName,
      "lastName" -> o.lastName,
      "title" -> o.title
    )
  }

  implicit val bazaarIdeaGuestReads: Reads[BazaarIdeaGuest] = (
    (JsPath \ "id").read[Long] and
    (JsPath \ "userId").readNullable[Long] and
    (JsPath \ "firstName").read[String](minLength[String](1) keepAnd maxLength[String](255)) and
    (JsPath \ "lastName").read[String](minLength[String](1) keepAnd maxLength[String](255)) and
    (JsPath \ "title").read[String](maxLength[String](255)) and
    (JsPath \ "delete").readNullable[Boolean]
  )(BazaarIdeaGuest.apply _)

  implicit val bazaarIdeaFundingWrites: Writes[BazaarIdeaFunding] = Writes[BazaarIdeaFunding] { o =>
    Json.toJson(o match {
      case TuitionFee => "tuition_fee"
      case Sponsor => "sponsor"
      case Grant => "grant"
      case Crowdfunding => "crowdfunding"
      case SelfFinanced => "self_financed"
    })
  }

  implicit val bazaarIdeaFundingReads: Reads[BazaarIdeaFunding] = JsPath.read[String].map {
    case "tuition_fee" => TuitionFee
    case "sponsor" => Sponsor
    case "grant" => Grant
    case "crowdfunding" => Crowdfunding
    case "self_financed" => SelfFinanced
  }

  implicit val bazaarCommentWrites: Writes[BazaarComment] = Writes[BazaarComment] { o =>
    Json.obj(
      "id" -> o.id,
      "userId" -> o.userId,
      "firstName" -> o.firstName,
      "lastName" -> o.lastName,
      "comment" -> o.comment,
      "createdAt" -> o.createdAt
    )
  }

  implicit val bazaarPreferenceWrites: Writes[BazaarPreference] = Writes[BazaarPreference] { o =>
    Json.obj(
      "id" -> o.id,
      "userId" -> o.userId,
      "ideaId" -> o.ideaId,
      "agree" -> o.agree,
      "wish" -> o.wish,
      "favorite" -> o.favorite,
      "viewed" -> o.viewed
    )
  }

  implicit val bazaarPreferenceCountsWrites = Json.writes[BazaarPreferenceCounts]

  private def getBazaarIdeaProps(o: BazaarIdea) = Json.obj(
    "id" -> o.id,
    "title" -> o.title,
    "creator" -> o.creator,
    "topics" -> o.topics,
    "valueDetails" -> o.valueDetails,
    "motivation" -> o.motivation,
    "score" -> o.score,
    "preference" -> o.preference,
    "counts" -> o.counts,
    "activityId" -> o.activityId,
    "createdAt" -> o.createdAt,
    "updatedAt" -> o.updatedAt
  )

  val bazaarLearnWrites: Writes[BazaarLearn] = Writes[BazaarLearn] { o =>
    getBazaarIdeaProps(o) ++ Json.obj(
      "location" -> o.location,
      "teachers" -> o.teachers,
      "tutors" -> o.tutors,
      "costs" -> o.costs,
      "type" -> "learn"
    )
  }

  val bazaarTeachWrites: Writes[BazaarTeach] = Writes[BazaarTeach] { o =>
    getBazaarIdeaProps(o) ++ Json.obj(
      "location" -> o.location,
      "teachers" -> o.teachers,
      "tutors" -> o.tutors,
      "costs" -> o.costs,
      "activityType" -> o.activityType,
      "audience" -> o.audience,
      "meetings" -> o.meetings,
      "dates" -> o.dates,
      "maxParticipants" -> o.maxParticipants,
      "programDetails" -> o.programDetails,
      "meetingDetails" -> o.meetingDetails,
      "outputDetails" -> o.outputDetails,
      "level" -> o.level,
      "requiredResources" -> o.requiredResources,
      "funding" -> o.funding,
      "type" -> "teach"
    )
  }

  val bazaarEventWrites: Writes[BazaarEvent] = Writes[BazaarEvent] { o =>
    getBazaarIdeaProps(o) ++ Json.obj(
      "activityType" -> o.activityType,
      "audience" -> o.audience,
      "meetings" -> o.meetings,
      "dates" -> o.dates,
      "maxParticipants" -> o.maxParticipants,
      "programDetails" -> o.programDetails,
      "requiredResources" -> o.requiredResources,
      "requiredSpaces" -> o.requiredSpaces,
      "funding" -> o.funding,
      "isOrganizer" -> o.isOrganizer,
      "guests" -> o.guests,
      "bookingRequired" -> o.bookingRequired,
      "type" -> "event"
    )
  }

  implicit val bazaarResearchRoleWrites: Writes[BazaarResearchRole] = Writes[BazaarResearchRole] { o =>
    Json.obj(
      "id" -> o.id,
      "people" -> o.people,
      "skills" -> o.skills
    )
  }

  implicit val bazaarResearchRoleReads: Reads[BazaarResearchRole] = (
    (JsPath \ "id").read[Long] and
    (JsPath \ "people").read[Int](min(1)) and
    (JsPath \ "skills").read[Seq[Skill]](minLength[Seq[Skill]](1)) and
    (JsPath \ "delete").readNullable[Boolean]
  )(BazaarResearchRole)

  val bazaarResearchWrites: Writes[BazaarResearch] = Writes[BazaarResearch] { o =>
    getBazaarIdeaProps(o) ++ Json.obj(
      "organizationName" -> o.organizationName,
      "requiredResources" -> o.requiredResources,
      "positions" -> o.positions,
      "deadline" -> o.deadline,
      "duration" -> o.duration,
      "type" -> "research"
    )
  }

  implicit val bazaarIdeaWrites: Writes[BazaarIdea] = Writes[BazaarIdea] {
    case o: BazaarLearn => Json.toJson(o)(bazaarLearnWrites)
    case o: BazaarTeach => Json.toJson(o)(bazaarTeachWrites)
    case o: BazaarEvent => Json.toJson(o)(bazaarEventWrites)
    case o: BazaarResearch => Json.toJson(o)(bazaarResearchWrites)
  }

  implicit val bazaarIdeaTypeWrites = Writes[BazaarIdeaType] {
    case BazaarLearnType => JsString("learn")
    case BazaarTeachType => JsString("teach")
    case BazaarEventType => JsString("event")
    case BazaarResearchType => JsString("research")
  }

  implicit val bazaarPreferenceSlimWrites = Json.writes[BazaarPreferenceSlim]
  implicit val bazaarIdeaSlimWrites = Json.writes[BazaarIdeaSlim]

  implicit val bazaarIdeaMiniWrites = Json.writes[BazaarIdeaMini]


  val imageWithDataWrites: OWrites[ImageWithData] = OWrites[ImageWithData] { o =>
    Json.obj(
      "extension" -> o.extension,
      "url" -> o.url
    )
  }


  implicit val dataImageWrites: Writes[DataImage] = imageWithDataWrites


  implicit val imageWrites: Writes[Image] = Writes[Image] { o =>
    Json.obj(
      "id" -> o.id
    ) ++ imageWithDataWrites.writes(o)
  }


  implicit val imageGalleryWrites: Writes[ImageGallery] = Writes[ImageGallery] { o =>
    Json.obj(
      "id" -> o.id,
      "name" -> o.name,
      "images" -> o.images
    )
  }


  implicit val dataImageReads: Reads[DataImage] =
    (JsPath \ "data").readNullable[String].flatMap {
      case Some(_) =>
        (JsPath \ "data").read[String]
          .map(DataImage.fromData)
          .collect(JsonValidationError("failed to parse data image")) { case Some(d) => d }
      case None =>
        (JsPath \ "extension").read[String].map(extension => DataImage(extension))
    }

  implicit val imageReads: Reads[Image] =
    (dataImageReads and (JsPath \ "id").read[Long] and (JsPath \ "delete").readNullable[Boolean]) {
      (dataImage, id, delete) =>
        Image(id = id, extension = dataImage.extension, data = dataImage.data, delete = delete)
    }

  implicit val imageGalleryReads: Reads[ImageGallery] = (
    (JsPath \ "id").read[Long] and
      (JsPath \ "images").read[Seq[Image]] and
      (JsPath \ "name").read[String](maxLength[String](255))
    )(ImageGallery.apply _)


  private def getActivityProps[A <: Activity[A]](activity: A) = Json.obj(
    "id" -> activity.id,
    "language" -> activity.language.language,
    "title" -> activity.title,
    "coverPic" -> activity.coverPic,
    "gallery" -> activity.gallery,
    "topics" -> activity.topics,
    "description" -> activity.description,
    "deadline" -> activity.optDeadline,
    "bazaarIdeaId" -> activity.bazaarIdeaId,
    "createdAt" -> activity.createdAt,
    "updatedAt" -> activity.updatedAt,
    "favorite" -> activity.favorite
  )

  implicit val activityScheduleWrites: Writes[ActivitySchedule] = Writes[ActivitySchedule] {
    case o: FiniteMeetings => Json.obj(
      "totalDays" -> o.totalDays,
      "totalHours" -> o.totalHours,
      "type" -> "finite_days"
    )
    case o: RecurringMeetings => Json.toJson(o)(recurringMeetingsWrites)
  }

  implicit val activityScheduleReads: Reads[ActivitySchedule] = (JsPath \ "type").read[String].flatMap {
    case "finite_days" => (
        (JsPath \ "totalDays").read[Int] and
        (JsPath \ "totalHours").read[Int]
      )(FiniteMeetings.apply _)
    case "recurring" => (
        (JsPath \ "days").read[Int] and
        (JsPath \ "every").read[Int] and
        (JsPath \ "entity").read[RecurringEntity] and
        (JsPath \ "hours").read[Int]
      )(RecurringMeetings.apply _)
  }

  implicit val activityGuestWrites: Writes[ActivityGuest] = Writes[ActivityGuest] { o =>
    Json.obj(
      "id" -> o.id,
      "userId" -> o.userId,
      "firstName" -> o.firstName,
      "lastName" -> o.lastName,
      "title" -> o.title,
      "bio" -> o.bio
    )
  }

  implicit val activityGuestSlimWrites = Json.writes[ActivityGuestSlim]

  implicit val activityGuestReads: Reads[ActivityGuest] = (
    (JsPath \ "id").read[Long] and
    (JsPath \ "userId").readNullable[Long] and
    (JsPath \ "firstName").read[String](minLength[String](1) keepAnd maxLength[String](127)) and
    (JsPath \ "lastName").read[String](minLength[String](1) keepAnd maxLength[String](127)) and
    (JsPath \ "title").read[String](minLength[String](1) keepAnd maxLength[String](255)) and
    (JsPath \ "bio").read[String](minLength[String](1) keepAnd maxLength[String](1048)) and
    (JsPath \ "delete").readNullable[Boolean]
  )(ActivityGuest.apply _)


  implicit val paymentMethodReads: Reads[PaymentMethod] =
    JsPath.read[String].collect(JsonValidationError("invalid payment method")) {
      case "paypal" => PayPal
      case "credit_card" => CreditCard
      case "wire_transfer" => WireTransfer
    }

  implicit val paymentMethodWrites: Writes[PaymentMethod] = Writes[PaymentMethod] { o =>
    JsString(o match {
      case PayPal => "paypal"
      case CreditCard => "credit_card"
      case WireTransfer => "wire_transfer"
    })
  }


  implicit val activitySubscriptionWrites: Writes[ActivitySubscription] = Writes[ActivitySubscription] { o =>
    Json.obj(
      "createdAt" -> o.createdAt,
      "paymentMethod" -> o.paymentMethod,
      "verified" -> o.verified,
      "cro" -> o.cro,
      "transactionId" -> o.transactionId,
      "amount" -> o.amount
    )
  }

  implicit val adminActivitySubscriptionWrites: Writes[AdminActivitySubscription] = Writes[AdminActivitySubscription] { o =>
    Json.obj(
      "user" -> o.user,
      "subscription" -> o.subscription
    )
  }

  private def getActivityTeachEventProps[A <: ActivityTeachEvent[A]](activity: A) = getActivityProps(activity) ++
    Json.obj(
      "level" -> activity.level,
      "audience" -> activity.audience,
      "outputType" -> activity.outputType,
      "program" -> activity.program,
      "activityType" -> activity.activityType,
      "costs" -> activity.costs,
      "payments" -> activity.payments,
      "minParticipants" -> activity.minParticipants,
      "maxParticipants" -> activity.maxParticipants,
      "schedule" -> activity.schedule,
      "dates" -> activity.dates,
      "startTime" -> activity.startTime,
      "guests" -> activity.guests,
      "requiredSkills" -> activity.requiredSkills,
      "acquiredSkills" -> activity.acquiredSkills,
      "subscription" -> activity.subscription,
      "subscriptions" -> activity.subscriptions
    )

  implicit val teachCategoryWrites: Writes[TeachCategory] = Writes[TeachCategory] { o =>
    JsString(o match {
      case CategoryX => "x"
      case CategoryY => "y"
      case CategoryZ => "z"
    })
  }

  implicit val teachCategoryReads: Reads[TeachCategory] = JsPath.read[String]
    .collect(JsonValidationError("failed to parse teaching category")) {
      case "x" => CategoryX
      case "y" => CategoryY
      case "z" => CategoryZ
    }

  implicit val activityDeadlineWrites = Json.writes[ActivityDeadline]

  implicit val activityTeachWrites: Writes[ActivityTeach] = Writes[ActivityTeach] { o =>
    getActivityTeachEventProps(o) ++ Json.obj(
      "outputDescription" -> o.outputDescription,
      "teachCategory" -> o.teachCategory,
      "type" -> "teach"
    )
  }

  implicit val activityTeachSlimWrites = new Writes[ActivityTeachSlim] {
    private val baseW = Json.writes[ActivityTeachSlim]
    override def writes(o: ActivityTeachSlim): JsValue =
      baseW.writes(o) + ("type" -> JsString("teach"))
  }

  implicit val activityEventWrites: Writes[ActivityEvent] = Writes[ActivityEvent] { o =>
    getActivityTeachEventProps(o) + ("type" -> JsString("event"))
  }

  implicit val activityEventSlimWrites = new Writes[ActivityEventSlim] {
    private val baseW = Json.writes[ActivityEventSlim]
    override def writes(o: ActivityEventSlim): JsValue =
      baseW.writes(o) + ("type" -> JsString("event"))
  }

  implicit val activityResearchRoleAppFull = Json.writes[ActivityResearchAppFull]
  implicit val activityResearchRoleApp = Json.writes[ActivityResearchApp]

  implicit val activityResearchRoleWrites = Json.writes[ActivityResearchRole]

  implicit val activityResearchRoleReads = (
    (JsPath \ "id").read[Long] and
    (JsPath \ "people").read[Int](min(1)) and
    (JsPath \ "skills").read[Seq[Skill]](minLength[Seq[Skill]](1)) and
    (JsPath \ "delete").readNullable[Boolean]
  ){
    (id, people, skills, delete) =>
      ActivityResearchRole(
        id = id,
        people = people,
        skills = skills,
        delete = delete)
  }

  implicit val activityResearchTeamWrites = Json.writes[ActivityResearchTeam]
  implicit val activityResearchTeamRead = Json.reads[ActivityResearchTeam]

  implicit val activityResearchWrites = Writes[ActivityResearch] { o =>
    val props = mutable.ArrayBuffer[(String, JsValueWrapper)](
      "organizationName" -> o.organizationName,
      "motivation" -> o.motivation,
      "valueDetails" -> o.valueDetails,
      "startDate" -> o.startDate,
      "duration" -> o.duration,
      "projectLink" -> o.projectLink,
      "roles" -> o.roles,
      "team" -> o.team,
      "type" -> "research")

    if (o.userHasAccess.isDefined)
      props += "userHasAccess" -> o.userHasAccess

    getActivityProps(o) ++ Json.obj(props:_*)
  }

  implicit val activityResearchSlimWrites = new Writes[ActivityResearchSlim] {
    private val baseW = Json.writes[ActivityResearchSlim]
    override def writes(o: ActivityResearchSlim): JsValue =
      baseW.writes(o) + ("type" -> JsString("research"))
  }

  implicit val activityWrites: Writes[Activity[_]] = Writes[Activity[_]] {
    case o: ActivityTeach => Json.toJson(o)(activityTeachWrites)
    case o: ActivityEvent => Json.toJson(o)(activityEventWrites)
    case o: ActivityResearch => Json.toJson(o)(activityResearchWrites)
  }

  implicit val activitySlimWrites = Writes[ActivitySlim] {
    case o: ActivityTeachSlim => activityTeachSlimWrites.writes(o)
    case o: ActivityEventSlim => activityEventSlimWrites.writes(o)
    case o: ActivityResearchSlim => activityResearchSlimWrites.writes(o)
  }

  implicit val activityMiniWrites = Json.writes[ActivityMini]


  implicit val fablabMachineWrites = Json.writes[FablabMachine]
  implicit val fablabMachineReads = Json.reads[FablabMachine]
  implicit val slimMachineWrites = Json.writes[SlimMachine]
  implicit val slimMachineReads = Json.reads[SlimMachine]

  implicit val fablabReservationTimeWrites = Json.writes[FablabReservationTime]
  private val fablabReservationWritesM = Json.writes[FablabReservation]
  implicit val fablabReservationWrites = Writes[FablabReservation] { o =>
    fablabReservationWritesM.writes(o) ++ Json.obj(
      "startTime" -> o.startTime,
      "endTime" -> o.endTime)
  }

  implicit val fablabReservationFlatWrites = Json.writes[FablabReservationFlat]

  implicit val fablabReservationTimeReads = Json.reads[FablabReservationTime]

  def fablabReservationReads(machineId: Long, userShort: UserShort): Reads[FablabReservation] = (
    (JsPath \ "times").read[Seq[FablabReservationTime]](minLength[Seq[FablabReservationTime]](1)) and
    (JsPath \ "operator").read[Boolean]
  ) {
    (times, operator) =>
      FablabReservation(
        id = 0,
        machine = SlimMachine(machineId, ""),
        user = userShort,
        times = times,
        operator = operator,
        createdAt = dummyTimestamp)
  }

  implicit val fablabQuotationWrites = Json.writes[FablabQuotation]


  implicit val bazaarIdeaStatRowWrites = Json.writes[BazaarIdeaStatRow]
  implicit val bazaarIdeasCountWrites = Json.writes[BazaarIdeasCount]
  implicit val bazaarTopCreatorWrites = Json.writes[BazaarTopCreator]

  implicit val activityClassWrites = Writes[ActivityClass] {
    case ActivityEventClass => JsString("event")
    case ActivityTeachClass => JsString("teach")
    case ActivityResearchClass => JsString("research")
  }

  implicit val activityStatWrites = Json.writes[ActivityStat]
  private val activityStatCountWritesM = Json.writes[ActivityStatCount]
  implicit val activityStatCountWrites = Writes[ActivityStatCount] { o =>
    activityStatCountWritesM.writes(o) ++ Json.obj(
      "done" -> o.done,
      "success" -> o.success
    )
  }
  implicit val activityProjectStat = Json.writes[ActivityProjectStat]
  implicit val activityProjectStatCountWrites = Json.writes[ActivityProjectStatCount]

  implicit val fablabMachineStatWrites = Json.writes[FablabMachineStat]
  implicit val fablabStatCountWrites = Json.writes[FablabStatCount]

  implicit val userStatWrites = Json.writes[UserStat]



  trait WithId[T] {
    def update(value: T, id: Long): T
    def id(value: T): Long
  }

  object DefaultWithId {

    implicit val guestWithId = new WithId[BazaarIdeaGuest] {
      override def update(value: BazaarIdeaGuest, id: Long) = value.copy(id = id)
      override def id(value: BazaarIdeaGuest) = value.id
    }

    implicit val topicWithId = new WithId[Topic] {
      override def update(value: Topic, id: Long) = value.copy(id = id)
      override def id(value: Topic) = value.id
    }

    implicit val singleFixedDaysMeetingsWithId = new WithId[SingleFixedDaysMeetings] {
      override def update(value: SingleFixedDaysMeetings, id: Long) = value.copy(id = id)
      override def id(value: SingleFixedDaysMeetings) = value.id
    }

    implicit val bazaarIdeaDateWithId = new WithId[SOSDate] {
      override def update(value: SOSDate, id: Long) = value.copy(id = id)
      override def id(value: SOSDate) = value.id
    }

    implicit val skillWithId = new WithId[Skill] {
      override def update(value: Skill, id: Long) = value.copy(id = id)
      override def id(value: Skill) = value.id
    }

    implicit val bazaarResearchRoleWithId = new WithId[BazaarResearchRole] {
      override def update(value: BazaarResearchRole, id: Long) = value.copy(id = id)
      override def id(value: BazaarResearchRole) = value.id
    }

    implicit val activityWithId = new WithId[Activity[_]] {
      override def update(value: Activity[_], id: Long) = value match {
        case a: ActivityTeach => a.copy(id = id)
        case a: ActivityEvent => a.copy(id = id)
        case a: ActivityResearch => a.copy(id = id)
      }
      override def id(value: Activity[_]) = value.id
    }

    implicit val imageWithId = new WithId[Image] {
      override def update(value: Image, id: Long) = value.copy(id = id)
      override def id(value: Image) = value.id
    }

    implicit val activityGuestWithId = new WithId[ActivityGuest] {
      override def update(value: ActivityGuest, id: Long) = value.copy(id = id)
      override def id(value: ActivityGuest) = value.id
    }

    implicit val activityResearchRoleWithId = new WithId[ActivityResearchRole] {
      override def update(value: ActivityResearchRole, id: Long) = value.copy(id = id)
      override def id(value: ActivityResearchRole): Long = value.id
    }

    implicit val activityResearchTeamWithId = new WithId[ActivityResearchTeam] {
      override def update(value: ActivityResearchTeam, id: Long) = value.copy(id = id)
      override def id(value: ActivityResearchTeam) = value.id
    }

  }

  object WithIdSyntax {
    implicit class WithIdOps[T](value: T) {
      def copy(id: Long)(implicit withId: WithId[T]): T = withId.update(value, id)
      def id(implicit withId: WithId[T]): Long = withId.id(value)
    }
  }

}
