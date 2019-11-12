package models.daos.slick.tables

import models.daos.slick.tables.SlickRulesTable.DBRules


private[slick] object SlickRulesTable {

  case class DBRules(
    text: String,
    uniqueColumn: Boolean = true)

}

private[slick] trait SlickRulesTable extends SlickTable {

  import profile.api._

  protected class RulesTable(tag: Tag) extends Table[DBRules](tag, "rules") {
    def text: Rep[String] = column("text")
    def uniqueColumn: Rep[Boolean] = column("unique_column", O.PrimaryKey)
    override def * = (text, uniqueColumn).mapTo[DBRules]
  }

  protected val RulesQuery = TableQuery[RulesTable]

}
