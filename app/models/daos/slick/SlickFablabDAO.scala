package models.daos.slick

import java.sql.Date

import javax.inject.Inject
import models._
import models.daos.FablabDAO
import models.daos.slick.tables.SlickFablabTables.{DBFablabMachine, DBFablabQuotation, DBFablabReservation, DBFablabReservationTime}
import models.daos.slick.tables.SlickUserTable.DBUser
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext


private object SlickFablabDAO {

  def dbMachineToMachine(operatorCost: Double)(dbMachine: DBFablabMachine) = FablabMachine(
    id = dbMachine.id,
    name = dbMachine.name,
    workArea = dbMachine.workArea,
    maxHeight = dbMachine.maxHeight,
    cutsMetal = dbMachine.cutsMetal,
    cutsNonMetal = dbMachine.cutsNonMetal,
    cutsMaterials = dbMachine.cutsMaterials,
    engravesMetal = dbMachine.engravesMetal,
    engravesNonMetal = dbMachine.engravesNonMetal,
    engravesMaterials = dbMachine.engravesMaterials,
    priceHour = dbMachine.priceHour,
    operator = dbMachine.operator,
    operatorCost = Some(operatorCost),
    createdAt = dbMachine.createdAt)

  def machineToDB(machine: FablabMachine, id: Long) = DBFablabMachine(
    id = id,
    name = machine.name,
    workArea = machine.workArea,
    maxHeight = machine.maxHeight,
    cutsMetal = machine.cutsMetal,
    cutsNonMetal = machine.cutsNonMetal,
    cutsMaterials = machine.cutsMaterials,
    engravesMetal = machine.engravesMetal,
    engravesNonMetal = machine.engravesNonMetal,
    engravesMaterials = machine.engravesMaterials,
    priceHour = machine.priceHour,
    operator = machine.operator,
    createdAt = machine.createdAt)

  def machineToDB(machine: FablabMachine): DBFablabMachine = machineToDB(machine, machine.id)


  def dbReservationToReservation(times: Seq[(DBFablabReservationTime, DBFablabMachine)])
    (tpl: (DBFablabReservation, DBUser)): FablabReservation = tpl match {
      case (reservation, user) =>
        val times_ = times.filter(_._1.fablabReservationId == reservation.id)
        val machine = times_.head._2
        FablabReservation(
          id = reservation.id,
          user = user.toShort,
          machine = SlimMachine(machine.id, machine.name),
          times = times_.map { case (dbTime, _) =>
            FablabReservationTime(dbTime.date, dbTime.hour)
          },
          operator = reservation.operator,
          createdAt = reservation.createdAt)
    }

}


class SlickFablabDAO @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  configuration: Configuration)(
  implicit val ec: ExecutionContext)
  extends FablabDAO with SlickDAO {

  import SlickFablabDAO._
  import profile.api._

  private val operatorCost = configuration.getOptional[Double]("fablab.operatorCost").getOrElse(0.0)

  private def queryMachine(id: Long) = FablabMachineQuery.filter(_.id === id)

  override def allMachines =
    db.run(FablabMachineQuery.sortBy(_.createdAt.desc).result).map(_.map(dbMachineToMachine(operatorCost)))

  override def findMachine(id: Long) = db run queryMachine(id).result.headOption map (_ map dbMachineToMachine(operatorCost))

  override def createMachine(machine: FablabMachine) = {
    val action = (FablabMachineQuery returning FablabMachineQuery.map(_.id)) += machineToDB(machine, 0)
    db run action map (id => machine.copy(id = id))
  }

  override def updateMachine(machine: FablabMachine) =
    db run queryMachine(machine.id).update(machineToDB(machine)) map { affected =>
      if (affected == 1) Some(machine)
      else None
    }

  override def deleteMachine(id: Long) = db run (for {
    reservationIds <- FablabReservationTimeQuery.filter(_.machineId === id).map(_.fablabReservationId).result
    _ <- FablabReservationTimeQuery.filter(_.machineId === id).delete
    _ <- FablabReservationQuery.filter(_.id inSet reservationIds).delete
    n <- queryMachine(id).delete
  } yield n == 1).transactionally


  private val queryReservations = FablabReservationQuery join UsersQuery on (_.userId === _.id)

  private def queryTimes(future: Boolean) = {
    val q = if (future) FablabReservationTimeQuery.filter(_.date > yesterday())
            else FablabReservationTimeQuery
    q.join(FablabMachineQuery).on(_.machineId === _.id)
  }

  override def allReservations(machineId: Option[Long], future: Boolean) = {
    val timesQ = queryTimes(future)

    val query = for {
      times <- machineId.map(mid => timesQ.filter(_._1.machineId === mid)).getOrElse(timesQ).result
      tuples <- queryReservations.filter(_._1.id inSet times.map(_._1.fablabReservationId).toSet).result
    } yield (tuples, times)

    db run query map {
      case (tuples, times) =>
        tuples map dbReservationToReservation(times)
    }
  }

  override def reservationsByDate(from: Date, to: Date) = {
    val q = for {
      t <- FablabReservationTimeQuery if t.date >= from && t.date <= to
      m <- FablabMachineQuery if m.id === t.machineId
      r <- FablabReservationQuery if r.id === t.fablabReservationId
      u <- UsersQuery if u.id === r.userId
    } yield (t, m, r, u)

    db.run(q.sortBy(_._1.date.asc).result).map(_.map {
      case (t, m ,r, u) =>
        FablabReservationFlat(
          id = r.id,
          user = u.toContacts,
          machine = SlimMachine(m.id, m.name),
          time = FablabReservationTime(t.date, t.hour),
          operator = r.operator)
    })
  }

  override def userReservations(userId: Long, future: Boolean) = {
    val query = for {
      tuples <- queryReservations.filter(_._2.id === userId).result
      times <- queryTimes(future).filter(_._1.fablabReservationId inSet tuples.map(_._1.id)).result
    } yield (tuples, times)

    db run query map {
      case (tuples, times) =>
        tuples.flatMap {
          case tpl@(r, _) =>
            if (times.exists(_._1.fablabReservationId == r.id))
              Some(dbReservationToReservation(times)(tpl))
            else None
        }
    }
  }

  override def createReservation(reservation: FablabReservation) = {
    val now = currentTimestamp()
    val action = queryMachine(reservation.machine.id).map(_.id).exists.result.flatMap { exists =>
      if (exists)
        for {
          id <- (FablabReservationQuery returning FablabReservationQuery.map(_.id)) += DBFablabReservation(
            id = reservation.id,
            userId = reservation.user.id,
            operator = reservation.operator,
            createdAt = now)
          _ <- FablabReservationTimeQuery ++= reservation.times.map(t =>
            DBFablabReservationTime(id, reservation.machine.id, t.date, t.hour))
        } yield Some(id)
      else
        DBIO.successful(None)
    }

    db run action.transactionally map (_ map (id => reservation.copy(id = id, createdAt = now)))
  }

  override def userOwnsReservation(id: Long, userId: Long) =
    db run FablabReservationQuery.filter(r => r.id === id && r.userId === userId).map(_.id).exists.result

  override def deleteReservation(id: Long) = db run (for {
      _ <- FablabReservationTimeQuery.filter(_.fablabReservationId === id).delete
      n <- FablabReservationQuery.filter(_.id === id).delete
    } yield n == 1).transactionally

  override def deleteReservationTime(id: Long, machineId: Long, date: Date, hour: Int) = {
    val action = for {
      n <- FablabReservationTimeQuery.filter(t =>
        t.fablabReservationId === id && t.machineId === machineId &&
        t.date === date && t.hour === hour).delete
      m <- FablabReservationTimeQuery.filter(_.fablabReservationId === id).length.result
      _ <- {
        if (m == 0)
          FablabReservationQuery.filter(_.id === id).delete
        else DBIO.successful(0)
      }
    } yield n > 0

    db.run(action)
  }


  override def allQuotations = db run (for {
    quotations <- FablabQuotationQuery.join(UsersQuery).on(_.userId === _.id)
      .sortBy(_._1.createdAt.desc).result
    quotationMachines <- FablabQuotationMachineQuery
      .join(FablabMachineQuery).on(_.machineId === _.id)
      .map { case (link, machine) => (link.quotationId, machine) }.result
    quotationMachinesMap = quotationMachines.groupBy(_._1)
  } yield quotations.map {
    case (quotation, user) =>
      val machines = quotationMachinesMap.getOrElse(quotation.id, Seq())
      FablabQuotation(
        id = quotation.id,
        userId = quotation.userId,
        realizationOf = quotation.realizationOf,
        undertaken = quotation.undertaken,
        machines = machines.map { case (_, machine) => SlimMachine(machine.id, machine.name) },
        createdAt = quotation.createdAt,
        user = Some(user.toContacts))
  })

  override def createQuotation(fablabQuotation: FablabQuotation) = db run (for {
    machinesOk <- {
      if (fablabQuotation.machines.isEmpty)
        DBIO.successful(true)
      else
        (FablabMachineQuery.filter(_.id inSet fablabQuotation.machines.map(_.id))
          .map(_.id).length === fablabQuotation.machines.length).result
    }
    optQuotation <- {
      val now = currentTimestamp()
      if (machinesOk)
        for {
          id <- (FablabQuotationQuery returning FablabQuotationQuery.map(_.id)) += DBFablabQuotation(
            id = 0, userId = fablabQuotation.userId,
            realizationOf = fablabQuotation.realizationOf,
            undertaken = fablabQuotation.undertaken, createdAt = now)
          _ <- FablabQuotationMachineQuery ++= fablabQuotation.machines.map(m => (id, m.id))
        } yield Some(fablabQuotation.copy(id = id, createdAt = now))
      else DBIO.successful(None)
    }
  } yield optQuotation)

  override def deleteQuotation(id: Long) = db run (for {
    _ <- FablabQuotationMachineQuery.filter(_.quotationId === id).delete
    n <- FablabQuotationQuery.filter(_.id === id).delete
  } yield n == 1).transactionally

  override def updateUndertakenQuotation(id: Long, undertaken: Boolean) =
    db.run(FablabQuotationQuery.filter(_.id === id).map(_.undertaken).update(undertaken).map(_ == 1))

}
