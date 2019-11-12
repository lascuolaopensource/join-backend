package models.daos.slick

import java.sql.Timestamp

import models._
import models.daos.slick.tables.SlickTables
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext


trait SlickDAO extends SlickTables {

  import profile.api._

  implicit val ec: ExecutionContext

  protected def timestampPlus(days: Int) = SimpleExpression.unary[Timestamp, Timestamp] { (n, qb) =>
    qb.expr(n)
    qb.sqlBuilder += s" + interval '$days days'"
  }

  protected val timestampPlus = SimpleExpression.binary[Timestamp, Int, Timestamp] { (n1, n2, qb) =>
    qb.expr(n1)
    qb.sqlBuilder += " + make_interval(days := "
    qb.expr(n2)
    qb.sqlBuilder += ")"
  }

  protected type SingleInsertResult[T] =
    profile.ProfileAction[profile.ReturningInsertActionComposer[T, Long]#SingleInsertResult, NoStream, Effect.Write]

  // common queries
  protected def findUserById(user: User) = UsersQuery.filter(_.id === user.id).result.headOption
  protected def findUserByEmail(email: String) = UsersQuery.filter(_.email === email).result.headOption

  protected def findPasswordInfoByUserId(userId: Long) = PasswordInfoQuery.filter(_.userId === userId).result.headOption

  protected def filterAccessTokensNotExpired(dbAccessToken: AccessTokenTable) =
    dbAccessToken.expiresIn >= new Timestamp(DateTime.now().getMillis)

  protected def deleteLearnAction(id: Long) = {
    for {
      _ <- BazaarIdeaTopicQuery.filter(_.bazaarTeachLearnId === id).delete
      _ <- BazaarIdeaGuestQuery.filter(_.bazaarTeachLearnId === id).delete
      _ <- BazaarPreferenceQuery.filter(_.bazaarTeachLearnId === id).delete
      _ <- BazaarCommentQuery.filter(_.bazaarTeachLearnId === id).delete
      _ <- ActivityTeachEventQuery.filter(_.bazaarTeachLearnId === id).map(_.bazaarTeachLearnId).update(None)
      n <- BazaarTeachLearnQuery.filter(_.id === id).delete
    } yield n == 1
  }

  protected def deleteAbstractIdea(id: Long) = {
    for {
      _ <- BazaarIdeaAudienceQuery.filter(_.bazaarAbstractIdeaId === id).delete
      _ <- BazaarIdeaMeetingDurationQuery.filter(_.bazaarAbstractIdeaId === id).delete
      _ <- BazaarIdeaDateQuery.filter(_.bazaarAbstractIdeaId === id).delete
      _ <- BazaarIdeaFundingQuery.filter(_.bazaarAbstractIdeaId === id).delete
      n <- BazaarAbstractIdeaQuery.filter(_.id === id).delete
    } yield n == 1
  }

  protected def deleteTeachAction(id: Long) = {
    for {
      idea <- BazaarTeachLearnQuery.filter(_.id === id).result.head
      _ <- BazaarIdeaTopicQuery.filter(_.bazaarTeachLearnId === idea.id).delete
      _ <- BazaarIdeaGuestQuery.filter(_.bazaarTeachLearnId === idea.id).delete
      _ <- BazaarPreferenceQuery.filter(_.bazaarTeachLearnId === idea.id).delete
      _ <- BazaarCommentQuery.filter(_.bazaarTeachLearnId === idea.id).delete
      _ <- ActivityTeachEventQuery.filter(_.bazaarTeachLearnId === id).map(_.bazaarTeachLearnId).update(None)
      n <- BazaarTeachLearnQuery.filter(_.id === idea.id).delete
      _ <- deleteAbstractIdea(idea.bazaarAbstractIdeaId.get)
    } yield n == 1
  }

  protected def deleteEventAction(id: Long) = {
    for {
      idea <- BazaarEventQueryAll.filter(_.id === id).result.head
      _ <- BazaarIdeaTopicQuery.filter(_.bazaarEventId === idea.id).delete
      _ <- BazaarIdeaGuestQuery.filter(_.bazaarEventId === idea.id).delete
      _ <- BazaarPreferenceQuery.filter(_.bazaarEventId === idea.id).delete
      _ <- BazaarCommentQuery.filter(_.bazaarEventId === idea.id).delete
      _ <- ActivityTeachEventQuery.filter(_.bazaarEventId === id).map(_.bazaarEventId).update(None)
      n <- BazaarEventQueryAll.filter(_.id === idea.id).delete
      _ <- deleteAbstractIdea(idea.bazaarAbstractIdeaId)
    } yield n == 1
  }

  protected def deleteResearchAction(id: Long) = {
    for {
      _ <- BazaarIdeaTopicQuery.filter(_.bazaarResearchId === id).delete
      researchRoleQuery = BazaarResearchRoleQuery.filter(_.bazaarResearchId === id)
      _ <- BazaarResearchSkillQuery.filter(_.bazaarResearchRoleId in researchRoleQuery.map(_.id)).delete
      _ <- researchRoleQuery.delete
      _ <- BazaarPreferenceQuery.filter(_.bazaarResearchId === id).delete
      _ <- BazaarCommentQuery.filter(_.bazaarResearchId === id).delete
      _ <- ActivityResearchQuery.filter(_.bazaarResearchId === id).map(_.bazaarResearchId).update(None)
      n <- BazaarResearchQuery.filter(_.id === id).delete
    } yield n == 1
  }

  protected def computeTrends(p0: Seq[Long], p1: Seq[Long]): Seq[StatTrend] =
    p1.zipWithIndex map {
      case (id, i1) =>
        val i0 = p0.indexOf(id)
        if (i1 > i0) UpTrend
        else if (i1 < i0) DownTrend
        else EqTrend
    }

}
