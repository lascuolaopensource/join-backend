package models.daos.slick.tables

import java.sql.{Date, Timestamp}

import models._
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.duration._
import scala.language.implicitConversions


trait SlickTable extends HasDatabaseConfigProvider[JdbcProfile] {

  protected val profile: JdbcProfile

  import Implicits.WithId
  import Implicits.WithIdSyntax._

  protected def zipWithIds[T: WithId](seq: Seq[T], ids: Seq[Long]): Seq[T] = seq zip ids map { case (t, id) => t.copy(id) }

  protected def getDeletableIds[T <: Deletable : WithId](seq: Seq[T]): Seq[Long] = seq.filter(_.toDelete).map(_.id)

  protected def getCreatable[T <: Deletable : WithId](seq: Seq[T]): Seq[T] = seq filter (x => !x.toDelete && x.id == 0)
  protected def getExisting[T <: Deletable : WithId](seq: Seq[T]): Seq[T] = seq filter (x => !x.toDelete && x.id != 0)

  implicit def dateTimeToTimestamp(dateTime: DateTime): java.sql.Timestamp = new Timestamp(dateTime.getMillis)
  implicit def dateToTimestamp(date: java.util.Date): java.sql.Timestamp = new Timestamp(date.getTime)
  implicit def longToTimestamp(long: Long): java.sql.Timestamp = new Timestamp(long)

  protected def currentTimestamp() = new Timestamp(System.currentTimeMillis())
  protected def today(): Date = new Date(System.currentTimeMillis())
  protected def yesterday(): Date = new Date(System.currentTimeMillis() - 1.day.toMillis)

  protected implicit def levelToInt(level: SOSLevel): Int = level match {
    case EntryLevel => 0
    case IntermediateLevel => 1
    case AdvancedLevel => 2
  }

  protected implicit def intToLevel(level: Int): SOSLevel = level match {
    case 0 => EntryLevel
    case 1 => IntermediateLevel
    case 2 => AdvancedLevel
  }

  protected implicit def audienceToInt(audience: Audience): Int = audience match {
    case Kids => 0
    case Teenagers => 1
    case Students => 2
    case Researchers => 3
    case Professionals => 4
    case Companies => 5
    case PublicAdministrations => 6
    case Seniors => 7
    case Immigrants => 8
    case Unemployed => 9
  }

  protected implicit def intToAudience(audience: Int): Audience = audience match {
    case 0 => Kids
    case 1 => Teenagers
    case 2 => Students
    case 3 => Researchers
    case 4 => Professionals
    case 5 => Companies
    case 6 => PublicAdministrations
    case 7 => Seniors
    case 8 => Immigrants
    case 9 => Unemployed
  }

  protected implicit def teachActivityTypeToInt(teachActivityType: TeachActivityType): Int = teachActivityType match {
    case LecturePerformance => 0
    case LabsSeminary => 1
    case XYZFormat => 2
    case CulturalEvent => 3
    case VerticalFormat => 4
    case MachineUsage => 5
  }

  protected implicit def intToTeachActivityType(int: Int): TeachActivityType = int match {
    case 0 => LecturePerformance
    case 1 => LabsSeminary
    case 2 => XYZFormat
    case 3 => CulturalEvent
    case 4 => VerticalFormat
    case 5 => MachineUsage
  }

  protected implicit def eventActivityTypeToInt(eventActivityType: EventActivityType): Int = eventActivityType match {
    case Talk => 0
    case Projection => 1
    case Exposition => 2
    case Workshop => 3
    case Performance => 4
  }

  protected implicit def intToEventActivityType(int: Int): EventActivityType = int match {
    case 0 => Talk
    case 1 => Projection
    case 2 => Exposition
    case 3 => Workshop
    case 4 => Performance
  }

  protected implicit def activityTypeToInt(activityType: ActivityType): Int = activityType match {
    case x: TeachActivityType => x
    case x: EventActivityType => x
  }

  protected implicit def recurringEntityToInt(recurringEntity: RecurringEntity): Int = recurringEntity match {
    case Weekly => 0
    case Monthly => 1
    case Yearly => 2
  }

  protected implicit def intToRecurringEntity(int: Int): RecurringEntity = int match {
    case 0 => Weekly
    case 1 => Monthly
    case 2 => Yearly
  }

}


trait SlickTables extends SlickTable
  with SlickMailTokenTable
  with SlickAccessTokenTable
  with SlickPasswordInfoTable
  with SlickUserTable
  with SlickSkillTable
  with SlickMembershipTables
  with SlickBazaarIdeaTables
  with SlickBazaarAbstractIdeaTable
  with SlickBazaarTeachLearnTable
  with SlickBazaarEventTable
  with SlickBazaarResearchTable
  with SlickTopicTable
  with SlickBazaarPreferenceTable
  with SlickBazaarCommentTable
  with SlickUserFavoriteTable
  with SlickImageGalleryTables
  with SlickActivityTeachEventTables
  with SlickActivityResearchTables
  with SlickPaymentInfoTables
  with SlickFablabTables
  with SlickRulesTable
