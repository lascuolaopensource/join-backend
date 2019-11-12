package services

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import javax.imageio.ImageIO
import javax.inject.Inject
import javax.xml.bind.DatatypeConverter

import fly.play.s3.{Bucket, S3}
import net.coobird.thumbnailator.Thumbnails
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment, Mode}

import scala.util.hashing.MurmurHash3


trait S3Service extends Service {

  def hashString(s: String): String

  def getActivityImagesBucket: Bucket
  def getGalleriesBucket: Bucket

  def resizePic(base64Pic: String, maxSize: Int): Array[Byte]

}


class S3ServiceImpl @Inject()(environment: Environment, wsClient: WSClient, configuration: Configuration) extends S3Service {

  override def hashString(s: String) = MurmurHash3.stringHash(s).toHexString

  private val env = environment.mode match {
    case Mode.Dev | Mode.Test => "dev"
    case Mode.Prod => "prod"
  }

  private val s3 = S3.fromConfiguration(wsClient, configuration)

  override def getActivityImagesBucket: Bucket = s3.getBucket(s"sos-activity-images-$env")

  override def getGalleriesBucket: Bucket = s3.getBucket(s"sos-gallery-images-$env")

  override def resizePic(base64Pic: String, maxSize: Int): Array[Byte] = {
    val decodedBytes = DatatypeConverter.parseBase64Binary(base64Pic)
    val thumb = Thumbnails.of(new ByteArrayInputStream(decodedBytes))

    val (height, width) = {
      val image = ImageIO.read(new ByteArrayInputStream(decodedBytes))
      (image.getHeight, image.getWidth)
    }

    val rightSized = if (height > maxSize || width > maxSize)
      thumb.size(maxSize, maxSize)
    else thumb.size(width, height)

    val outputStream = new ByteArrayOutputStream()
    rightSized.toOutputStream(outputStream)
    outputStream.toByteArray
  }

}
