package models.daos.slick

import models.Image
import models.daos.slick.tables.SlickImageGalleryTables
import models.daos.slick.tables.SlickImageGalleryTables.DBImageGalleryImage

import scala.concurrent.ExecutionContext


private[slick] trait SlickImageGalleryHelpers extends SlickImageGalleryTables {

  import models.Implicits.DefaultWithId._
  import profile.api._

  implicit val ec: ExecutionContext

  protected def updateGalleryImages(galleryId: Long, images: Seq[Image]) =
    for {
      _ <- getDeletableIds(images) match {
        case Seq() => DBIO.successful(())
        case deletableIds => ImageGalleryImageQuery.filter(_.id inSet deletableIds).delete
      }

      creatable = getCreatable(images)
      newIds <- (ImageGalleryImageQuery returning ImageGalleryImageQuery.map(_.id)) ++= creatable.map { image =>
        DBImageGalleryImage(0, galleryId, image.extension)
      }

      existing = getExisting(images)
      _ <- DBIO.sequence {
        existing.map { image =>
          ImageGalleryImageQuery.filter(_.id === image.id).update(DBImageGalleryImage(image.id, galleryId, image.extension))
        }
      }
    } yield existing ++ zipWithIds(creatable, newIds)

}
