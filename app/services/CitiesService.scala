package services

import java.io.FileInputStream

import javax.inject.Singleton
import play.api.libs.json.Json


trait CitiesService extends Service {

  def search(term: String, limit: Int = 10): Seq[String]
  def validate(city: String): Boolean

}


object JsonCitiesService {

  private case class CityModel(nome: String)

  private val cities = {
    implicit val cityModelReads = Json.reads[CityModel]
    Json.parse(new FileInputStream("app/assets/json/comuni.json"))
      .as[Seq[CityModel]].map(_.nome).toStream
  }

}


@Singleton
class JsonCitiesService extends CitiesService {

  import JsonCitiesService._

  override def search(term: String, limit: Int): Seq[String] = {
    val termLower = term.toLowerCase
    cities filter (_.toLowerCase startsWith termLower) take limit
  }

  override def validate(city: String): Boolean = cities contains city

}
