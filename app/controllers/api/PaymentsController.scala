package controllers.api

import controllers.api.ApiController.ApiControllerComponents
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import services.BraintreeService

import scala.concurrent.Future


@Singleton
class PaymentsController @Inject()(
  override val controllerComponents: ApiControllerComponents,
  braintreeService: BraintreeService
) extends ApiController {

  def clientToken = AllScope { _ =>
    Future.successful(Ok(Json.obj("token" -> braintreeService.generateClientToken)))
  }

}
