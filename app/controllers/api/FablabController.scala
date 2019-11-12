package controllers.api

import controllers.api.ApiController.{ApiControllerComponents, RequestType}
import javax.inject.{Inject, Singleton}
import models.Implicits._
import models.{FablabQuotation, SlimMachine}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import services.FablabService


private object FablabController {

  case class QuotationRequest(
    realizationOf: String,
    machines: Seq[SlimMachine])

  implicit val quotationRequestReads: Reads[QuotationRequest] = (
    (JsPath \ "realizationOf").read[String](maxLength[String](1024)) and
    (JsPath \ "machines").read[Seq[SlimMachine]]
  )(QuotationRequest.apply _)

}


@Singleton()
class FablabController @Inject()(
  override val controllerComponents: ApiControllerComponents,
  fablabService: FablabService
) extends ApiController {

  import FablabController._

  def allMachines = AllScope { _ =>
    fablabService.allMachines map { machines =>
      Ok(Json obj "machines" -> machines)
    }
  }

  def findMachine(id: Long) = AllScope { _ =>
    fablabService findMachine id map (resultJsonFromServiceReply(_))
  }


  def allReservations = AllScope { request =>
    val future = request.getQueryString("future").isDefined
    fablabService.allReservations(future = future).map { reservations =>
      Ok(Json obj "reservations" -> reservations)
    }
  }

  def machineReservations(machineId: Long) = AllScope { request =>
    val future = request.getQueryString("future").isDefined
    fablabService.allReservations(Some(machineId), future).map { reservations =>
      Ok(Json obj "reservations" -> reservations)
    }
  }

  private def userReservationsAction(userId: Long)(implicit request: RequestType) = {
    val future = request.getQueryString("future").isDefined
    fablabService.userReservations(userId, future) map toOk("reservations")
  }

  def userReservations = AllScope { implicit request =>
    userReservationsAction(request.authInfo.user.id)
  }

  def otherUserReservations(userId: Long) = AdminScope { implicit request =>
    userReservationsAction(userId)
  }

  def createReservation(machineId: Long) = AllScope { implicit request =>
    jsonOrBadRequest { json =>
      json.validate(fablabReservationReads(machineId, request.authInfo.user.toShort)).fold(
        defaultErrorHandler,
        reservation =>
          fablabService.createReservation(reservation).map(resultJsonFromServiceReply(_))
      )
    }
  }

  def deleteReservation(id: Long) = AllScope { request =>
    val userId = if (request.authInfo.user.isAdmin) None else Some(request.authInfo.user.id)
    fablabService.deleteReservation(id, userId).map(resultJsonFromServiceReply(_))
  }


  def createQuotation = AllScope { implicit request =>
    jsonOrBadRequest { json =>
      json.validate[QuotationRequest].fold(
        defaultErrorHandler,
        quotationR => {
          val quotation = FablabQuotation(
            id = 0,
            userId = request.authInfo.user.id,
            realizationOf = quotationR.realizationOf,
            undertaken = false,
            machines = quotationR.machines,
            createdAt = models.dummyTimestamp)
          fablabService.createQuotation(quotation).map(resultJsonFromServiceReply(_))
        }
      )
    }
  }

}
