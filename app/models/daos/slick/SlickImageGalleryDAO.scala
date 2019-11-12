package models.daos.slick

import javax.inject.Inject
import models.Image
import models.daos.ImageGalleryDAO
import models.daos.slick.SlickImageGalleryDAO._
import models.daos.slick.tables.SlickImageGalleryTables._
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext


private object SlickImageGalleryDAO {

  sealed trait JoinedQueryType
  final case object JoinedNoFilter extends JoinedQueryType
  final case class JoinedFilterName(name: String) extends JoinedQueryType
  final case class JoinedFilterId(id: Long) extends JoinedQueryType

}


class SlickImageGalleryDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit val ec: ExecutionContext)
  extends ImageGalleryDAO with SlickDAO {

  import profile.api._

  private def joinedQuery(queryType: JoinedQueryType = JoinedNoFilter) =
    (queryType match {
      case JoinedNoFilter => ImageGalleryQuery
      case JoinedFilterName(name) => ImageGalleryQuery.filter(_.name like s"%$name%")
      case JoinedFilterId(id) => ImageGalleryQuery.filter(_.id === id)
    }).joinLeft(ImageGalleryImageQuery).on(_.id === _.galleryId)

  private def joinedResults(results: Seq[(DBImageGallery, Option[DBImageGalleryImage])]) =
    results.groupBy(_._1).map {
      case (dbGallery, joined) =>
        dbToImageGallery(dbGallery, joined.flatMap(_._2))
    }.toSeq

  override def all = db run joinedQuery().result map joinedResults

  override def search(name: String) = db run joinedQuery(JoinedFilterName(name)).result map joinedResults

  override def find(id: Long) = db run joinedQuery(JoinedFilterId(id)).result map (joinedResults(_).headOption)

  override def addImage(galleryId: Long, image: Image) = db run (for {
    exists <- ImageGalleryQuery.filter(_.id === galleryId).exists.result
    optImage <- {
      if (exists)
        for {
          newId <- (ImageGalleryImageQuery returning ImageGalleryImageQuery.map(_.id)) +=
            DBImageGalleryImage(0, galleryId, image.extension)
        } yield Some(Image(newId, image.extension))
      else DBIO.successful(None)
    }
  } yield optImage)

  override def findImage(imageId: Long) =
    db run ImageGalleryImageQuery.filter(_.id === imageId).result.headOption map (_.map { image =>
      (image.galleryId, Image(image.id, image.imageExt))
    })

  override def updateImage(image: Image) = db run (for {
    optImage <- ImageGalleryImageQuery.filter(_.id === image.id).result.headOption
    r <- {
      if (optImage.isDefined)
        ImageGalleryImageQuery.filter(_.id === image.id).map(_.imageExt).update(image.extension)
      else
        DBIO.successful(0)
    }
  } yield optImage match {
    case Some(dbImage) if r == 1 => Some((dbImage.galleryId, image))
    case _ => None
  })

  override def deleteImage(imageId: Long) = db run (for {
    r <- ImageGalleryImageQuery.filter(_.id === imageId).delete
  } yield r == 1)

}
