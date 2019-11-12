package models.daos

import java.sql.Date

import models.{FablabMachine, FablabQuotation, FablabReservation, FablabReservationFlat}

import scala.concurrent.Future


trait FablabDAO {

  def allMachines: Future[Seq[FablabMachine]]
  def findMachine(id: Long): Future[Option[FablabMachine]]
  def createMachine(machine: FablabMachine): Future[FablabMachine]
  def updateMachine(machine: FablabMachine): Future[Option[FablabMachine]]
  def deleteMachine(id: Long): Future[Boolean]

  def allReservations(machineId: Option[Long] = None, future: Boolean = false): Future[Seq[FablabReservation]]
  def reservationsByDate(from: Date, to: Date): Future[Seq[FablabReservationFlat]]
  def userReservations(userId: Long, future: Boolean = false): Future[Seq[FablabReservation]]
  def createReservation(reservation: FablabReservation): Future[Option[FablabReservation]]
  def userOwnsReservation(id: Long, userId: Long): Future[Boolean]
  def deleteReservation(id: Long): Future[Boolean]
  def deleteReservationTime(id: Long, machineId: Long, date: Date, hour: Int): Future[Boolean]

  def allQuotations: Future[Seq[FablabQuotation]]
  def createQuotation(fablabQuotation: FablabQuotation): Future[Option[FablabQuotation]]
  def deleteQuotation(id: Long): Future[Boolean]
  def updateUndertakenQuotation(id: Long, undertaken: Boolean): Future[Boolean]

}
