package models.daos.slick.tables

import slick.lifted.ProvenShape


trait SlickBazaarAbstractIdeaTable extends SlickTable {

  import profile.api._

  case class DBBazaarAbstractIdea(
    id: Long,
    requiredResources: Option[String],
    maxParticipants: Int,
    programDetails: String,
    days: Option[Int],
    recurringDays: Option[Int],
    recurringEvery: Option[Int],
    recurringEntity: Option[Int],
    hoursPerMeeting: Option[Int]
  )

  protected class BazaarAbstractIdeaTable(tag: Tag) extends Table[DBBazaarAbstractIdea](tag, "bazaar_abstract_idea") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def requiredResources: Rep[Option[String]] = column("required_resources")
    def maxParticipants: Rep[Int] = column("max_participants")
    def programDetails: Rep[String] = column("program_details")
    def days: Rep[Option[Int]] = column("days")
    def recurringDays: Rep[Option[Int]] = column("recurring_days")
    def recurringEvery: Rep[Option[Int]] = column("recurring_every")
    def recurringEntity: Rep[Option[Int]] = column("recurring_entity")
    def hoursPerMeeting: Rep[Option[Int]] = column("hours_per_meeting")
    override def * : ProvenShape[DBBazaarAbstractIdea] =
      (id, requiredResources, maxParticipants, programDetails,
        days, recurringDays, recurringEvery, recurringEntity, hoursPerMeeting) <>
        (DBBazaarAbstractIdea.tupled, DBBazaarAbstractIdea.unapply)
  }

  val BazaarAbstractIdeaQuery: TableQuery[BazaarAbstractIdeaTable] = TableQuery[BazaarAbstractIdeaTable]

}
