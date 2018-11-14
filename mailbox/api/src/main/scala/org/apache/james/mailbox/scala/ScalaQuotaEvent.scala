package org.apache.james.mailbox.scala

import java.time.Instant
import java.util.Optional

import julienrf.json.derived
import org.apache.james.core.quota.{QuotaCount, QuotaSize, QuotaValue}
import org.apache.james.core.{Domain, User}
import org.apache.james.mailbox.MailboxListener.{QuotaEvent => JavaQuotaEvent, QuotaUsageUpdatedEvent => JavaQuotaUsageUpdatedEvent}
import org.apache.james.mailbox.model.{QuotaRoot, Quota => JavaQuota}
import play.api.libs.json._

import scala.collection.JavaConverters._

private sealed trait ScalaQuotaEvent {
  def getQuotaRoot: QuotaRoot
  def toJava: JavaQuotaEvent
}

private object DTO {
  case class Quota[T <: QuotaValue[T]](used: T, limit: T, limits: Map[JavaQuota.Scope, T]) {
    def toJava: JavaQuota[T] =
      JavaQuota.builder[T].used(used).computedLimit(limit).limitsByScope(limits.asJava).build()
  }

  case class QuotaUsageUpdatedEvent(quotaRoot: QuotaRoot, countQuota: Quota[QuotaCount],
                                            sizeQuota: Quota[QuotaSize], time: Instant) extends ScalaQuotaEvent {
    override def getQuotaRoot: QuotaRoot = quotaRoot
    override def toJava: JavaQuotaEvent =
      new JavaQuotaUsageUpdatedEvent(null, getQuotaRoot, countQuota.toJava, sizeQuota.toJava, time)
  }

  case class QuotaNoop(user: User, quotaRoot: QuotaRoot) extends ScalaQuotaEvent {
    override def getQuotaRoot: QuotaRoot = quotaRoot
    override def toJava: JavaQuotaEvent = ???
  }
}

private object JsonSerialize {
  implicit val userWriters: Writes[User] = (user: User) => JsString(user.asString)
  implicit val quotaRootWrites: Writes[QuotaRoot] = quotaRoot => JsString(quotaRoot.getValue)
  implicit val quotaValueWrites: Writes[QuotaValue[_]] = value => JsNumber(value.asLong)
  implicit val quotaScopeWrites: Writes[JavaQuota.Scope] = value => JsString(value.name)
  implicit val quotaCountWrites: Writes[DTO.Quota[QuotaCount]] = Json.writes[DTO.Quota[QuotaCount]]
  implicit val quotaSizeWrites: Writes[DTO.Quota[QuotaSize]] = Json.writes[DTO.Quota[QuotaSize]]

  implicit val userReads: Reads[User] = {
    case JsString(userAsString) => JsSuccess(User.fromUsername(userAsString))
    case _ => JsError()
  }
  implicit val quotaRootReads: Reads[QuotaRoot] = {
    case JsString(quotaRoot) => JsSuccess(QuotaRoot.quotaRoot(quotaRoot, Optional.empty[Domain]))
    case _ => JsError()
  }
  implicit val quotaCountReads: Reads[QuotaCount] = {
    case JsNumber(count) => JsSuccess(QuotaCount.count(count.toLong))
    case _ => JsError()
  }
  implicit val quotaSizeReads: Reads[QuotaSize] = {
    case JsNumber(size) => JsSuccess(QuotaSize.size(size.toLong))
    case _ => JsError()
  }
  implicit val quotaScopeReads: Reads[JavaQuota.Scope] = {
    case JsString(value) => JsSuccess(JavaQuota.Scope.valueOf(value))
    case _ => JsError()
  }

  implicit def scopeMapReads[V](implicit vr: Reads[V]): Reads[Map[JavaQuota.Scope, V]] =
    Reads.mapReads[JavaQuota.Scope, V] { str =>
      Json.fromJson[JavaQuota.Scope](JsString(str))
    }

  implicit def scopeMapWrite[V](implicit vr: Writes[V]): Writes[Map[JavaQuota.Scope, V]] =
    (m: Map[JavaQuota.Scope, V]) => {
      JsObject(m.map { case (k, v) => (k.toString, vr.writes(v)) }.toSeq)
    }

  implicit val quotaCReads: Reads[DTO.Quota[QuotaCount]] = Json.reads[DTO.Quota[QuotaCount]]
  implicit val quotaSReads: Reads[DTO.Quota[QuotaSize]] = Json.reads[DTO.Quota[QuotaSize]]

  implicit val quotaEventOFormat: OFormat[ScalaQuotaEvent] = derived.oformat()

  def toJson(event: ScalaQuotaEvent): String = Json.toJson(event).toString()

  def fromJson(json: String): JsResult[ScalaQuotaEvent] = Json.fromJson[ScalaQuotaEvent](Json.parse(json))
}

object ScalaQuotaEvent {

  private def toScala[T <: QuotaValue[T]](java: JavaQuota[T]): DTO.Quota[T] =
    DTO.Quota(used = java.getUsed, limit = java.getLimit, limits = java.getLimitByScope.asScala.toMap)

  private def toScala(event: JavaQuotaUsageUpdatedEvent): DTO.QuotaUsageUpdatedEvent =
    DTO.QuotaUsageUpdatedEvent(quotaRoot = event.getQuotaRoot, countQuota = toScala(event.getCountQuota), sizeQuota = toScala(event.getSizeQuota), event.getInstant)

  def toJson(event: JavaQuotaEvent): String = event match {
    case e: JavaQuotaUsageUpdatedEvent => JsonSerialize.toJson(toScala(e))
    case _ => throw new RuntimeException("no encoder found")
  }

  def fromJson(json: String): JsResult[JavaQuotaEvent] = {
    JsonSerialize.fromJson(json)
      .map(event => event.toJava)
  }
}