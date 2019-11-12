package controllers.api

import controllers.api.ApiController.{ApiControllerComponents, RequestType}
import javax.inject.{Inject, Singleton}
import models.Implicits._
import models._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import services.BazaarIdeaService
import services.Service.ServiceRep

import scala.concurrent.Future
import scala.language.implicitConversions


private object BazaarIdeasController {

  def validateText(n: Int, required: Boolean) =
    (if (required) minLength[String](1) else JsPath.read[String]) keepAnd maxLength[String](n)
  def validateShortText(required: Boolean = true) = validateText(255, required)
  def validateMidText(required: Boolean = true) = validateText(511, required)
  def validateLongText(required: Boolean = true) = validateText(2047, required)
  val validateTopics = JsPath.read[Seq[Topic]]
    .filter(JsonValidationError("error.minLength"))(_.exists(t => !t.toDelete))


  case class BazaarLearnRequest(
    title: String,
    location: BazaarIdeaLocation,
    topics: Seq[Topic],
    teachers: Seq[BazaarIdeaGuest],
    tutors: Seq[BazaarIdeaGuest],
    valueDetails: String,
    motivation: String,
    costs: Option[String])

  implicit val bazaarLearnRequestReads: Reads[BazaarLearnRequest] = (
    (JsPath \ "title").read[String](validateShortText()) and
      (JsPath \ "location").read[BazaarIdeaLocation] and
      (JsPath \ "topics").read[Seq[Topic]](validateTopics) and
      (JsPath \ "teachers").read[Seq[BazaarIdeaGuest]] and
      (JsPath \ "tutors").read[Seq[BazaarIdeaGuest]] and
      (JsPath \ "valueDetails").read[String](validateLongText()) and
      (JsPath \ "motivation").read[String](validateLongText()) and
      (JsPath \ "costs").readNullable[String](validateMidText(required = false))
    ) (BazaarLearnRequest.apply _)

  def learnRequestToModel(id: Long, user: User, bazaarLearnRequest: BazaarLearnRequest) = BazaarLearn(
    id = id,
    title = bazaarLearnRequest.title,
    creator = user,
    location = bazaarLearnRequest.location,
    topics = bazaarLearnRequest.topics,
    teachers = bazaarLearnRequest.teachers,
    tutors = bazaarLearnRequest.tutors,
    valueDetails = bazaarLearnRequest.valueDetails,
    motivation = bazaarLearnRequest.motivation,
    costs = bazaarLearnRequest.costs
  )


  case class BazaarTeachRequest(
    title: String,
    location: BazaarIdeaLocation,
    activityType: TeachActivityType,
    audience: Seq[Audience],
    level: SOSLevel,
    topics: Seq[Topic],
    meetings: BazaarIdeaMeetingsType,
    dates: Seq[SOSDate],
    requiredResources: Option[String],
    maxParticipants: Int,
    teachers: Seq[BazaarIdeaGuest],
    tutors: Seq[BazaarIdeaGuest],
    programDetails: String,
    meetingDetails: String,
    outputDetails: String,
    valueDetails: String,
    motivation: String,
    funding: Seq[BazaarIdeaFunding],
    costs: Option[String])

  implicit val bazaarTeachRequestReads: Reads[BazaarTeachRequest] = (
    (JsPath \ "title").read[String](validateShortText()) and
      (JsPath \ "location").read[BazaarIdeaLocation] and
      (JsPath \ "activityType").read[TeachActivityType] and
      (JsPath \ "audience").read[Seq[Audience]] and
      (JsPath \ "level").read[SOSLevel] and
      (JsPath \ "topics").read[Seq[Topic]](validateTopics) and
      (JsPath \ "meetings").read[BazaarIdeaMeetingsType] and
      (JsPath \ "dates").read[Seq[SOSDate]] and
      (JsPath \ "requiredResources").readNullable[String](validateMidText(required = false)) and
      (JsPath \ "maxParticipants").read[Int] and
      (JsPath \ "teachers").read[Seq[BazaarIdeaGuest]] and
      (JsPath \ "tutors").read[Seq[BazaarIdeaGuest]] and
      (JsPath \ "programDetails").read[String](validateLongText()) and
      (JsPath \ "meetingDetails").read[String](validateLongText()) and
      (JsPath \ "outputDetails").read[String](validateLongText()) and
      (JsPath \ "valueDetails").read[String](validateLongText()) and
      (JsPath \ "motivation").read[String](validateLongText()) and
      (JsPath \ "funding").read[Seq[BazaarIdeaFunding]](minLength[Seq[BazaarIdeaFunding]](1)) and
      (JsPath \ "costs").readNullable[String](validateMidText(required = false))
    ) (BazaarTeachRequest.apply _)

  def teachRequestToModel(id: Long, user: User, bazaarTeachRequest: BazaarTeachRequest) = BazaarTeach(
    id = id,
    title = bazaarTeachRequest.title,
    creator = user,
    location = bazaarTeachRequest.location,
    activityType = bazaarTeachRequest.activityType,
    audience = bazaarTeachRequest.audience,
    level = bazaarTeachRequest.level,
    topics = bazaarTeachRequest.topics,
    meetings = bazaarTeachRequest.meetings,
    dates = bazaarTeachRequest.dates,
    requiredResources = bazaarTeachRequest.requiredResources,
    maxParticipants = bazaarTeachRequest.maxParticipants,
    teachers = bazaarTeachRequest.teachers,
    tutors = bazaarTeachRequest.tutors,
    programDetails = bazaarTeachRequest.programDetails,
    meetingDetails = bazaarTeachRequest.meetingDetails,
    outputDetails = bazaarTeachRequest.outputDetails,
    valueDetails = bazaarTeachRequest.valueDetails,
    motivation = bazaarTeachRequest.motivation,
    funding = bazaarTeachRequest.funding,
    costs = bazaarTeachRequest.costs
  )


  case class BazaarEventRequest(
    title: String,
    activityType: EventActivityType,
    audience: Seq[Audience],
    topics: Seq[Topic],
    meetings: BazaarIdeaMeetingsType,
    dates: Seq[SOSDate],
    requiredResources: Option[String],
    requiredSpaces: Option[String],
    maxParticipants: Int,
    programDetails: String,
    valueDetails: String,
    motivation: String,
    funding: Seq[BazaarIdeaFunding],
    isOrganizer: Boolean,
    guests: Seq[BazaarIdeaGuest],
    bookingRequired: Boolean)

  implicit val bazaarEventRequestReads: Reads[BazaarEventRequest] = (
    (JsPath \ "title").read[String](validateShortText()) and
      (JsPath \ "activityType").read[EventActivityType] and
      (JsPath \ "audience").read[Seq[Audience]] and
      (JsPath \ "topics").read[Seq[Topic]](validateTopics) and
      (JsPath \ "meetings").read[BazaarIdeaMeetingsType] and
      (JsPath \ "dates").read[Seq[SOSDate]] and
      (JsPath \ "requiredResources").readNullable[String](validateMidText(required = false)) and
      (JsPath \ "requiredSpaces").readNullable[String](validateMidText(required = false)) and
      (JsPath \ "maxParticipants").read[Int] and
      (JsPath \ "programDetails").read[String](validateLongText()) and
      (JsPath \ "valueDetails").read[String](validateLongText()) and
      (JsPath \ "motivation").read[String](validateLongText()) and
      (JsPath \ "funding").read[Seq[BazaarIdeaFunding]](minLength[Seq[BazaarIdeaFunding]](1)) and
      (JsPath \ "isOrganizer").read[Boolean] and
      (JsPath \ "guests").read[Seq[BazaarIdeaGuest]] and
      (JsPath \ "bookingRequired").read[Boolean]
    ) (BazaarEventRequest.apply _)

  def eventRequestToModel(id: Long, user: User, bazaarEventRequest: BazaarEventRequest) = BazaarEvent(
    id = id,
    title = bazaarEventRequest.title,
    creator = user,
    activityType = bazaarEventRequest.activityType,
    audience = bazaarEventRequest.audience,
    topics = bazaarEventRequest.topics,
    meetings = bazaarEventRequest.meetings,
    dates = bazaarEventRequest.dates,
    requiredResources = bazaarEventRequest.requiredResources,
    requiredSpaces = bazaarEventRequest.requiredSpaces,
    maxParticipants = bazaarEventRequest.maxParticipants,
    programDetails = bazaarEventRequest.programDetails,
    valueDetails = bazaarEventRequest.valueDetails,
    motivation = bazaarEventRequest.motivation,
    funding = bazaarEventRequest.funding,
    isOrganizer = bazaarEventRequest.isOrganizer,
    guests = bazaarEventRequest.guests,
    bookingRequired = bazaarEventRequest.bookingRequired
  )


  case class BazaarResearchRequest(
    title: String,
    topics: Seq[Topic],
    organizationName: Option[String],
    valueDetails: String,
    motivation: String,
    requiredResources: String,
    positions: Seq[BazaarResearchRole],
    deadline: Int,
    duration: Int)

  implicit val bazaarResearchRequestReads: Reads[BazaarResearchRequest] = (
    (JsPath \ "title").read[String](validateShortText()) and
      (JsPath \ "topics").read[Seq[Topic]](validateTopics) and
      (JsPath \ "organizationName").readNullable[String](validateShortText(required = false)) and
      (JsPath \ "valueDetails").read[String](validateLongText()) and
      (JsPath \ "motivation").read[String](validateLongText()) and
      (JsPath \ "requiredResources").read[String](validateMidText()) and
      (JsPath \ "positions").read[Seq[BazaarResearchRole]](minLength[Seq[BazaarResearchRole]](1)) and
      (JsPath \ "deadline").read[Int](min(1)) and
      (JsPath \ "duration").read[Int](min(1))
    )(BazaarResearchRequest)

  def researchRequestToModel(id: Long, user: User, bazaarResearchRequest: BazaarResearchRequest) = BazaarResearch(
    id = id,
    title = bazaarResearchRequest.title,
    creator = user,
    topics = bazaarResearchRequest.topics,
    organizationName = bazaarResearchRequest.organizationName,
    valueDetails = bazaarResearchRequest.valueDetails,
    motivation = bazaarResearchRequest.motivation,
    requiredResources = bazaarResearchRequest.requiredResources,
    positions = bazaarResearchRequest.positions,
    deadline = bazaarResearchRequest.deadline,
    duration = bazaarResearchRequest.duration
  )

}


@Singleton
class BazaarIdeasController @Inject()(
  override val controllerComponents: ApiControllerComponents,
  bazaarIdeaService: BazaarIdeaService
) extends ApiController {

  import BazaarIdeasController._

  private def includeDisabledIdeas(implicit request: RequestType): Boolean =
    request.authInfo.clientId.getOrElse("user") == "admin"

  def index = AllScope { implicit request =>
    request.getQueryString("search") match {
      case Some(search) if search.nonEmpty =>
        bazaarIdeaService.search(request.authInfo.user.id, search, includeDisabledIdeas).map { ideas =>
          Ok(Json.obj("ideas" -> ideas))
        }
      case _ =>
        bazaarIdeaService.all(request.authInfo.user.id, includeDisabledIdeas) map {
          case (teach, learn, events, research) =>
            Ok(Json.obj(
              "teach" -> teach,
              "learn" -> learn,
              "event" -> events,
              "research" -> research
            ))
        }
    }
  }


  def allSlim = AllScope { implicit request =>
    bazaarIdeaService.allSlim(request.authInfo.user.id, includeDisabledIdeas)
      .map(ideas => Ok(Json.obj("ideas" -> ideas)))
  }


  private def okIdeas(ideas: Seq[BazaarIdea]) = Ok(Json.obj("ideas" -> ideas))

  def indexTeach = AllScope { implicit request =>
    bazaarIdeaService.allTeach(request.authInfo.user.id, includeDisabledIdeas) map okIdeas
  }

  def indexLearn = AllScope { implicit request =>
    bazaarIdeaService.allLearn(request.authInfo.user.id, includeDisabledIdeas) map okIdeas
  }

  def indexEvent = AllScope { implicit request =>
    bazaarIdeaService.allEvent(request.authInfo.user.id, includeDisabledIdeas) map okIdeas
  }

  def indexResearch = AllScope { implicit request =>
    bazaarIdeaService.allResearch(request.authInfo.user.id, includeDisabledIdeas) map okIdeas
  }


  private def favoritesForUser(userId: Long)(implicit request: RequestType) =
    bazaarIdeaService.favorites(userId, includeDisabledIdeas) map okIdeas

  def favorites(userId: Long) = AdminScope { implicit request =>
    favoritesForUser(userId)
  }

  def myFavorites = AllScope { implicit request =>
    favoritesForUser(request.authInfo.user.id)
  }


  def showTeach(id: Long) = AllScope { implicit request =>
    bazaarIdeaService.findTeach(id, request.authInfo.user.id, includeDisabledIdeas)
      .map(idea => resultJsonFromServiceReply(idea))
  }

  def showLearn(id: Long) = AllScope { implicit request =>
    bazaarIdeaService.findLearn(id, request.authInfo.user.id, includeDisabledIdeas)
      .map(idea => resultJsonFromServiceReply(idea))
  }

  def showEvent(id: Long) = AllScope { implicit request =>
    bazaarIdeaService.findEvent(id, request.authInfo.user.id, includeDisabledIdeas)
      .map(idea => resultJsonFromServiceReply(idea))
  }

  def showResearch(id: Long) = AllScope { implicit request =>
    bazaarIdeaService.findResearch(id, request.authInfo.user.id, includeDisabledIdeas)
      .map(idea => resultJsonFromServiceReply(idea))
  }



  private def create[T <: BazaarIdea, R](requestToModel: (Long, User, R) => T, create: T => Future[T])(implicit fmt: Reads[R]) = {
    AllScope { implicit request =>
      jsonOrBadRequest { json =>
        Json.fromJson[R](json).fold(
          defaultErrorHandler,
          createBazaarIdea => {
            val bazaarIdea = requestToModel(0, request.authInfo.user, createBazaarIdea)
            create(bazaarIdea).map(idea => Ok(Json.toJson(idea)))
          }
        )
      }
    }
  }

  private def update[T <: BazaarIdea, R](
    id: Long, requestToModel: (Long, User, R) => T, update: (T, Boolean) => Future[ServiceRep[T]]
  )(implicit fmt: Reads[R]) = {
    AllScope { implicit request =>
      jsonOrBadRequest { json =>
        Json.fromJson[R](json).fold(
          defaultErrorHandler,
          bazaarIdeaRequest => {
            val bazaarIdea = requestToModel(id, request.authInfo.user, bazaarIdeaRequest)
            update(bazaarIdea, includeDisabledIdeas).map(resultJsonFromServiceRep[T])
          }
        )
      }
    }
  }


  def createLearn = create[BazaarLearn, BazaarLearnRequest](learnRequestToModel, bazaarIdeaService.create)
  def updateLearn(id: Long) = update[BazaarLearn, BazaarLearnRequest](id, learnRequestToModel, bazaarIdeaService.update)

  def createTeach = create[BazaarTeach, BazaarTeachRequest](teachRequestToModel, bazaarIdeaService.create)
  def updateTeach(id: Long) = update[BazaarTeach, BazaarTeachRequest](id, teachRequestToModel, bazaarIdeaService.update)

  def createEvent = create[BazaarEvent, BazaarEventRequest](eventRequestToModel, bazaarIdeaService.create)
  def updateEvent(id: Long) = update[BazaarEvent, BazaarEventRequest](id, eventRequestToModel, bazaarIdeaService.update)

  def createResearch = create[BazaarResearch, BazaarResearchRequest](researchRequestToModel, bazaarIdeaService.create)
  def updateResearch(id: Long) = update[BazaarResearch, BazaarResearchRequest](id, researchRequestToModel, bazaarIdeaService.update)

}
