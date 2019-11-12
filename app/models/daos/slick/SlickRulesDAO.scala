package models.daos.slick

import javax.inject.Inject
import models.daos.RulesDAO
import models.daos.slick.tables.SlickRulesTable.DBRules
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext


class SlickRulesDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit val ec: ExecutionContext
) extends RulesDAO with SlickDAO {

  import profile.api._


  override def get = db.run(RulesQuery.map(_.text).result.headOption)

  override def set(rules: String) =
    db.run(RulesQuery.insertOrUpdate(DBRules(rules))).map(_ > 0)

}
