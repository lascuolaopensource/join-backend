package services

import java.sql.Timestamp

import javax.inject.{Inject, Singleton}
import models.{FablabMachineStat, FablabStatCount}
import models.daos.FablabStatDAO

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class FablabStatService @Inject()(
  fablabStatDAO: FablabStatDAO
)(implicit ec: ExecutionContext)
  extends StatService
{

  def topMachinesByUsage(from: Timestamp, to: Timestamp, limit: Int = defaultLimit): Future[Seq[FablabMachineStat]] =
    fablabStatDAO.topMachinesByUsage(from, to, limit)

  def counts(from: Timestamp, to: Timestamp): Future[FablabStatCount] =
    fablabStatDAO.counts(from, to)

}
