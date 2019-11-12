package models.daos.slick.tables

import models.Topic
import models.daos.slick.tables.SlickTopicTable.DBTopic
import slick.lifted.ProvenShape

import scala.language.implicitConversions


private[slick] object SlickTopicTable {

  case class DBTopic(id: Long, topic: String)

  implicit def dbTopicToTopic(dbTopic: DBTopic): Topic = Topic(
    id = dbTopic.id,
    topic = dbTopic.topic
  )

}


private[slick] trait SlickTopicTable extends SlickTable {

  import profile.api._

  protected class TopicTable(tag: Tag) extends Table[DBTopic](tag, "topic") {
    def id: Rep[Long] = column("id", O.AutoInc, O.PrimaryKey)
    def topic: Rep[String] = column("topic")
    override def * : ProvenShape[DBTopic] = (id, topic) <> (DBTopic.tupled, DBTopic.unapply)
  }

  val TopicQuery: TableQuery[TopicTable] = TableQuery[TopicTable]

}
