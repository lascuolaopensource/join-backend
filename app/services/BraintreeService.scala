package services

import java.math.BigDecimal
import javax.inject.Inject

import com.braintreegateway.{BraintreeGateway, Environment, Transaction, TransactionRequest}
import play.api.Configuration
import services.BraintreeService._

import scala.language.implicitConversions


trait BraintreeService {

  def generateClientToken: String

  def createTransaction(amount: Double, nonce: String): ServiceReply[Transaction, GenericError]
  def submitTransaction(transaction: Transaction): ServiceReply[Transaction, GenericError]
  def voidTransaction(transaction: Transaction): ServiceReply[Transaction, GenericError]

}


private object BraintreeService {

  implicit def resultConverter[T](r: com.braintreegateway.Result[T]): ServiceReply[T, GenericError] =
    if (r.isSuccess) Ok(r.getTarget) else Error(GenericError(r.getMessage))

}


class BraintreeServiceImpl @Inject()(
  configuration: Configuration
) extends BraintreeService {

  private val gateway = {
    val config = configuration.get[Configuration]("braintree")
    val environment = config.getAndValidate[String]("environment", Set("sandbox", "production"))

    new BraintreeGateway(
      if (environment == "production")
        Environment.PRODUCTION
      else Environment.SANDBOX,
      config.get[String]("merchant_id"),
      config.get[String]("public_key"),
      config.get[String]("private_key")
    )
  }

  override def generateClientToken = gateway.clientToken().generate()

  override def createTransaction(amount: Double, nonce: String) = {
    val decimalAmount = new BigDecimal(String.valueOf(f"$amount%.2f"))
    val request = new TransactionRequest() amount decimalAmount paymentMethodNonce nonce
    gateway transaction() sale request
  }

  override def submitTransaction(transaction: Transaction) =
    gateway transaction() submitForSettlement transaction.getId

  override def voidTransaction(transaction: Transaction) =
    gateway transaction() voidTransaction transaction.getId

}
