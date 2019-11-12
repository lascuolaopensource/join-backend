package models.daos.slick

import java.sql.Timestamp

import models.{BazaarIdea, BazaarIdeaSlim}

import scala.concurrent.Future


private[slick] trait SlickBazaarIdeaSpecificDAO[T <: BazaarIdea, DBT, DBTS] extends SlickDAO {

  import profile.api._

  protected type TableTypeS <: Table[DBTS]
  protected type QueryTypeS = Query[TableTypeS, DBTS, Seq]
  protected def allQuery: QueryTypeS
  protected def findQuery(id: Long): QueryTypeS

  protected def table2Disabled(t: TableTypeS): Rep[Boolean]
  protected def table2PastDeadline(t: TableTypeS, now: Timestamp): Rep[Boolean]

  protected implicit class TableOps(t: TableTypeS) {
    def disabled = table2Disabled(t)
    def isPastDeadline(now: Timestamp) = table2PastDeadline(t, now)
  }

  protected implicit class QueryOps(q: QueryTypeS) {
    def includeDisabled(yes: Boolean) =
      if (yes) q
      else q.filter(_.disabled === false)
    def includePastDeadline(yes: Boolean) =
      if (yes) q
      else q.filterNot(_.isPastDeadline(currentTimestamp()))
  }

  def enhance(dbIdea: DBTS): Future[T]

  final def all(disabled: Boolean): Future[Seq[T]] =
    db run allQuery.includeDisabled(disabled).includePastDeadline(disabled).result flatMap { ideas =>
      Future sequence (ideas map enhance)
    }

  def allSlim(userId: Long, disabled: Boolean): Future[Seq[BazaarIdeaSlim]]

  final def find(id: Long, disabled: Boolean): Future[Option[T]] =
    db run findQuery(id).includeDisabled(disabled).includePastDeadline(disabled).result.headOption flatMap {
      case Some(idea) => enhance(idea).map(Some(_))
      case None => Future.successful(None)
    }

  def create(idea: T): Future[T]

  def update(idea: T): Future[T]

  def delete(id: Long): Future[Boolean]

}
