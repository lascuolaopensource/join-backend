package controllers.api

import controllers.api.ApiController.ApiControllerComponents
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import services.CitiesService

import scala.concurrent.Future


@Singleton
class CitiesController @Inject()(
  override val controllerComponents: ApiControllerComponents,
  citiesService: CitiesService
) extends ApiController {

  def index(term: String) = AllScope { _ =>
    val cities = citiesService.search(term)
    Future.successful(Ok(Json.obj("cities" -> cities)))
  }

}
