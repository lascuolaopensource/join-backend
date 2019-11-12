package controllers.api.admin

import controllers.api.ApiController
import controllers.api.ApiController.ApiControllerComponents
import javax.inject.{Inject, Singleton}
import models.Image
import models.Implicits._
import play.api.libs.json._
import services.ImageGalleryService


@Singleton
class ImageGalleriesController @Inject()(
  override val controllerComponents: ApiControllerComponents,
  imageGalleryService: ImageGalleryService
) extends ApiController {

  // TODO: refactor addImage and updateImage

  def addImage(galleryId: Long) = AdminScope { implicit request =>
    jsonOrBadRequest { json =>
      json.validate[Image]
        .filter(JsError(JsPath \ "data", "is missing"))(_.data.isDefined)
        .fold(
          defaultErrorHandler,
          image => imageGalleryService.addImage(galleryId, image)
              .map(resultJsonFromServiceReply[Image, services.NotFound])
        )
    }
  }

  def updateImage(imageId: Long) = AdminScope { implicit request =>
    jsonOrBadRequest { json =>
      json.validate[Image]
        .filter(JsError(JsPath \ "data", "is missing"))(_.data.isDefined)
        .fold(
          defaultErrorHandler,
          image => imageGalleryService.updateImage(image)
            .map(resultJsonFromServiceReply[Image, services.NotFound])
        )
    }
  }

  def deleteImage(imageId: Long) = AdminScope { _ =>
    imageGalleryService.deleteImage(imageId)
      .map(resultJsonFromServiceReply[Unit, services.NotFound])
  }

}
