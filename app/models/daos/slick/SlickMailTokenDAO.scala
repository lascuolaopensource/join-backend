package models.daos.slick

import javax.inject.Inject
import models.MailToken
import models.daos.MailTokenDAO
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider

import scala.Function.const
import scala.concurrent.ExecutionContext


class SlickMailTokenDAO @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  configuration: Configuration
)(implicit val ec: ExecutionContext
) extends MailTokenDAO with SlickDAO {

  import profile.api._

  override def find(token: String) =
    db run MailTokenQuery.filter(_.token === token).result.headOption

  override def create(mailToken: MailToken) =
    db run (MailTokenQuery += mailToken) map const(mailToken)

  override def consume(token: String) =
    db run MailTokenQuery.filter(_.token === token).delete map (_ == 1)

}
