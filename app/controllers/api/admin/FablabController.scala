package controllers.api.admin

import java.sql.Date

import controllers.api.{ApiController, StatsController}
import controllers.api.ApiController.ApiControllerComponents
import javax.inject.{Inject, Singleton}
import models.FablabMachine
import models.Implicits._
import play.api.libs.json.Json
import services.{FablabService, FablabStatService}

import scala.concurrent.Future


private object FablabController {

  case class DeleteReservationTime(
    machineId: Long,
    date: Date,
    hour: Int)

  implicit val deleteReservationTimeReads = Json.reads[DeleteReservationTime]

}


@Singleton()
class FablabController @Inject()(
  override val controllerComponents: ApiControllerComponents,
  fablabService: FablabService,
  fablabStatService: FablabStatService
) extends ApiController with StatsController {

  import StatsController._
  import FablabController._

  def reservations = AdminScope { implicit request =>
    getDateFromReq("from", "to")
      .map { case (from, to) =>
        fablabService.reservationsByDate(from, to).map(toOk("reservations"))
      }
      .getOrElse(Future.successful(BadRequest))
  }

  def deleteReservationTime(id: Long) = AdminScope { implicit request =>
    validateOrBadRequest[DeleteReservationTime]() { data =>
      val DeleteReservationTime(machineId, date, hour) = data
      fablabService.deleteReservationTime(id, machineId, date, hour)
        .map(resultJsonFromServiceReply(_))
    }
  }

  def createMachine = AdminScope { implicit request =>
    validateOrBadRequest[FablabMachine]() { machine =>
      fablabService.createMachine(machine).map(toOk(_))
    }
  }

  def updateMachine(id: Long) = AdminScope { implicit request =>
    validateOrBadRequest[FablabMachine]() { machine =>
      fablabService.updateMachine(machine.copy(id = id)).map(resultJsonFromServiceReply(_))
    }
  }

  def deleteMachine(id: Long) = AdminScope { _ =>
    fablabService.deleteMachine(id).map(resultJsonFromServiceReply(_))
  }


  def allQuotations = AdminScope { _ =>
    fablabService.allQuotations map (quotations => Ok(Json obj "quotations" -> quotations))
  }

  def deleteQuotation(id: Long) = AdminScope { _ =>
    fablabService deleteQuotation id map (resultJsonFromServiceReply(_))
  }

  def updateUndertakenQuotation(id: Long) = AdminScope { request =>
    val undertaken = request.queryString.contains("undertaken")
    fablabService.updateUndertakenQuotation(id, undertaken).map(resultJsonFromServiceReply(_))
  }


  def topMachinesByUsage = AdminScope { implicit request =>
    processStatsRequest("from", "to", fablabStatService.topMachinesByUsage(_, _))
  }

  def counts = AdminScope { implicit request =>
    processStatsRequest("from", "to", fablabStatService.counts)
  }

}
