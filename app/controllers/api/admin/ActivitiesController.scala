package controllers.api.admin

import java.sql.{Date, Timestamp}

import controllers.api.{ApiController, StatsController}
import controllers.api.ApiController.ApiControllerComponents
import controllers.api.admin.ActivitiesController._
import javax.inject.{Inject, Singleton}
import models.Implicits._
import models._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads}
import services.{ActivityService, ActivityStatService, ServiceReply}

import scala.concurrent.Future


private object ActivitiesController {

  case class ActivityEventRequest(
    language: String,
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
    bazaarIdeaId: Option[Long])

  private object ActivityEventRequest {
    def fromParts(p1: ActivityEventRequest1, p2: ActivityEventRequest2) = ActivityEventRequest(
      language = p1.language,
      title = p1.title,
      coverPic = p1.coverPic,
      gallery = p1.gallery,
      level = p1.level,
      audience = p1.audience,
      topics = p1.topics,
      description = p1.description,
      outputType = p1.outputType,
      program = p1.program,
      activityType = p1.activityType,
      costs = p2.costs,
      payments = p2.payments,
      deadline = p2.deadline,
      minParticipants = p2.minParticipants,
      maxParticipants = p2.maxParticipants,
      schedule = p2.schedule,
      dates = p2.dates,
      startTime = p2.startTime,
      guests = p2.guests,
      requiredSkills = p2.requiredSkills,
      acquiredSkills = p2.acquiredSkills,
      bazaarIdeaId = p2.bazaarIdeaId)
  }

  private final case class ActivityEventRequest1(
    language: String,
    title: String,
    coverPic: DataImage,
    gallery: ImageGallery,
    level: Option[SOSLevel],
    audience: Seq[Audience],
    topics: Seq[Topic],
    description: String,
    outputType: String,
    program: String,
    activityType: EventActivityType)

  private final case class ActivityEventRequest2(
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
    bazaarIdeaId: Option[Long])

  private val activityEventRequest1Reads = Json.reads[ActivityEventRequest1]
  private val activityEventRequest2Reads = Json.reads[ActivityEventRequest2]
  implicit val activityEventRequestReads =
    (activityEventRequest1Reads and activityEventRequest2Reads)(ActivityEventRequest.fromParts _)

  def eventRequestToModel(id: Long, request: ActivityEventRequest): ActivityEvent = ActivityEvent(
    id = id,
    language = request.language,
    title = request.title,
    coverPic = request.coverPic,
    gallery = request.gallery,
    level = request.level,
    audience = request.audience,
    topics = request.topics,
    description = request.description,
    outputType = request.outputType,
    program = request.program,
    activityType = request.activityType,
    costs = request.costs,
    payments = request.payments,
    deadline = request.deadline,
    minParticipants = request.minParticipants,
    maxParticipants = request.maxParticipants,
    schedule = request.schedule,
    dates = request.dates,
    startTime = request.startTime,
    guests = request.guests,
    requiredSkills = request.requiredSkills,
    acquiredSkills = request.acquiredSkills,
    bazaarIdeaId = request.bazaarIdeaId,
    createdAt = dummyTimestamp,
    updatedAt = dummyTimestamp)


  case class ActivityTeachRequest(
    language: String,
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
    teachCategory: TeachCategory,
    costs: Option[Double],
    payments: Boolean,
    deadline: Date,
    minParticipants: Option[Int],
    maxParticipants: Option[Int],
    schedule: ActivitySchedule,
    dates: Seq[SOSDate],
    startTime: Option[Timestamp],
    guests: Seq[ActivityGuest],
    requiredSkills: Seq[Skill],
    acquiredSkills: Seq[Skill],
    bazaarIdeaId: Option[Long])

  private object ActivityTeachRequest {
    def fromParts(p1: ActivityTeachRequest1, p2: ActivityTeachRequest2): ActivityTeachRequest = ActivityTeachRequest(
      language = p1.language,
      title = p1.title,
      coverPic = p1.coverPic,
      gallery = p1.gallery,
      level = p1.level,
      audience = p1.audience,
      topics = p1.topics,
      description = p1.description,
      outputType = p1.outputType,
      outputDescription = p1.outputDescription,
      program = p1.program,
      activityType = p1.activityType,
      teachCategory = p1.teachCategory,
      costs = p2.costs,
      payments = p2.payments,
      deadline = p2.deadline,
      minParticipants = p2.minParticipants,
      maxParticipants = p2.maxParticipants,
      schedule = p2.schedule,
      dates = p2.dates,
      startTime = p2.startTime,
      guests = p2.guests,
      requiredSkills = p2.requiredSkills,
      acquiredSkills = p2.acquiredSkills,
      bazaarIdeaId = p2.bazaarIdeaId)
  }

  private final case class ActivityTeachRequest1(
    language: String,
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
    teachCategory: TeachCategory)

  private final case class ActivityTeachRequest2(
    costs: Option[Double],
    payments: Boolean,
    deadline: Date,
    minParticipants: Option[Int],
    maxParticipants: Option[Int],
    schedule: ActivitySchedule,
    dates: Seq[SOSDate],
    startTime: Option[Timestamp],
    guests: Seq[ActivityGuest],
    requiredSkills: Seq[Skill],
    acquiredSkills: Seq[Skill],
    bazaarIdeaId: Option[Long])

  private val activityTeachRequest1Reads = Json.reads[ActivityTeachRequest1]
  private val activityTeachRequest2Reads = Json.reads[ActivityTeachRequest2]
  implicit val activityTeachRequestReads =
    (activityTeachRequest1Reads and activityTeachRequest2Reads)(ActivityTeachRequest.fromParts _)

  def teachRequestToModel(id: Long, request: ActivityTeachRequest): ActivityTeach = ActivityTeach(
    id = id,
    language = request.language,
    title = request.title,
    coverPic = request.coverPic,
    gallery = request.gallery,
    level = request.level,
    audience = request.audience,
    topics = request.topics,
    description = request.description,
    outputType = request.outputType,
    outputDescription = request.outputDescription,
    program = request.program,
    activityType = request.activityType,
    teachCategory = request.teachCategory,
    costs = request.costs,
    payments = request.payments,
    deadline = request.deadline,
    minParticipants = request.minParticipants,
    maxParticipants = request.maxParticipants,
    schedule = request.schedule,
    dates = request.dates,
    startTime = request.startTime,
    guests = request.guests,
    requiredSkills = request.requiredSkills,
    acquiredSkills = request.acquiredSkills,
    bazaarIdeaId = request.bazaarIdeaId,
    createdAt = dummyTimestamp,
    updatedAt = dummyTimestamp)


  case class ActivityResearchRequest(
    language: String,
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
    bazaarIdeaId: Option[Long])

  implicit val activityResearchRequestReads = Json.reads[ActivityResearchRequest]

  def researchRequestToModel(id: Long, request: ActivityResearchRequest): ActivityResearch = ActivityResearch(
    id = id,
    language = request.language,
    title = request.title,
    coverPic = request.coverPic,
    gallery = request.gallery,
    topics = request.topics,
    organizationName = request.organizationName,
    motivation = request.motivation,
    valueDetails = request.valueDetails,
    deadline = request.deadline,
    startDate = request.startDate,
    duration = request.duration,
    projectLink = request.projectLink,
    roles = request.roles,
    team = request.team,
    bazaarIdeaId = request.bazaarIdeaId,
    createdAt = dummyTimestamp,
    updatedAt = dummyTimestamp)

}


@Singleton
class ActivitiesController @Inject()(
  override val controllerComponents: ApiControllerComponents,
  activityService: ActivityService,
  activityStatService: ActivityStatService
) extends ApiController with StatsController {

  private type FutureNotFound[A] = Future[ServiceReply[A, services.NotFound]]

  // NOTE: currying helps type inference
  private def create[A <: Activity[A], R: Reads](requestToModel: (Long, R) => A)(create: A => Future[A]) =
    AdminScope { implicit request =>
      jsonOrBadRequest { json =>
        json.validate[R].fold(
          defaultErrorHandler,
          createRequest => {
            val activity = requestToModel(0, createRequest)
            create(activity).map(a => Ok(Json.toJson(a)))
          }
        )
      }
    }

  def createEvent = create(eventRequestToModel)(activityService.create[ActivityEvent])

  def createTeach = create(teachRequestToModel)(activityService.create[ActivityTeach])

  def createResearch = create(researchRequestToModel)(activityService.create)


  private def update[A <: Activity[A], R: Reads](id: Long, reqToModel: (Long, R) => A)(update: A => FutureNotFound[A]) =
    AdminScope { implicit request =>
      jsonOrBadRequest { json =>
        json.validate[R].fold(
          defaultErrorHandler,
          updateRequest => {
            val activity = reqToModel(id, updateRequest)
            update(activity).map(resultJsonFromServiceReply[A, services.NotFound])
          }
        )
      }
    }

  def updateEvent(id: Long) = update(id, eventRequestToModel)(activityService.update[ActivityEvent])

  def updateTeach(id: Long) = update(id, teachRequestToModel)(activityService.update[ActivityTeach])

  def updateResearch(id: Long) = update(id, researchRequestToModel)(activityService.update)


  private def delete(id: Long, f: Long => FutureNotFound[Unit]) = AdminScope { _ =>
    f(id).map(resultJsonFromServiceReply(_))
  }

  def deleteEvent(id: Long) = delete(id, activityService.deleteTeachEvent)

  def deleteTeach(id: Long) = delete(id, activityService.deleteTeachEvent)

  def deleteResearch(id: Long) = delete(id, activityService.deleteResearch)


  def subscriptions(activityId: Long) = AdminScope { _ =>
    activityService.subscriptions(activityId).map(subs => Ok(Json.obj("subscriptions" -> subs)))
  }

  def verifySubscription(activityId: Long, userId: Long) = AdminScope { implicit request =>
    jsonOrBadRequest { json =>
      json.validate((JsPath \ "success").read[Boolean]).fold(
        defaultErrorHandler,
        activityService.verifySubscription(activityId, userId, _).map(resultJsonFromServiceReply(_))
      )
    }
  }

  def deleteSubscription(activityId: Long, userId: Long) = AdminScope { _ =>
    activityService.deleteSubscription(activityId, userId).map(resultJsonFromServiceReply(_))
  }

  def researchByUser(userId: Long) = AdminScope { implicit request =>
    activityService.researchByUser(userId, getLanguage).map(toOk("activities"))
  }


  def next = AdminScope { implicit request =>
    processStatsRequest("from", activityStatService.next(_, getLanguage))
  }

  def top = AdminScope { implicit request =>
    processStatsRequest("from", "to", activityStatService.top(_, _, getLanguage))
  }

  def count = AdminScope { implicit request =>
    processStatsRequest("date", activityStatService.count)
  }

  def topProjects = AdminScope { implicit request =>
    processStatsRequest("from", "to", activityStatService.topProjects(_, _, getLanguage))
  }

  def countProjects = AdminScope { implicit request =>
    processStatsRequest("from", "to", activityStatService.countProjects)
  }

}
