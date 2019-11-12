package models.daos.slick

import java.sql.Timestamp

import javax.inject.Inject
import models._
import models.daos.slick.tables.SlickBazaarPreferenceTable.DBBazaarPreference
import models.daos.{BazaarCommentDAO, BazaarPreferenceDAO}
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.DBIOAction

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions


private object SlickBazaarPreferenceDAO {

  implicit def preferenceToDB(preference: BazaarPreference): DBBazaarPreference = {
    val dbPreference = DBBazaarPreference(
      id = preference.id,
      bazaarTeachLearnId = None,
      bazaarEventId = None,
      bazaarResearchId = None,
      userId = preference.userId,
      agree = None,
      wishId = preference.wish.map(_.id),
      favorite = None,
      viewed = None
    )

    preference.ideaType match {
      case BazaarLearnType | BazaarTeachType =>
        dbPreference.copy(bazaarTeachLearnId = Some(preference.ideaId))
      case BazaarEventType =>
        dbPreference.copy(bazaarEventId = Some(preference.ideaId))
      case BazaarResearchType =>
        dbPreference.copy(bazaarResearchId = Some(preference.ideaId))
    }
  }

  implicit def dbPreferenceToPreference(dbPreference: DBBazaarPreference)(implicit ideaType: BazaarIdeaType): BazaarPreference =
    BazaarPreference(
      id = dbPreference.id,
      userId = dbPreference.userId,
      ideaId = getIdeaId(ideaType, dbPreference).get,
      ideaType = ideaType,
      agree = dbPreference.agree.isDefined,
      wish = None,
      favorite = dbPreference.favorite.isDefined,
      viewed = dbPreference.viewed.isDefined
    )

  def getIdeaId(ideaType: BazaarIdeaType, t: DBBazaarPreference) = ideaType match {
    case BazaarLearnType | BazaarTeachType  => t.bazaarTeachLearnId
    case BazaarEventType                    => t.bazaarEventId
    case BazaarResearchType                 => t.bazaarResearchId
  }

  def getUpsertValue(flag: Boolean, stored: Option[Timestamp], now: Timestamp) =
    if (flag)
      if (stored.isDefined) stored
      else Some(now)
    else None

}


class SlickBazaarPreferenceDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider,
  protected val bazaarCommentDAO: BazaarCommentDAO)(
  implicit val ec: ExecutionContext)
  extends BazaarPreferenceDAO with SlickDAO {

  import profile.api._
  import SlickBazaarPreferenceDAO._


  private def fk(ideaType: BazaarIdeaType, t: BazaarPreferenceTable) = ideaType match {
    case BazaarLearnType | BazaarTeachType  => t.bazaarTeachLearnId
    case BazaarEventType                    => t.bazaarEventId
    case BazaarResearchType                 => t.bazaarResearchId
  }

  private def preferenceQuery(userId: Long, ideaId: Long, ideaType: BazaarIdeaType) =
    BazaarPreferenceQuery.filter(up => up.userId === userId && fk(ideaType, up) === ideaId)

  override def find(userId: Long, ideaId: Long, ideaType: BazaarIdeaType): Future[Option[BazaarPreference]] = {
    val query = preferenceQuery(userId, ideaId, ideaType)
      .joinLeft(BazaarCommentQuery).on(_.wishId === _.id)
      .zip(UsersQuery.filter(_.id === userId).map(u => (u.firstName, u.lastName)))

    db run query.result.headOption map (_.map { data =>
      val ((dbUP, dbComment), (firstName, lastName)) = data
      val comment = dbComment.map(c => dbCommentToComment(c).copy(firstName = Some(firstName), lastName = Some(lastName)))
      implicit val ideaTypeImplicit = ideaType
      (dbUP: BazaarPreference).copy(wish = comment)
    })
  }

  override def upsertFlags(bazaarPreference: BazaarPreference): Future[BazaarPreference] = {
    val BazaarPreference(_, userId, ideaId, ideaType, agree, _, favorite, viewed) = bazaarPreference
    implicit val ideaTypeImplicit = ideaType

    val now = currentTimestamp()

    val action = for {
      prefOpt <- BazaarPreferenceQuery.filter(t => fk(ideaType, t) === ideaId && t.userId === userId).result.headOption
      pref <- prefOpt match {
        case Some(pref) =>
          val agree_ = getUpsertValue(agree, pref.agree, now)
          val favorite_ = getUpsertValue(favorite, pref.favorite, now)
          val viewed_ = getUpsertValue(viewed, pref.viewed, now)

          for {
            n <- BazaarPreferenceQuery.filter(_.id === pref.id)
              .map(p => (p.agree, p.favorite, p.viewed))
              .update(agree_, favorite_, viewed_)
            w <- BazaarCommentQuery.filter(_.id === pref.wishId).result.headOption.map(_.map(dbCommentToComment))
          } yield (if (n == 1) bazaarPreference.copy(id = pref.id) else pref: BazaarPreference).copy(wish = w)
        case None =>
          ((BazaarPreferenceQuery returning BazaarPreferenceQuery.map(_.id)) += bazaarPreference).map(id => bazaarPreference.copy(id = id))
      }
    } yield pref

    db run action
  }

  override def upsertWish(bazaarPreference: BazaarPreference): Future[BazaarPreference] = {
    val BazaarPreference(_, userId, ideaId, ideaType, _, Some(wish), _, _) = bazaarPreference
    implicit val ideaTypeImplicit = ideaType

    val action = for {
      prefOpt <- BazaarPreferenceQuery.filter(t => fk(ideaType, t) === ideaId && t.userId === userId).result.headOption
      pref <- prefOpt match {
        case Some(pref) if pref.wishId.isDefined =>
          BazaarCommentQuery.filter(_.id === pref.wishId.get).map(_.comment).update(wish.comment)
            .map(_ => dbPreferenceToPreference(pref).copy(wish = Some(wish)))
        case Some(pref) =>
          for {
            comment <- DBIO.from(bazaarCommentDAO.create(ideaId, ideaType, wish))
            _ <- BazaarPreferenceQuery.filter(_.id === pref.id).map(_.wishId).update(Some(comment.id))
          } yield dbPreferenceToPreference(pref).copy(wish = Some(comment))
        case None =>
          for {
            comment <- DBIO.from(bazaarCommentDAO.create(ideaId, ideaType, wish))
            prefId <- (BazaarPreferenceQuery returning BazaarPreferenceQuery.map(_.id)) += bazaarPreference.copy(wish = Some(comment))
          } yield bazaarPreference.copy(id = prefId, wish = Some(comment))
      }
    } yield pref

    db run action.transactionally
  }

  override def deleteWish(bazaarPreference: BazaarPreference) = {
    val action = for {
      prefOpt <- BazaarPreferenceQuery.filter(p => p.id === bazaarPreference.id && p.userId === bazaarPreference.userId).result.headOption
      res <- {
        if (prefOpt.isDefined)
          for {
            n1 <- BazaarPreferenceQuery.filter(_.id === bazaarPreference.id).map(_.wishId).update(None)
            n2 <- BazaarCommentQuery.filter(_.id === prefOpt.get.wishId.get).delete
          } yield Some(n1 == 1 && n2 == 1)
        else
          DBIOAction.successful(None)
      }
    } yield res

    db run action.transactionally map (optRes => optRes.map(res => if (res) bazaarPreference.copy(wish = None) else bazaarPreference))
  }

  override def count(ideaId: Long, bazaarIdeaType: BazaarIdeaType) =
    db run BazaarPreferenceQuery.filter(p => fk(bazaarIdeaType, p) === ideaId).result map { preferences =>
      (preferences.count(_.viewed.isDefined), preferences.count(_.agree.isDefined),
        preferences.count(_.wishId.isDefined), preferences.count(_.favorite.isDefined))
    }

}
