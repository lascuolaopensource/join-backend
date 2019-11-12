package services

import java.sql.Date

import javax.inject.Inject
import models.{FablabMachine, FablabQuotation, FablabReservation, FablabReservationFlat}
import models.daos.FablabDAO

import scala.concurrent.{ExecutionContext, Future}


trait FablabService extends Service {

  def allMachines: Future[Seq[FablabMachine]]
  def findMachine(id: Long): Future[ServiceReply[FablabMachine, NotFound]]
  def createMachine(machine: FablabMachine): Future[FablabMachine]
  def updateMachine(machine: FablabMachine): Future[ServiceReply[FablabMachine, NotFound]]
  def deleteMachine(id: Long): Future[ServiceReply[Unit, NotFound]]

  def allReservations(machineId: Option[Long] = None, future: Boolean = false): Future[Seq[FablabReservation]]
  def reservationsByDate(from: Date, to: Date): Future[Seq[FablabReservationFlat]]
  def userReservations(userId: Long, future: Boolean = false): Future[Seq[FablabReservation]]
  def createReservation(reservation: FablabReservation): Future[ServiceReply[FablabReservation, NotFound]]
  def deleteReservation(id: Long, userId: Option[Long] = None): Future[ServiceReply[Unit, _ >: NotFound with NotAuthorized <: ServiceError]]
  def deleteReservationTime(id: Long, machineId: Long, date: Date, hour: Int): Future[ServiceReply[Unit, NotFound]]

  def allQuotations: Future[Seq[FablabQuotation]]
  def createQuotation(fablabQuotation: FablabQuotation): Future[ServiceReply[FablabQuotation, NotFound]]
  def deleteQuotation(id: Long): Future[ServiceReply[Unit, NotFound]]
  def updateUndertakenQuotation(id: Long, undertaken: Boolean): Future[ServiceReply[Unit, NotFound]]

}


class FablabServiceImpl @Inject()(fablabDAO: FablabDAO)(implicit ec: ExecutionContext) extends FablabService {

  import Service.Ops._


  override def allMachines = fablabDAO.allMachines

  override def findMachine(id: Long) = fablabDAO findMachine id map toNotFound

  override def createMachine(machine: FablabMachine) = fablabDAO createMachine machine

  override def updateMachine(machine: FablabMachine) = fablabDAO updateMachine machine map toNotFound

  override def deleteMachine(id: Long) = fablabDAO deleteMachine id map toNotFound


  override def allReservations(machineId: Option[Long], future: Boolean) =
    fablabDAO allReservations (machineId, future)

  override def reservationsByDate(from: Date, to: Date) =
    fablabDAO.reservationsByDate(from, to)

  override def userReservations(userId: Long, future: Boolean) =
    fablabDAO userReservations (userId, future)

  override def createReservation(reservation: FablabReservation) =
    fablabDAO createReservation reservation map toNotFound

  override def deleteReservation(id: Long, userId: Option[Long]) = for {
    ok <- userId match {
      case Some(uid) => fablabDAO.userOwnsReservation(id, uid)
      case None => Future.successful(true)
    }
    result <- if (ok) fablabDAO.deleteReservation(id).map(_.toNotFound)
              else Service.futureFailed(NotAuthorized())
  } yield result

  override def deleteReservationTime(id: Long, machineId: Long, date: Date, hour: Int) =
    fablabDAO.deleteReservationTime(id, machineId, date, hour).map(toNotFound)


  override def allQuotations = fablabDAO.allQuotations

  override def createQuotation(fablabQuotation: FablabQuotation) =
    fablabDAO createQuotation fablabQuotation map (_.toNotFound)

  override def deleteQuotation(id: Long) = fablabDAO deleteQuotation id map toNotFound

  override def updateUndertakenQuotation(id: Long, undertaken: Boolean) =
    fablabDAO.updateUndertakenQuotation(id, undertaken).map(toNotFound)

}

