package models.daos.slick.tables


private[tables] trait SlickActivityCommonTables extends SlickTable {

  import profile.api._

  protected abstract class SlickActivityTopicTable(tag: Tag, tableName: String)
    extends Table[(Long, Long)](tag, tableName)
  {
    def activityId: Rep[Long]
    def topicId: Rep[Long]
    override def * = (activityId, topicId)
  }

  protected abstract class SlickActivityFavoriteTable(tag: Tag, tableName: String)
    extends Table[(Long, Long)](tag, tableName)
  {
    def activityId: Rep[Long]
    def userId: Rep[Long]
    override def * = (activityId, userId)
  }

}
