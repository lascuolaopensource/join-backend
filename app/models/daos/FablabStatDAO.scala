package models.daos

import java.sql.Timestamp

import models.{FablabMachineStat, FablabStatCount}

import scala.concurrent.Future


trait FablabStatDAO {

  def topMachinesByUsage(from: Timestamp, to: Timestamp, limit: Int): Future[Seq[FablabMachineStat]]

  def counts(from: Timestamp, to: Timestamp): Future[FablabStatCount]

}
