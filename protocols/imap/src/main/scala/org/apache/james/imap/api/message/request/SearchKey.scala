package org.apache.james.imap.api.message.request

import java.util.{Date, Optional}

import javax.mail.Flags.Flag
import org.apache.james.imap.api.ImapConstants
import org.apache.james.imap.api.message.{IdRange, UidRange}
import org.apache.james.imap.api.process.ImapSession
import org.apache.james.mailbox.MessageUid
import org.apache.james.mailbox.exception.MessageRangeException
import org.apache.james.mailbox.model.SearchQuery
import org.apache.james.mailbox.model.SearchQuery.{AddressType, Criterion, DateResolution}
import org.slf4j.LoggerFactory

import scala.collection.immutable.Iterable

sealed trait SearchKey

final case class All() extends SearchKey

final case class And(keys: Seq[SearchKey]) extends SearchKey

final case class Answered() extends SearchKey

final case class Bcc(value: String) extends SearchKey

final case class Before(date: DayMonthYear) extends SearchKey

final case class Body(value: String) extends SearchKey

final case class Cc(value: String) extends SearchKey

final case class Deleted() extends SearchKey

final case class Draft() extends SearchKey

final case class Header(name: String, value: String) extends SearchKey

final case class Flagged() extends SearchKey

final case class From(value: String) extends SearchKey

final case class Keyword(value: String) extends SearchKey

final case class Larger(size: Long) extends SearchKey

final case class New() extends SearchKey

final case class ModSeq(modSeq: Long) extends SearchKey

final case class Not(key: SearchKey) extends SearchKey

final case class Old() extends SearchKey

final case class Older(seconds: Long) extends SearchKey

final case class Or(keyOne: SearchKey, keyTwo: SearchKey) extends SearchKey

final case class On(date: DayMonthYear) extends SearchKey

final case class Recent() extends SearchKey

final case class Seen() extends SearchKey

final case class SentBefore(date: DayMonthYear) extends SearchKey

final case class SentOn(date: DayMonthYear) extends SearchKey

final case class SentSince(date: DayMonthYear) extends SearchKey

final case class SequenceNumbers(ids: Seq[IdRange]) extends SearchKey

final case class Since(date: DayMonthYear) extends SearchKey

final case class Smaller(size: Long) extends SearchKey

final case class Subject(value: String) extends SearchKey

final case class Text(value: String) extends SearchKey

final case class To(value: String) extends SearchKey

final case class Uid(ids: Iterable[UidRange]) extends SearchKey

final case class UnAnswered() extends SearchKey

final case class UnDeleted() extends SearchKey

final case class UnDraft() extends SearchKey

final case class UnFlagged() extends SearchKey

final case class UnKeyword(value: String) extends SearchKey

final case class UnSeen() extends SearchKey

final case class Younger(seconds: Long) extends SearchKey

object Criterion {

  private val LOGGER = LoggerFactory.getLogger(classOf[Criterion])

  @throws[MessageRangeException]
  def toQuery(key: SearchKey, session: ImapSession): SearchQuery = {
    val result = new SearchQuery
    val selected = session.getSelected
    if (selected != null) result.addRecentMessageUids(selected.getRecent)
    val criterion = toCriterion(key, session)
    result.andCriteria(criterion)
    result
  }

  @throws[MessageRangeException]
  private def toCriterion(key: SearchKey, session: ImapSession): SearchQuery.Criterion = {
    key match {
      case All() => SearchQuery.all
      case And(keys) => and(keys, session)
      case Answered() => SearchQuery.flagIsSet(Flag.ANSWERED)
      case Bcc(value) => SearchQuery.address(AddressType.Bcc, value)
      case Before(date) => SearchQuery.internalDateBefore(date.toDate, DateResolution.Day)
      case Body(value) => SearchQuery.bodyContains(value)
      case Cc(value) => SearchQuery.address(AddressType.Cc, value)
      case Deleted() => SearchQuery.flagIsSet(Flag.DELETED)
      case Draft() => SearchQuery.flagIsSet(Flag.DRAFT)
      case Flagged() => SearchQuery.flagIsSet(Flag.FLAGGED)
      case From(value) => SearchQuery.address(AddressType.From, value)
      case Header(name, value) =>
        // Check if header exists if the value is empty. See IMAP-311
        if (value == null || value.length == 0) SearchQuery.headerExists(name)
        else SearchQuery.headerContains(name, value)
      case Keyword(value) => SearchQuery.flagIsSet(value)
      case Larger(size) => SearchQuery.sizeGreaterThan(size)
      case New() => SearchQuery.and(SearchQuery.flagIsSet(Flag.RECENT), SearchQuery.flagIsUnSet(Flag.SEEN))
      case Not(key) => SearchQuery.not(toCriterion(key, session))
      case Old() => SearchQuery.flagIsUnSet(Flag.RECENT)
      case On(date) => SearchQuery.internalDateOn(date.toDate, DateResolution.Day)
      case Or(keyOne, keyTwo) => or(keyOne, keyTwo, session)
      case Recent() => SearchQuery.flagIsSet(Flag.RECENT)
      case Seen() => SearchQuery.flagIsSet(Flag.SEEN)
      case SentBefore(date) => SearchQuery.headerDateBefore(ImapConstants.RFC822_DATE, date.toDate, DateResolution.Day)
      case SentOn(date) => SearchQuery.headerDateOn(ImapConstants.RFC822_DATE, date.toDate, DateResolution.Day)
      case SentSince(date) =>
        // Include the date which is used as search param. See IMAP-293
        val onCrit = SearchQuery.headerDateOn(ImapConstants.RFC822_DATE, date.toDate, DateResolution.Day)
        val afterCrit = SearchQuery.headerDateAfter(ImapConstants.RFC822_DATE, date.toDate, DateResolution.Day)
        SearchQuery.or(onCrit, afterCrit)
      case SequenceNumbers(ids) => sequence(ids, session)
      case Since(date) => SearchQuery.or(SearchQuery.internalDateOn(date.toDate, DateResolution.Day), SearchQuery.internalDateAfter(date.toDate, DateResolution.Day))
      case Smaller(size) => SearchQuery.sizeLessThan(size)
      case Subject(value) => SearchQuery.headerContains(ImapConstants.RFC822_SUBJECT, value)
      case Text(value) => SearchQuery.mailContains(value)
      case To(value) => SearchQuery.address(AddressType.To, value)
      case Uid(ids) => uids(ids, session)
      case UnAnswered() => SearchQuery.flagIsUnSet(Flag.ANSWERED)
      case UnDeleted() =>SearchQuery.flagIsUnSet(Flag.DELETED)
      case UnDraft() => SearchQuery.flagIsUnSet(Flag.DRAFT)
      case UnFlagged() => SearchQuery.flagIsUnSet(Flag.FLAGGED)
      case UnKeyword(value) => SearchQuery.flagIsUnSet(value)
      case UnSeen() => SearchQuery.flagIsUnSet(Flag.SEEN)
      case Older(seconds) =>
        val withinDate = createWithinDate(seconds)
        SearchQuery.or(SearchQuery.internalDateOn(withinDate, DateResolution.Second), SearchQuery.internalDateBefore(withinDate, DateResolution.Second))
      case Younger(seconds) =>
        val withinDate2 = createWithinDate(seconds)
        SearchQuery.or(SearchQuery.internalDateOn(withinDate2, DateResolution.Second), SearchQuery.internalDateAfter(withinDate2, DateResolution.Second))
      case ModSeq(modSeq) =>
        session.setAttribute("SEARCH_MODSEQ", true)
        SearchQuery.or(SearchQuery.modSeqEquals(modSeq), SearchQuery.modSeqGreaterThan(modSeq))
      case key =>
        LOGGER.warn("Ignoring unknown search key {}", key )
        SearchQuery.all
    }
  }

  @throws[MessageRangeException]
  private def and(keys: Seq[SearchKey], session: ImapSession) = {
    import scala.jdk.CollectionConverters._
    SearchQuery.and(keys.map(toCriterion(_, session)).asJava)
  }

  private def createWithinDate(seconds: Long) = {
    val res = System.currentTimeMillis - seconds * 1000
    new Date(res)
  }

  @throws[MessageRangeException]
  private def or(keyOne: SearchKey, keyTwo: SearchKey, session: ImapSession) = {
    val criterionOne = toCriterion(keyOne, session)
    val criterionTwo = toCriterion(keyTwo, session)
    SearchQuery.or(criterionOne, criterionTwo)
  }

  /**
   * Create a {@link Criterion} for the given sequence-sets.
   * This include special handling which is needed for SEARCH to not return a BAD response on a invalid message-set.
   * See IMAP-292 for more details.
   */
  @throws[MessageRangeException]
  private def sequence(sequenceNumbers: Seq[IdRange], session: ImapSession) = {
    val selected = session.getSelected

    def toUidRangeCriterion(range: IdRange) = {
      val lowVal = range.getLowVal
      val highVal = range.getHighVal
      // Take care of "*" and "*:*" values by return the last message in
      // the mailbox. See IMAP-289
      if (lowVal == Long.MaxValue && highVal == Long.MaxValue) {
        val highUid = selected.getLastUid.orElse(MessageUid.MIN_VALUE)
        Some(new SearchQuery.UidRange(highUid))
      } else {
        val maybeLowUid =
          if (lowVal != Long.MinValue) selected.uid(lowVal.toInt)
          else selected.getFirstUid
        // The lowVal should never be
        // SelectedMailbox.NO_SUCH_MESSAGE but we check for it
        // just to be safe
        maybeLowUid.map(lowUid => {
          val highUid = {
            if (highVal != Long.MaxValue) selected.uid(highVal.toInt)
            else Optional.empty[MessageUid]
          }.or(() => selected.getLastUid)
          Option(new SearchQuery.UidRange(lowUid, highUid.orElse(MessageUid.MAX_VALUE)))
        }).orElse(None)
      }
    }

    // First of check if we have any messages in the mailbox
    // if not we don't need to go through all of this
    val ranges: Seq[SearchQuery.UidRange] =
      if (selected.existsCount > 0) {
        sequenceNumbers.flatMap(toUidRangeCriterion)
      } else {
        Seq.empty
      }
    SearchQuery.uid(ranges.toArray)
  }

  /**
   * Create a {@link Criterion} for the given uid-sets.
   * This include special handling which is needed for SEARCH to not return a BAD response on a invalid message-set.
   * See IMAP-292 for more details.
   */
  @throws[MessageRangeException]
  private def uids(uids: Iterable[UidRange], session: ImapSession) = {
    val selected = session.getSelected

    def toUidRangeCriterion(range: UidRange) = {
      val lowVal = range.getLowVal
      val highVal = range.getHighVal
      // Take care of "*" and "*:*" values by return the last
      // message in
      // the mailbox. See IMAP-289
      if (lowVal == MessageUid.MAX_VALUE && highVal == MessageUid.MAX_VALUE)
        new SearchQuery.UidRange(selected.getLastUid.orElse(MessageUid.MIN_VALUE))
      else if (highVal == MessageUid.MAX_VALUE && selected.getLastUid.orElse(MessageUid.MIN_VALUE).compareTo(lowVal) < 0) { // Sequence uid ranges which use
        // *:<uid-higher-then-last-uid>
        // MUST return at least the highest uid in the mailbox
        // See IMAP-291
        new SearchQuery.UidRange(selected.getLastUid.orElse(MessageUid.MIN_VALUE))
      }
      else new SearchQuery.UidRange(lowVal, highVal)
    }

    // First of check if we have any messages in the mailbox
    // if not we don't need to go through all of this
    val ranges = if (selected.existsCount > 0) {
      uids.map(toUidRangeCriterion)
    } else {
      Seq.empty
    }
    SearchQuery.uid(ranges.toArray)
  }
}