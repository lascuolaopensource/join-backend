package models.daos.slick.tables

import java.sql.{Date, Timestamp}

import models.daos.slick.tables.SlickFablabTables._


private[slick] object SlickFablabTables {

  case class DBFablabMachine(
    id: Long,
    name: String,
    workArea: Option[String],
    maxHeight: Option[String],
    cutsMetal: Option[Boolean],
    cutsNonMetal: Option[Boolean],
    cutsMaterials: Option[String],
    engravesMetal: Option[Boolean],
    engravesNonMetal: Option[Boolean],
    engravesMaterials: Option[String],
    priceHour: Double,
    operator: Boolean,
    createdAt: Timestamp)

  case class DBFablabReservation(
    id: Long,
    userId: Long,
    operator: Boolean,
    createdAt: Timestamp)

  case class DBFablabReservationTime(
    fablabReservationId: Long,
    machineId: Long,
    date: Date,
    hour: Int)

  case class DBFablabQuotation(
    id: Long,
    userId: Long,
    realizationOf: String,
    undertaken: Boolean,
    createdAt: Timestamp)

}


private[slick] trait SlickFablabTables extends SlickTable {

  import profile.api._

  protected class FablabMachineTable(tag: Tag) extends Table[DBFablabMachine](tag, "fablab_machine") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def name: Rep[String] = column("name")
    def workArea: Rep[Option[String]] = column("work_area")
    def maxHeight: Rep[Option[String]] = column("max_height")
    def cutsMetal: Rep[Option[Boolean]] = column("cuts_metal")
    def cutsNonMetal: Rep[Option[Boolean]] = column("cuts_non_metal")
    def cutsMaterials: Rep[Option[String]] = column("cuts_materials")
    def engravesMetal: Rep[Option[Boolean]] = column("engraves_metal")
    def engravesNonMetal: Rep[Option[Boolean]] = column("engraves_non_metal")
    def engravesMaterials: Rep[Option[String]] = column("engraves_materials")
    def priceHours: Rep[Double] = column("price_hour")
    def operator: Rep[Boolean] = column("operator")
    def createdAt: Rep[Timestamp] = column("created_at")
    override def * = (id, name, workArea, maxHeight, cutsMetal, cutsNonMetal, cutsMaterials,
      engravesMetal, engravesNonMetal, engravesMaterials, priceHours, operator, createdAt) <>
      (DBFablabMachine.tupled, DBFablabMachine.unapply)
  }

  protected val FablabMachineQuery = TableQuery[FablabMachineTable]


  protected class FablabReservationTable(tag: Tag) extends Table[DBFablabReservation](tag, "fablab_reservation") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def userId: Rep[Long] = column("user_id")
    def operator: Rep[Boolean] = column("operator")
    def createdAt: Rep[Timestamp] = column("created_at")
    override def * = (id, userId, operator, createdAt) <>
      (DBFablabReservation.tupled, DBFablabReservation.unapply)
  }

  protected val FablabReservationQuery = TableQuery[FablabReservationTable]


  protected class FablabReservationTimeTable(tag: Tag) extends Table[DBFablabReservationTime](tag, "fablab_reservation_time") {
    def fablabReservationId: Rep[Long] = column("fablab_reservation_id")
    def machineId: Rep[Long] = column("machine_id")
    def date: Rep[Date] = column("date")
    def hour: Rep[Int] = column("hour")
    override def * = (fablabReservationId, machineId, date, hour) <>
      (DBFablabReservationTime.tupled, DBFablabReservationTime.unapply)
  }

  protected val FablabReservationTimeQuery = TableQuery[FablabReservationTimeTable]


  protected class FablabQuotationTable(tag: Tag) extends Table[DBFablabQuotation](tag, "fablab_quotation") {
    def id: Rep[Long] = column("id", O.AutoInc, O.PrimaryKey)
    def userId: Rep[Long] = column("user_id")
    def realizationOf: Rep[String] = column("realization_of")
    def undertaken: Rep[Boolean] = column("undertaken")
    def createdAt: Rep[Timestamp] = column("created_at")
    override def * =
      (id, userId, realizationOf, undertaken, createdAt).mapTo[DBFablabQuotation]
  }

  protected val FablabQuotationQuery = TableQuery[FablabQuotationTable]


  protected class FablabQuotationMachineTable(tag: Tag) extends Table[(Long, Long)](tag, "fablab_quotation_machine") {
    def quotationId: Rep[Long] = column("fablab_quotation_id")
    def machineId: Rep[Long] = column("fablab_machine_id")
    override def * = (quotationId, machineId)
  }

  protected val FablabQuotationMachineQuery = TableQuery[FablabQuotationMachineTable]

}
