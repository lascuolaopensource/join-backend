package models.daos.slick.tables

import models.{Image, ImageGallery}


private[slick] object SlickImageGalleryTables {

  case class DBImageGallery(
    id: Long,
    name: String)

  case class DBImageGalleryImage(
    id: Long,
    galleryId: Long,
    imageExt: String)

  def dbToImageGallery(gallery: DBImageGallery, images: Seq[DBImageGalleryImage]): ImageGallery =
    ImageGallery(
      id = gallery.id,
      images = images.map(image => Image(image.id, image.imageExt)),
      name = gallery.name
    )

}


private[slick] trait SlickImageGalleryTables extends SlickTable {

  import SlickImageGalleryTables._
  import profile.api._

  protected class ImageGalleryTable(tag: Tag) extends Table[DBImageGallery](tag, "image_gallery") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def name: Rep[String] = column("name")
    override def * = (id, name) <> (DBImageGallery.tupled, DBImageGallery.unapply)
  }

  protected val ImageGalleryQuery = TableQuery[ImageGalleryTable]


  protected class ImageGalleryImageTable(tag: Tag) extends Table[DBImageGalleryImage](tag, "image_gallery_image") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def galleryId: Rep[Long] = column("gallery_id")
    def imageExt: Rep[String] = column("image_ext")
    override def * = (id, galleryId, imageExt) <> (DBImageGalleryImage.tupled, DBImageGalleryImage.unapply)
  }

  protected val ImageGalleryImageQuery = TableQuery[ImageGalleryImageTable]

}
