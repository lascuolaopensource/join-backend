package modules


import auth.{ApiTokenEndpoint, OAuthDataHandler, OAuthDataHandlerImpl}
import com.google.inject.AbstractModule
import models.User
import models.daos._
import models.daos.slick._
import net.codingwell.scalaguice.ScalaModule
import scalaoauth2.provider.TokenEndpoint
import services._

/**
  * The base Guice module.
  */
class BaseModule extends AbstractModule with ScalaModule {

  def configure(): Unit = {
    bind(classOf[java.time.Clock]).toInstance(java.time.Clock.systemDefaultZone)

    bind[SkillService].to[SkillServiceImpl]
    bind[MembershipService].to[MembershipServiceImpl]
    bind[BazaarIdeaService].to[BazaarIdeaServiceImpl]
    bind[TopicService].to[TopicServiceImpl]
    bind[BazaarPreferenceService].to[BazaarPreferenceServiceImpl]
    bind[BazaarCommentService].to[BazaarCommentServiceImpl]
    bind[ActivityService].to[ActivityServiceImpl]
    bind[ImageGalleryService].to[ImageGalleryServiceImpl]
    bind[FablabService].to[FablabServiceImpl]

    bind[S3Service].to[S3ServiceImpl]
    bind[BraintreeService].to[BraintreeServiceImpl]
    bind[CitiesService].to[JsonCitiesService]

    bind[UserDAO].to[SlickUserDAO]
    bind[SkillDAO].to[SlickSkillDAO]
    bind[MembershipDAO].to[SlickMembershipDAO]
    bind[MembershipTypeDAO].to[SlickMembershipTypeDAO]
    bind[BazaarIdeaDAO].to[SlickBazaarIdeaDAO]
    bind[BazaarPreferenceDAO].to[SlickBazaarPreferenceDAO]
    bind[BazaarCommentDAO].to[SlickBazaarCommentDAO]
    bind[ActivityTeachEventDAO].to[SlickActivityTeachEventDAO]
    bind[ActivityResearchDAO].to[SlickActivityResearchDAO]
    bind[ImageGalleryDAO].to[SlickImageGalleryDAO]
    bind[FablabDAO].to[SlickFablabDAO]

    bind[MailTokenDAO].to[SlickMailTokenDAO]
    bind[RulesDAO].to[SlickRulesDAO]

    bind[BazaarStatDAO].to[SlickBazaarStatDAO]
    bind[ActivityStatDAO].to[SlickActivityStatDAO]
    bind[FablabStatDAO].to[SlickFablabStatDAO]
    bind[UserStatDAO].to[SlickUserStatDAO]

    bind[AccessTokenDAO].to[SlickAccessTokenDAO]
    bind[TokenEndpoint].to[ApiTokenEndpoint]
    bind[OAuthDataHandler].to[OAuthDataHandlerImpl]

    bind[Option[User]].toInstance(None)
  }

}
