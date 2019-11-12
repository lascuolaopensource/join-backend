package tasks

import com.typesafe.config.ConfigFactory
import play.api.Mode
import play.api._
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.{implicitConversions, postfixOps}


object AppTask {

  implicit def stringToMode(s: String): Mode = s match {
    case "dev" => Mode.Dev
    case "prod" => Mode.Prod
  }

}


trait AppTask {

  import AppTask._

  protected lazy val logger = Logger("app-task")

  protected def task(app: Application)(implicit ec: ExecutionContext): Future[Unit]

  def main(args: Array[String]): Unit = {
    val env = args.head
    val app = GuiceApplicationBuilder(
      environment = Environment.simple(mode = env),
      configuration = Configuration(ConfigFactory.load(s"env/$env"))
    ).build()

    implicit val ec = app.actorSystem.dispatcher

    logger.info("Running task...")
    Await.result(task(app), 5 minute)

    logger.info("Shutting down app...")
    Await.result(app.stop(), 1 minute)
  }

}
