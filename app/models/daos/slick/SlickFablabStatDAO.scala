package models.daos.slick

import java.sql.{Date, Timestamp}

import javax.inject.Inject
import models.{FablabMachineStat, FablabStatCount}
import models.daos.FablabStatDAO
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext


class SlickFablabStatDAO @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit val ec: ExecutionContext)
  extends FablabStatDAO with SlickDAO
{

  import profile.api._

  private lazy val topMachinesByUsageQ = Compiled {
    (from: Rep[Date], to: Rep[Date], limit: ConstColumn[Long]) =>
      (for {
        (mId, hs) <- FablabReservationTimeQuery
          .filter(t => t.date > from && t.date <= to)
          .groupBy(_.machineId)
          .map { case (mId, group) => (mId, group.length) }
          .sortBy(_._2.desc)
          .take(limit)
        machine <- FablabMachineQuery if mId === machine.id
      } yield (mId, machine.name, hs)).sortBy(_._3.desc)
  }

  override def topMachinesByUsage(from: Timestamp, to: Timestamp, limit: Int) = {
    val (fromD, toD) = (new Date(from.getTime), new Date(to.getTime))
    val from0D = new Date(from.getTime - (to.getTime - from.getTime))

    val action = for {
      p1 <- topMachinesByUsageQ(fromD, toD, limit).result
      p0 <- topMachinesByUsageQ(from0D, fromD, limit).result
    } yield p1 zip computeTrends(p0.map(_._1), p1.map(_._1)) map {
      case ((mId, name, hs), trend) => FablabMachineStat(mId, name, hs, trend)
    }

    db run action
  }

  override def counts(from: Timestamp, to: Timestamp) = {
    val action = for {
      quotations <- FablabQuotationQuery.filter(q => q.createdAt > from && q.createdAt < to).length.result
      reservations <- FablabReservationQuery.filter(r => r.createdAt > from && r.createdAt < to).length.result
      machines <- FablabMachineQuery.filter(m => m.createdAt > from && m.createdAt < to).length.result
    } yield FablabStatCount(quotations, reservations, machines)

    db run action
  }

}
