package org.apache.james.imap.search

import java.nio.charset.{Charset, IllegalCharsetNameException, UnsupportedCharsetException}

import org.apache.james.imap.api.display.HumanReadableText
import org.apache.james.imap.api.message.request.SearchResultOption
import org.apache.james.imap.api.message.response.{StatusResponse, StatusResponseFactory}
import org.apache.james.imap.api.process.ImapSession
import org.apache.james.imap.api.{ImapConstants, ImapMessage, Tag}
import org.apache.james.imap.decode.ImapRequestLineReader.CharacterValidator
import org.apache.james.imap.decode.parser.AbstractUidCommandParser
import org.apache.james.imap.decode.{DecodingException, ImapRequestLineReader}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * Parse SEARCH commands
 */
object SearchCommandParser {
  private val LOGGER = LoggerFactory.getLogger(classOf[SearchCommandParser])
}

class SearchCommandParser(val statusResponseFactory: StatusResponseFactory) extends AbstractUidCommandParser(ImapConstants.SEARCH_COMMAND, statusResponseFactory) {
  /**
   * Parses the request argument into a valid search term.
   *
   * @param request
   * <code>ImapRequestLineReader</code>, not null
   * @param charset
   * <code>Charset</code> or null if there is no charset
   * @param isFirstToken
   * true when this is the first token read, false otherwise
   */
  @throws[DecodingException]
  @throws[IllegalCharsetNameException]
  @throws[UnsupportedCharsetException]
  protected def searchKey(session: ImapSession, request: ImapRequestLineReader, charset: Charset, isFirstToken: Boolean): SearchKey = {
    val next = request.nextChar
    if (next >= '0' && next <= '9' || next == '*' || next == '$') sequenceSet(session, request)
    else if (next == '(') paren(session, request, charset)
    else {
      val consumedFirstCharacter = 1
      consumeAndCap(request) match {
        case 'A' =>
          a(request, consumedFirstCharacter)
        case 'B' =>
          b(request, charset, consumedFirstCharacter)
        case 'C' =>
          c(session, request, isFirstToken, charset, consumedFirstCharacter)
        case 'D' =>
          d(request, consumedFirstCharacter)
        case 'F' =>
          f(request, charset, consumedFirstCharacter)
        case 'H' =>
          header(request, charset, consumedFirstCharacter)
        case 'K' =>
          keyword(request, consumedFirstCharacter)
        case 'L' =>
          larger(request, consumedFirstCharacter)
        case 'M' =>
          modseq(request, consumedFirstCharacter)
        case 'N' =>
          n(session, request, charset, consumedFirstCharacter)
        case 'O' =>
          o(session, request, charset, consumedFirstCharacter)
        case 'R' =>
          recent(request, consumedFirstCharacter)
        case 'S' =>
          s(request, charset, consumedFirstCharacter)
        case 'T' =>
          t(request, charset, consumedFirstCharacter)
        case 'U' =>
          u(request, consumedFirstCharacter)
        case 'Y' =>
          younger(request, consumedFirstCharacter)
        case _ =>
          throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
      }
    }
  }

  @throws[DecodingException]
  private def modseq(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("modseq", alreadyConsumed)(request)
    try ModSeq(request.number)
    catch {
      case _: DecodingException =>
        // Just consume the [<entry-name> <entry-type-req>] and ignore it
        // See RFC4551 3.4. MODSEQ Search Criterion in SEARCH
        request.consumeQuoted
        val validateAnyChar: CharacterValidator = _ => true
        request.consumeWord(validateAnyChar)
        ModSeq(request.number)
    }
  }

  @throws[DecodingException]
  private def paren(session: ImapSession, request: ImapRequestLineReader, charset: Charset): SearchKey = {
    val accumulator = mutable.Buffer[SearchKey]()

    @tailrec
    def addUntilParen(session: ImapSession, request: ImapRequestLineReader, charset: Charset): Unit = {
      val next = request.nextWordChar
      if (next == ')') {
        request.consume
      } else {
        accumulator.append(searchKey(session, request, null, isFirstToken = false))
        addUntilParen(session, request, charset)
      }
    }

    request.consume
    addUntilParen(session, request, charset)
    And(accumulator.toSeq)
  }

  @throws[DecodingException]
  private def consumeAndCap(request: ImapRequestLineReader) = ImapRequestLineReader.cap(request.consume)

  @throws[DecodingException]
  private def cc(request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey = {
    expectCharacters("cc", alreadyConsumed)(request)
    nextIsSpace(request)
    Cc(request.astring(charset))
  }

  @throws[DecodingException]
  @throws[IllegalCharsetNameException]
  @throws[UnsupportedCharsetException]
  private def c(session: ImapSession, request: ImapRequestLineReader, isFirstToken: Boolean, charset: Charset, alreadyConsumed: Int): SearchKey =
    consumeAndCap(request) match {
      case 'C' =>
        cc(request, charset, alreadyConsumed + 1)
      case 'H' =>
        parseCharset(session, request, isFirstToken, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  @throws[IllegalCharsetNameException]
  @throws[UnsupportedCharsetException]
  private def parseCharset(session: ImapSession, request: ImapRequestLineReader, isFirstToken: Boolean, alreadyConsumed: Int): SearchKey = {
    expectCharacters("charset ", alreadyConsumed)(request)
    if (!isFirstToken) throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
    val charset = Charset.forName(request.astring)
    request.nextWordChar
    searchKey(session, request, charset, isFirstToken = false)
  }

  @throws[DecodingException]
  private def u(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey =
    consumeAndCap(request) match {
      case 'I' =>
        uid(request, alreadyConsumed + 1)
      case 'N' =>
        un(request, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def un(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey =
    consumeAndCap(request) match {
      case 'A' =>
        unanswered(request, alreadyConsumed + 1)
      case 'D' =>
        und(request, alreadyConsumed + 1)
      case 'F' =>
        unflagged(request, alreadyConsumed + 1)
      case 'K' =>
        unkeyword(request, alreadyConsumed + 1)
      case 'S' =>
        unseen(request, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def und(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey =
    consumeAndCap(request) match {
      case 'E' =>
        undeleted(request, alreadyConsumed + 1)
      case 'R' =>
        undraft(request, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def t(request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey =
    consumeAndCap(request) match {
      case 'E' =>
        text(request, charset, alreadyConsumed + 1)
      case 'O' =>
        to(request, charset, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def s(request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int) : SearchKey =
    consumeAndCap(request) match {
      case 'E' =>
        se(request, alreadyConsumed + 1)
      case 'I' =>
        since(request, alreadyConsumed + 1)
      case 'M' =>
        smaller(request, alreadyConsumed + 1)
      case 'U' =>
        subject(request, charset, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def se(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey =
    consumeAndCap(request) match {
      case 'E' =>
        seen(request, alreadyConsumed + 1)
      case 'N' =>
        sen(request, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def sen(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey =
    consumeAndCap(request) match {
      case 'T' =>
        sent(request, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def sent(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey =
    consumeAndCap(request) match {
      case 'B' =>
        sentBefore(request, alreadyConsumed + 1)
      case 'O' =>
        sentOn(request, alreadyConsumed + 1)
      case 'S' =>
        sentSince(request, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def o(session: ImapSession, request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey =
    consumeAndCap(request) match {
      case 'L' =>
        old(request, alreadyConsumed + 1)
      case 'N' =>
        on(request, alreadyConsumed + 1)
      case 'R' =>
        or(session, request, charset, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def old(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("old", alreadyConsumed)(request)
    try { // Check if its OLDER keyword
      nextIs(request, 'e')
      older(request, alreadyConsumed + 1)
    } catch {
      case _: DecodingException =>
        Old()
    }
  }

  @throws[DecodingException]
  private def n(session: ImapSession, request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey =
    consumeAndCap(request) match {
      case 'E' =>
        newOperator(request, alreadyConsumed + 1)
      case 'O' =>
        not(session, request, charset, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def f(request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey =
    consumeAndCap(request) match {
      case 'L' =>
        flagged(request, alreadyConsumed + 1)
      case 'R' =>
        from(request, charset, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def d(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey =
    consumeAndCap(request) match {
      case 'E' =>
        deleted(request, alreadyConsumed + 1)
      case 'R' =>
        draft(request, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def keyword(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("keyword ", alreadyConsumed)(request)
    Keyword(request.atom)
  }

  @throws[DecodingException]
  private def unkeyword(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("unkeyword ", alreadyConsumed)(request)
    UnKeyword(request.atom)
  }

  @throws[DecodingException]
  private def header(request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey = {
    expectCharacters("header ", alreadyConsumed)(request)
    val field = request.astring(charset)
    nextIsSpace(request)
    val value = request.astring(charset)
    Header(field, value)
  }

  @throws[DecodingException]
  private def larger(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("larger ", alreadyConsumed)(request)
    Larger(request.number)
  }

  @throws[DecodingException]
  private def smaller(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("smaller ", alreadyConsumed)(request)
    Smaller(request.number)
  }

  @throws[DecodingException]
  private def from(request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey = {
    expectCharacters("from ", alreadyConsumed)(request)
    From(request.astring(charset))
  }

  @throws[DecodingException]
  private def flagged(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("flagged", alreadyConsumed)(request)
    Flagged()
  }

  @throws[DecodingException]
  private def unseen(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("unseen", alreadyConsumed)(request)
    UnSeen()
  }

  @throws[DecodingException]
  private def undraft(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("undraft", alreadyConsumed)(request)
    UnDraft()
  }

  @throws[DecodingException]
  private def undeleted(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("undeleted", alreadyConsumed)(request)
    UnDeleted()
  }

  @throws[DecodingException]
  private def unflagged(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("unflagged", alreadyConsumed)(request)
    UnFlagged()
  }

  @throws[DecodingException]
  private def unanswered(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("unanswered", alreadyConsumed)(request)
    UnAnswered()
  }

  @throws[DecodingException]
  private def younger(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("younger ", alreadyConsumed)(request)
    Younger(request.nzNumber)
  }

  @throws[DecodingException]
  private def older(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("older ", alreadyConsumed)(request)
    Older(request.nzNumber)
  }

  @throws[DecodingException]
  private def or(session: ImapSession, request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey = {
    expectCharacters("or ", alreadyConsumed)(request)
    val firstKey = searchKey(session, request, charset, isFirstToken = false)
    nextIsSpace(request)
    val secondKey = searchKey(session, request, charset, isFirstToken = false)
    Or(firstKey, secondKey)
  }

  @throws[DecodingException]
  private def not(session: ImapSession, request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey = {
    expectCharacters("not ", alreadyConsumed)(request)
    val nextKey = searchKey(session, request, charset, isFirstToken = false)
    Not(nextKey)
  }

  @throws[DecodingException]
  private def newOperator(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("new", alreadyConsumed)(request)
    New()
  }

  @throws[DecodingException]
  private def recent(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("recent", alreadyConsumed)(request)
    Recent()
  }

  @throws[DecodingException]
  private def seen(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("seen", alreadyConsumed)(request)
    Seen()
  }

  @throws[DecodingException]
  private def draft(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("draft", alreadyConsumed)(request)
    Draft()
  }

  @throws[DecodingException]
  private def deleted(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("deleted", alreadyConsumed)(request)
    Deleted()
  }

  @throws[DecodingException]
  private def b(request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey =
    consumeAndCap(request) match {
      case 'C' =>
        bcc(request, charset, alreadyConsumed + 1)
      case 'E' =>
        before(request, alreadyConsumed + 1)
      case 'O' =>
        body(request, charset, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def body(request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey = {
    expectCharacters("body ", alreadyConsumed)(request)
    Body(request.astring(charset))
  }

  @throws[DecodingException]
  private def on(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("on ", alreadyConsumed)(request)
    On(request.date)
  }

  @throws[DecodingException]
  private def sentBefore(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("sentbefore ", alreadyConsumed)(request)
    SentBefore(request.date)
  }

  @throws[DecodingException]
  private def sentSince(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("sentsince ", alreadyConsumed)(request)
    SentSince(request.date)
  }

  @throws[DecodingException]
  private def since(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("since ", alreadyConsumed)(request)
    Since(request.date)
  }

  @throws[DecodingException]
  private def sentOn(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("senton ", alreadyConsumed)(request)
    SentOn(request.date)
  }

  @throws[DecodingException]
  private def before(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("before", alreadyConsumed)(request)
    Before(request.date)
  }

  @throws[DecodingException]
  private def bcc(request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey = {
    expectCharacters("bcc ", alreadyConsumed)(request)
    Bcc(request.astring(charset))
  }

  @throws[DecodingException]
  private def text(request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey = {
    expectCharacters("text", alreadyConsumed)(request)
    Text(request.astring(charset))
  }

  @throws[DecodingException]
  private def uid(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("uid ", alreadyConsumed)(request)
    Uid(request.parseUidRange.toSeq)
  }

  @throws[DecodingException]
  private def sequenceSet(session: ImapSession, request: ImapRequestLineReader): SearchKey =
    SequenceNumbers(request.parseIdRange(session).toSeq)

  @throws[DecodingException]
  private def to(request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey = {
    expectCharacters("to ", alreadyConsumed)(request)
    To(request.astring(charset))
  }

  @throws[DecodingException]
  private def subject(request: ImapRequestLineReader, charset: Charset, alreadyConsumed: Int): SearchKey = {
    expectCharacters("subject ", alreadyConsumed)(request)
    Subject(request.astring(charset))
  }

  @throws[DecodingException]
  private def a(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey =
    consumeAndCap(request) match {
      case 'L' =>
        all(request, alreadyConsumed + 1)
      case 'N' =>
        answered(request, alreadyConsumed + 1)
      case _ =>
        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def answered(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("answered", alreadyConsumed)(request)
    Answered()
  }

  @throws[DecodingException]
  private def all(request: ImapRequestLineReader, alreadyConsumed: Int): SearchKey = {
    expectCharacters("all", alreadyConsumed)(request)
    All()
  }

  @throws[DecodingException]
  private def nextIsSpace(request: ImapRequestLineReader): Unit = {
    val next = request.consume
    if (next != ' ') throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  private def expectCharacters(chars: String, alreadyConsumed: Int)(request: ImapRequestLineReader): Unit =
    chars.drop(alreadyConsumed).foreach(c => nextIs(request, c))

  @throws[DecodingException]
  private def nextIs(request: ImapRequestLineReader, char: Char): Unit = {
    val next = request.consume
    if (next != char.toUpper && next != char.toLower) throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
  }

  @throws[DecodingException]
  @throws[IllegalCharsetNameException]
  @throws[UnsupportedCharsetException]
  def decode(session: ImapSession, request: ImapRequestLineReader): SearchKey = {
    request.nextWordChar
    val firstKey: SearchKey = searchKey(session, request, null, isFirstToken = true)

    def parseOtherKeys(): Seq[SearchKey] = {
      val keys = mutable.Buffer[SearchKey]()
      while (request.nextChar == ' ') {
        request.nextWordChar
        val key = searchKey(session, request, null, isFirstToken = false)
        keys.append(key)
      }
      keys.toSeq
    }

    val result =
      if (request.nextChar == ' ')
        And(Seq(firstKey).concat(parseOtherKeys()))
      else
        firstKey
    request.eol()
    result
  }

  private def unsupportedCharset(tag: Tag) = {
    val badCharset = StatusResponse.ResponseCode.badCharset
    taggedNo(tag, ImapConstants.SEARCH_COMMAND, HumanReadableText.BAD_CHARSET, badCharset)
  }

  /**
   * Parse the {@link SearchResultOption}'s which are used for ESEARCH
   */
  @throws[DecodingException]
  private def parseOptions(reader: ImapRequestLineReader): Seq[SearchResultOption] = {
    val options = mutable.Buffer[SearchResultOption]()
    reader.consumeChar('(')
    reader.nextWordChar
    var cap = consumeAndCap(reader)
    while (cap != ')') {
      cap match {
        case 'A' =>
          expectCharacters("all", 1)(reader)
          options.append(SearchResultOption.ALL)
        case 'C' =>
          expectCharacters("count", 1)(reader)
          options.append(SearchResultOption.COUNT)
        case 'M' =>
          consumeAndCap(reader) match {
            case 'A' =>
              expectCharacters("max", 2)(reader)
              options.append(SearchResultOption.MAX)
            case 'I' =>
              expectCharacters("min", 2)(reader)
              options.append(SearchResultOption.MIN)
            case _ =>
              throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
          }

        // Check for SAVE options which is part of the SEARCHRES extension
        case 'S' =>
          expectCharacters("save", 1)(reader)
          options.append(SearchResultOption.SAVE)
        case _ =>
          throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
      }
      reader.nextWordChar
      cap = consumeAndCap(reader)
    }
    // if the options are empty then we parsed RETURN () which is a shortcut for ALL.
    // See http://www.faqs.org/rfcs/rfc4731.html 3.1
    if (options.isEmpty) Seq(SearchResultOption.ALL)
    else options.toSeq
  }

  @throws[DecodingException]
  override protected def decode(request: ImapRequestLineReader, tag: Tag, useUids: Boolean, session: ImapSession): ImapMessage = try {
    var maybeRecent: Option[SearchKey] = None
    var options = Seq.empty[SearchResultOption]
    val c = ImapRequestLineReader.cap(request.nextWordChar)
    if (c == 'R') { // if we found a R its either RECENT or RETURN so consume it
      request.consume
      nextIs(request, 'e')
      consumeAndCap(request) match {
        case 'C' =>
          maybeRecent = Some(recent(request, alreadyConsumed = 2))
        case 'T' =>
          expectCharacters("return", 2)(request)
          request.nextWordChar
          options = parseOptions(request)
        case _ =>
          throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key")
      }
    }
    val finalKey = maybeRecent.map { recent =>
      if (request.nextChar != ' ') {
        request.eol()
        recent
      }
      else { // Parse the search term from the request
        val key = decode(session, request)
        And(Seq(recent, key))
      }
    }.getOrElse(decode(session, request))

    new SearchRequest(SearchOperation(finalKey, options), useUids, tag)
  } catch {
    case e@(_: IllegalCharsetNameException | _: UnsupportedCharsetException) =>
      SearchCommandParser.LOGGER.debug("Unable to decode request", e)
      unsupportedCharset(tag)
  }
}