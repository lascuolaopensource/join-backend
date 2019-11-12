package services

import javax.inject.Inject
import fly.play.s3.BucketFile
import models.daos.ImageGalleryDAO
import models.{Image, ImageGallery}

import scala.concurrent.{ExecutionContext, Future}


trait ImageGalleryService extends Service {

  def fillGalleryUrls(gallery: ImageGallery): ImageGallery

  def all: Future[Seq[ImageGallery]]
  def search(name: String): Future[Seq[ImageGallery]]
  def find(id: Long): Future[ServiceReply[ImageGallery, NotFound]]

  def addImage(galleryId: Long, image: Image): Future[ServiceReply[Image, NotFound]]
  def updateImage(image: Image): Future[ServiceReply[Image, NotFound]]
  def deleteImage(imageId: Long): Future[ServiceReply[Unit, NotFound]]

}


class ImageGalleryServiceImpl @Inject()(
  s3Service: S3Service,
  imageGalleryDAO: ImageGalleryDAO
)(implicit ec: ExecutionContext) extends ImageGalleryService {

  import Service.Ops._

  @inline private def getImagePath(galleryId: Long, image: Image) = s"$galleryId/${image.filename}"
  @inline private def getImagePathWoExt(galleryId: Long, image: Image) = s"$galleryId/${image.id}"

  private def fillImageUrl(galleryId: Long)(image: Image) =
    image.copy(url = Some(s3Service.getGalleriesBucket.url(getImagePath(galleryId, image))))

  override def fillGalleryUrls(gallery: ImageGallery) =
    gallery.copy(images = gallery.images.map(fillImageUrl(gallery.id)))

  override def all = imageGalleryDAO.all.map(_.map(fillGalleryUrls))

  override def search(name: String) = imageGalleryDAO.search(name).map(_.map(fillGalleryUrls))

  override def find(id: Long) = imageGalleryDAO.find(id).map(_.map(fillGalleryUrls).toNotFound)


  private def uploadImageToS3(galleryId: Long, image: Image, maxSize: Int = 2048): Future[Unit] = {
    val bytes = s3Service.resizePic(image.data.get, maxSize)
    val bucketFile = BucketFile(getImagePath(galleryId, image), s"image/${image.extension}", bytes)
    s3Service.getGalleriesBucket + bucketFile
  }

  override def addImage(galleryId: Long, image: Image) = for {
    optImage <- imageGalleryDAO.addImage(galleryId, image)
    _ <- optImage match {
      case Some(dbImage) => uploadImageToS3(galleryId, image.copy(id = dbImage.id))
      case None => Future.successful(())
    }
  } yield optImage.map(fillImageUrl(galleryId)).toNotFound


  private def updateImageOnS3(galleryId: Long, image: Image, maxSize: Int = 2048): Future[Unit] = {
    for {
      files <- s3Service.getGalleriesBucket.list(getImagePathWoExt(galleryId, image))
      _ <- Future sequence (files map (file => s3Service.getGalleriesBucket - file.name))
      _ <- uploadImageToS3(galleryId, image, maxSize)
    } yield ()
  }

  override def updateImage(image: Image) = for {
    optImage <- imageGalleryDAO.updateImage(image)
    optGalleryId <- (optImage map {
      case (galleryId, _) =>
        updateImageOnS3(galleryId, image).map(_ => galleryId)
    }).toFutureOption
  } yield optGalleryId.map(fillImageUrl(_)(image)).toNotFound


  private def deleteImageFromS3(galleryId: Long, image: Image): Future[Unit] =
    s3Service.getGalleriesBucket - getImagePath(galleryId, image)

  override def deleteImage(imageId: Long) = for {
    optImage <- imageGalleryDAO.findImage(imageId)
    r <- optImage match {
      case Some((galleryId, image)) =>
        for {
          _ <- deleteImageFromS3(galleryId, image)
          r <- imageGalleryDAO.deleteImage(image.id)
        } yield r
      case None => Future.successful(false)
    }
  } yield r.toNotFound

}
