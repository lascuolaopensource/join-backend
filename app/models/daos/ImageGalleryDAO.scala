package models.daos

import models.{Image, ImageGallery}

import scala.concurrent.Future


trait ImageGalleryDAO {

  def all: Future[Seq[ImageGallery]]
  def search(name: String): Future[Seq[ImageGallery]]
  def find(id: Long): Future[Option[ImageGallery]]

  def addImage(galleryId: Long, image: Image): Future[Option[Image]]
  def findImage(imageId: Long): Future[Option[(Long, Image)]]
  def updateImage(image: Image): Future[Option[(Long, Image)]]
  def deleteImage(imageId: Long): Future[Boolean]

}
