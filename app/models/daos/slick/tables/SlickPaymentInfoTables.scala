package models.daos.slick.tables

import java.sql.Timestamp

import models.daos.slick.tables.SlickPaymentInfoTables._
import models._

import scala.language.implicitConversions


private[slick] object SlickPaymentInfoTables {

  case class DBPaymentInfo(
    id: Long,
    paymentMethod: Int,
    transactionId: Option[String],
    cro: Option[String],
    verified: Option[Boolean],
    amount: Double,
    createdAt: Timestamp
  ) {
    lazy val updatableFields = verified
  }

  implicit def paymentMethodToDB(paymentMethod: PaymentMethod): Int = paymentMethod match {
    case PayPal => 0
    case CreditCard => 1
    case WireTransfer => 2
  }

  implicit def dbToPaymentMethod(int: Int): PaymentMethod = int match {
    case 0 => PayPal
    case 1 => CreditCard
    case 2 => WireTransfer
  }

  implicit def paymentInfoToDB(paymentInfo: PaymentInfo):  DBPaymentInfo = DBPaymentInfo(
    id = paymentInfo.id,
    paymentMethod = paymentInfo.paymentMethod,
    transactionId = paymentInfo.transactionId,
    cro = paymentInfo.cro,
    verified = paymentInfo.verified,
    amount = paymentInfo.amount,
    createdAt = new Timestamp(System.currentTimeMillis()))

}


private[slick] trait SlickPaymentInfoTables extends SlickTable {

  import profile.api._

  protected class PaymentInfoTable(tag: Tag) extends Table[DBPaymentInfo](tag, "payment_info") {
    def id: Rep[Long] = column("id", O.PrimaryKey, O.AutoInc)
    def paymentMethod: Rep[Int] = column("payment_method")
    def transactionId: Rep[Option[String]] = column("transaction_id")
    def cro: Rep[Option[String]] = column("cro")
    def verified: Rep[Option[Boolean]] = column("verified")
    def amount: Rep[Double] = column("amount")
    def createdAt: Rep[Timestamp] = column("created_at")
    override def * =
      (id, paymentMethod, transactionId, cro, verified, amount, createdAt) <>
        (DBPaymentInfo.tupled, DBPaymentInfo.unapply)

    def updatableFields = verified
  }

  protected val PaymentInfoQuery = TableQuery[PaymentInfoTable]

}
