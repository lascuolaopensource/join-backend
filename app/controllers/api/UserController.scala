package controllers.api

import controllers.api.ApiController.{ApiControllerComponents, RequestType}
import javax.inject.{Inject, Singleton}
import models.Implicits._
import models.forms.{FormUtils, optionalList}
import models.{GivenCity, MailToken, OtherCity, SlimSkill}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints.{emailAddress, maxLength, nonEmpty}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsPath, JsValue, Json}
import services._

import scala.concurrent.Future
import scala.util.Try


@Singleton
class UserController @Inject()(
  override val controllerComponents: ApiControllerComponents,
  override val messagesApi: MessagesApi,
  userService: UserService,
  skillService: SkillService,
  membershipService: MembershipService,
  mailTokenService: MailTokenService,
  mailService: MailService,
  citiesService: CitiesService
) extends ApiController with I18nSupport {

  def me = AllScope { implicit request =>
    val futureUser = userService.find(request.authInfo.user.id)
      .flatMap(skillService.enrichWithSkills)
      .flatMap(membershipService.enrichWithMemberships(_, getLanguage))

    futureUser map (_.map(user => Ok(Json.toJson(user))) getOrElse {
      Logger.warn(s"Wrong user stored in auth ${request.authInfo.user.toString}")
      NotFound
    })
  }

  @inline def clientIsAdmin(implicit request: RequestType) =
    request.authInfo.clientId.contains("admin")

  def find(id: Long) = AllScope { implicit request =>
    val clientId = request.authInfo.clientId.get
    val userId = if (clientId == "user") Some(request.authInfo.user.id) else None
    val futureUser = userService.find(id, userId) flatMap skillService.enrichWithSkills flatMap { optUser =>
      if (clientId == "admin")
        membershipService.enrichWithMemberships(optUser, getLanguage)
      else
        Future.successful(optUser)
    }

    val thisUserWrites = if (clientIsAdmin) userFullWrites else userWrites
    futureUser map (_.map(user => Ok(Json.toJson(user)(thisUserWrites))) getOrElse NotFound)
  }

  def index = AllScope { implicit request =>
    val optName = request.getQueryString("name")
    val skills = request.queryString.get("skillId")
      .map(_.flatMap(s => Try(s.toLong).toOption))
      .getOrElse(Seq.empty)

    val optUsers = if (optName.isEmpty && skills.isEmpty) {
      if (clientIsAdmin)
        Some(userService.all)
      else
        None
    } else {
      val matchAll = request.queryString.contains("matchAll")
      Some(userService.search(request.authInfo.user.id, optName, skills, matchAll))
    }

    optUsers match {
      case Some(futureUsers) =>
        futureUsers flatMap { users =>
          val futuresWithSkills = users.map(skillService.enrichWithSkills)
          Future.sequence {
            if (clientIsAdmin)
              futuresWithSkills.map(_.flatMap(membershipService.enrichWithMemberships(_, getLanguage)))
            else
              futuresWithSkills
          }
        } map { users =>
          val thisUserWrites = if (clientIsAdmin) userFullWrites else userWrites
          Ok(Json.toJson(Map("users" -> users.map(Json.toJson(_)(thisUserWrites)))))
        }
      case None =>
        Future.successful(Unauthorized)
    }
  }

  private object UpdateProfileRequest {

    val form = Form(
      mapping(
        "firstName" -> nonEmptyText(maxLength = 32),
        "lastName" -> nonEmptyText(maxLength = 32),
        "email" -> text.verifying(nonEmpty, maxLength(255), emailAddress),
        "preferredLang" -> nonEmptyText,
        "telephone" -> optional(text(maxLength = 128)),
        "bio" -> optional(text(maxLength = 400)),
        "title" -> optional(text(maxLength = 255)),
        "city" -> optional(mapping(
          "city" -> optional(text(maxLength = 127)),
          "other" -> boolean
        )(CityData.apply)(CityData.unapply)),
        "skills" -> optionalList(mapping(
          "id" -> of[Long],
          "name" -> nonEmptyText(minLength = 3, maxLength = 255)
        )(SlimSkill.apply)(SlimSkill.unapply))
      )(Data.apply)(Data.unapply)
        .verifying("Language not valid", data => Seq("en", "it").contains(data.preferredLang))
        .verifying("City not valid", _.city.forall(_.city.forall(citiesService.validate)))
    )

    case class CityData(
      city: Option[String],
      other: Boolean)

    case class Data(
      firstName: String,
      lastName: String,
      email: String,
      preferredLang: String,
      telephone: Option[String],
      bio: Option[String],
      title: Option[String],
      city: Option[CityData],
      skills: Option[List[SlimSkill]])

    def bindForm(json: JsValue) = form.bind(FormUtils.fromJson(js = json))

  }

  def update = AllScope { implicit request =>
    jsonOrBadRequest { json =>
      UpdateProfileRequest.bindForm(json).fold(
        defaultJsonWithErrorsAction,
        profileData => {
          val user = request.authInfo.user.copy(
            firstName = profileData.firstName,
            lastName = profileData.lastName,
            email = profileData.email,
            preferredLang = profileData.preferredLang,
            telephone = profileData.telephone,
            bio = profileData.bio,
            title = profileData.title,
            city = profileData.city.map(_.city.map(GivenCity).getOrElse(OtherCity))
          )

          val oldEmail = request.authInfo.user.email

          val future = if (profileData.email == oldEmail)
            userService.save(user, profileData.skills).map((None, _))
          else {
            for {
              token <- mailTokenService.create(MailToken.changeEmail(profileData.email, user.id))
              updated <- userService.save(user.copy(email = oldEmail), profileData.skills)
            } yield (Some(token), updated)
          }

          future.flatMap {
            case (optToken, updated) =>
              optToken.map { token =>
                val url = controllers.routes.MailConfirmController.confirmChangeEmail(token.token).absoluteURL()
                mailService.changeEmail(updated, token.email, url)
              }
              skillService.enrichWithSkills(updated).map(u => Ok(Json.toJson(u)))
          }
        }
      )
    }
  }

  def favorite(id: Long) = AllScope { implicit request =>
    jsonOrBadRequest { json =>
      json.validate((JsPath \ "favorite").read[Boolean]).fold(
        defaultErrorHandler,
        favorite => {
          userService.favorite(request.authInfo.user.id, id, favorite).map { serviceReply =>
            resultJsonFromServiceReply(serviceReply.map(_ => favorite), Some("favorite"))
          }
        }
      )
    }
  }

  private def favoritesForUser(userId: Long) =
    userService.favorites(userId).map(users => Ok(Json.obj("users" -> users)))

  def favorites(id: Long) = AdminScope { _ =>
    favoritesForUser(id)
  }

  def myFavorites = AllScope { request =>
    favoritesForUser(request.authInfo.user.id)
  }

}
