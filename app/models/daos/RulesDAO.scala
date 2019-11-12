package models.daos

import scala.concurrent.Future


trait RulesDAO {

  def get: Future[Option[String]]
  def set(rules: String): Future[Boolean]

}
