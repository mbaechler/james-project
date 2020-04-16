/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.imap.decode.parser;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.Tag;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.UidRange;
import org.apache.james.imap.api.message.request.All;
import org.apache.james.imap.api.message.request.And;
import org.apache.james.imap.api.message.request.Answered;
import org.apache.james.imap.api.message.request.Bcc;
import org.apache.james.imap.api.message.request.Before;
import org.apache.james.imap.api.message.request.Body;
import org.apache.james.imap.api.message.request.Cc;
import org.apache.james.imap.api.message.request.DayMonthYear;
import org.apache.james.imap.api.message.request.Deleted;
import org.apache.james.imap.api.message.request.Draft;
import org.apache.james.imap.api.message.request.Flagged;
import org.apache.james.imap.api.message.request.From;
import org.apache.james.imap.api.message.request.Header;
import org.apache.james.imap.api.message.request.Keyword;
import org.apache.james.imap.api.message.request.Larger;
import org.apache.james.imap.api.message.request.ModSeq;
import org.apache.james.imap.api.message.request.New;
import org.apache.james.imap.api.message.request.Not;
import org.apache.james.imap.api.message.request.Old;
import org.apache.james.imap.api.message.request.Older;
import org.apache.james.imap.api.message.request.On;
import org.apache.james.imap.api.message.request.Or;
import org.apache.james.imap.api.message.request.Recent;
import org.apache.james.imap.api.message.request.SearchKey;
import org.apache.james.imap.api.message.request.SearchOperation;
import org.apache.james.imap.api.message.request.SearchResultOption;
import org.apache.james.imap.api.message.request.Seen;
import org.apache.james.imap.api.message.request.SentBefore;
import org.apache.james.imap.api.message.request.SentOn;
import org.apache.james.imap.api.message.request.SentSince;
import org.apache.james.imap.api.message.request.SequenceNumbers;
import org.apache.james.imap.api.message.request.Since;
import org.apache.james.imap.api.message.request.Smaller;
import org.apache.james.imap.api.message.request.Subject;
import org.apache.james.imap.api.message.request.Text;
import org.apache.james.imap.api.message.request.To;
import org.apache.james.imap.api.message.request.Uid;
import org.apache.james.imap.api.message.request.UnAnswered;
import org.apache.james.imap.api.message.request.UnDeleted;
import org.apache.james.imap.api.message.request.UnDraft;
import org.apache.james.imap.api.message.request.UnFlagged;
import org.apache.james.imap.api.message.request.UnKeyword;
import org.apache.james.imap.api.message.request.UnSeen;
import org.apache.james.imap.api.message.request.Younger;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponse.ResponseCode;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.decode.DecodingException;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.message.request.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import scala.jdk.javaapi.CollectionConverters;


/**
 * Parse SEARCH commands
 */
public class SearchCommandParser extends AbstractUidCommandParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchCommandParser.class);

    public SearchCommandParser(StatusResponseFactory statusResponseFactory) {
        super(ImapConstants.SEARCH_COMMAND, statusResponseFactory);
    }

    /**
     * Parses the request argument into a valid search term.
     * 
     * @param request
     *            <code>ImapRequestLineReader</code>, not null
     * @param charset
     *            <code>Charset</code> or null if there is no charset
     * @param isFirstToken
     *            true when this is the first token read, false otherwise
     */
    protected SearchKey searchKey(ImapSession session, ImapRequestLineReader request, Charset charset, boolean isFirstToken) throws DecodingException, IllegalCharsetNameException, UnsupportedCharsetException {
        final char next = request.nextChar();
        
        if (next >= '0' && next <= '9' || next == '*' || next == '$') {
            return sequenceSet(session, request);
        } else if (next == '(') {
            return paren(session, request, charset);
        } else {
            final int cap = consumeAndCap(request);
            switch (cap) {
            
            case 'A':
                return a(request);
            case 'B':
                return b(request, charset);
            case 'C':
                return c(session, request, isFirstToken, charset);
            case 'D':
                return d(request);
            case 'E':
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
            case 'F':
                return f(request, charset);
            case 'G':
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
            case 'H':
                return header(request, charset);
            case 'I':
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
            case 'J':
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
            case 'K':
                return keyword(request);
            case 'L':
                return larger(request);
            case 'M':
                return modseq(request);
            case 'N':
                return n(session, request, charset);
            case 'O':
                return o(session, request, charset);
            case 'P':
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
            case 'Q':
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
            case 'R':
                nextIsE(request);
                nextIsC(request);
                return recent(request);
            case 'S':
                return s(request, charset);
            case 'T':
                return t(request, charset);
            case 'U':
                return u(request);
            case 'Y':
                return younger(request);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
            }
        }
    }

    private SearchKey modseq(ImapRequestLineReader request) throws DecodingException {        
        nextIsO(request);
        nextIsD(request);
        nextIsS(request);
        nextIsE(request);
        nextIsQ(request);
        
        try {
            return ModSeq.apply(request.number());
        } catch (DecodingException e) {
            // Just consume the [<entry-name> <entry-type-req>] and ignore it
            // See RFC4551 3.4. MODSEQ Search Criterion in SEARCH
            request.consumeQuoted();
            request.consumeWord(chr -> true);
            return ModSeq.apply(request.number());
        }
    }

    private SearchKey paren(ImapSession session, ImapRequestLineReader request, Charset charset) throws DecodingException {
        request.consume();
        List<SearchKey> keys = new ArrayList<>();
        addUntilParen(session, request, keys, charset);
        return And.apply(CollectionConverters.asScala(keys).toSeq());
    }

    private void addUntilParen(ImapSession session, ImapRequestLineReader request, List<SearchKey> keys, Charset charset) throws DecodingException {
        final char next = request.nextWordChar();
        if (next == ')') {
            request.consume();
        } else {
            final SearchKey key = searchKey(session, request, null, false);
            keys.add(key);
            addUntilParen(session, request, keys, charset);
        }
    }

    private int consumeAndCap(ImapRequestLineReader request) throws DecodingException {
        final char next = request.consume();
        return ImapRequestLineReader.cap(next);
    }
    
    private SearchKey cc(ImapRequestLineReader request, Charset charset) throws DecodingException {
        final SearchKey result;
        nextIsSpace(request);
        final String value = request.astring(charset);
        result = Cc.apply(value);
        return result;
    }

    private SearchKey c(ImapSession session, ImapRequestLineReader request, boolean isFirstToken, Charset charset) throws DecodingException, IllegalCharsetNameException, UnsupportedCharsetException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'C':
            return cc(request, charset);
        case 'H':
            return charset(session, request, isFirstToken);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey charset(ImapSession session, ImapRequestLineReader request, boolean isFirstToken) throws DecodingException, IllegalCharsetNameException, UnsupportedCharsetException {
        final SearchKey result;
        nextIsA(request);
        nextIsR(request);
        nextIsS(request);
        nextIsE(request);
        nextIsT(request);
        nextIsSpace(request);
        if (!isFirstToken) {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
        final String value = request.astring();
        final Charset charset = Charset.forName(value);
        request.nextWordChar();
        result = searchKey(session, request, charset, false);
        return result;
    }

    private SearchKey u(ImapRequestLineReader request) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'I':
            return uid(request);
        case 'N':
            return un(request);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey un(ImapRequestLineReader request) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'A':
            return unanswered(request);
        case 'D':
            return und(request);
        case 'F':
            return unflagged(request);
        case 'K':
            return unkeyword(request);
        case 'S':
            return unseen(request);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey und(ImapRequestLineReader request) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'E':
            return undeleted(request);
        case 'R':
            return undraft(request);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey t(ImapRequestLineReader request, Charset charset) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'E':
            return text(request, charset);
        case 'O':
            return to(request, charset);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey s(ImapRequestLineReader request, Charset charset) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'E':
            return se(request);
        case 'I':
            return since(request);
        case 'M':
            return smaller(request);
        case 'U':
            return subject(request, charset);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey se(ImapRequestLineReader request) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'E':
            return seen(request);
        case 'N':
            return sen(request);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey sen(ImapRequestLineReader request) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'T':
            return sent(request);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey sent(ImapRequestLineReader request) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'B':
            return sentBefore(request);
        case 'O':
            return sentOn(request);
        case 'S':
            return sentSince(request);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey o(ImapSession session, ImapRequestLineReader request, Charset charset) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'L':
            return old(request);
        case 'N':
            return on(request);
        case 'R':
            return or(session, request, charset);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }
    
    private SearchKey old(ImapRequestLineReader request) throws DecodingException {
        nextIsD(request);
        try {
            // Check if its OLDER keyword
            nextIsE(request);
            return older(request);
        } catch (DecodingException e) {
            return Old.apply();
        }
    }
    
    private SearchKey n(ImapSession session, ImapRequestLineReader request, Charset charset) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'E':
            return newOperator(request);
        case 'O':
            return not(session, request, charset);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey f(ImapRequestLineReader request, Charset charset) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'L':
            return flagged(request);
        case 'R':
            return from(request, charset);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey d(ImapRequestLineReader request) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'E':
            return deleted(request);
        case 'R':
            return draft(request);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey keyword(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsE(request);
        nextIsY(request);
        nextIsW(request);
        nextIsO(request);
        nextIsR(request);
        nextIsD(request);
        nextIsSpace(request);
        final String value = request.atom();
        result = Keyword.apply(value);
        return result;
    }

    private SearchKey unkeyword(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsE(request);
        nextIsY(request);
        nextIsW(request);
        nextIsO(request);
        nextIsR(request);
        nextIsD(request);
        nextIsSpace(request);
        final String value = request.atom();
        result = UnKeyword.apply(value);
        return result;
    }

    private SearchKey header(ImapRequestLineReader request, Charset charset) throws DecodingException {
        final SearchKey result;
        nextIsE(request);
        nextIsA(request);
        nextIsD(request);
        nextIsE(request);
        nextIsR(request);
        nextIsSpace(request);
        final String field = request.astring(charset);
        nextIsSpace(request);
        final String value = request.astring(charset);
        result = Header.apply(field, value);
        return result;
    }

    private SearchKey larger(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsA(request);
        nextIsR(request);
        nextIsG(request);
        nextIsE(request);
        nextIsR(request);
        nextIsSpace(request);
        final long value = request.number();
        result = Larger.apply(value);
        return result;
    }

    private SearchKey smaller(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsA(request);
        nextIsL(request);
        nextIsL(request);
        nextIsE(request);
        nextIsR(request);
        nextIsSpace(request);
        final long value = request.number();
        result = Smaller.apply(value);
        return result;
    }

    private SearchKey from(ImapRequestLineReader request, Charset charset) throws DecodingException {
        final SearchKey result;
        nextIsO(request);
        nextIsM(request);
        nextIsSpace(request);
        final String value = request.astring(charset);
        result = From.apply(value);
        return result;
    }

    private SearchKey flagged(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsA(request);
        nextIsG(request);
        nextIsG(request);
        nextIsE(request);
        nextIsD(request);
        result = Flagged.apply();
        return result;
    }

    private SearchKey unseen(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsE(request);
        nextIsE(request);
        nextIsN(request);
        result = UnSeen.apply();
        return result;
    }

    private SearchKey undraft(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsA(request);
        nextIsF(request);
        nextIsT(request);
        result = UnDraft.apply();
        return result;
    }

    private SearchKey undeleted(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsL(request);
        nextIsE(request);
        nextIsT(request);
        nextIsE(request);
        nextIsD(request);
        result = UnDeleted.apply();
        return result;
    }

    private SearchKey unflagged(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsL(request);
        nextIsA(request);
        nextIsG(request);
        nextIsG(request);
        nextIsE(request);
        nextIsD(request);
        result = UnFlagged.apply();
        return result;
    }

    private SearchKey unanswered(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsN(request);
        nextIsS(request);
        nextIsW(request);
        nextIsE(request);
        nextIsR(request);
        nextIsE(request);
        nextIsD(request);
        result = UnAnswered.apply();
        return result;
    }
    
    private SearchKey younger(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsO(request);
        nextIsU(request);
        nextIsN(request);
        nextIsG(request);
        nextIsE(request);
        nextIsR(request);
        nextIsSpace(request);
        result = Younger.apply(request.nzNumber());
        return result;
    }
    
    private SearchKey older(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsR(request);
        
        nextIsSpace(request);
        result = Older.apply(request.nzNumber());
        return result;
    }

    private SearchKey or(ImapSession session, ImapRequestLineReader request, Charset charset) throws DecodingException {
        final SearchKey result;
        nextIsSpace(request);
        final SearchKey firstKey = searchKey(session, request, charset, false);
        nextIsSpace(request);
        final SearchKey secondKey = searchKey(session, request, charset, false);
        result = Or.apply(firstKey, secondKey);
        return result;
    }

    private SearchKey not(ImapSession session, ImapRequestLineReader request, Charset charset) throws DecodingException {
        final SearchKey result;
        nextIsT(request);
        nextIsSpace(request);
        final SearchKey nextKey = searchKey(session, request, charset, false);
        result = Not.apply(nextKey);
        return result;
    }

    private SearchKey newOperator(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsW(request);
        result = New.apply();
        return result;
    }

    private SearchKey recent(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        //nextIsE(request);
        //nextIsC(request);
        nextIsE(request);
        nextIsN(request);
        nextIsT(request);
        result = Recent.apply();
        return result;
    }

    private SearchKey seen(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsN(request);
        result = Seen.apply();
        return result;
    }

    private SearchKey draft(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsA(request);
        nextIsF(request);
        nextIsT(request);
        result = Draft.apply();
        return result;
    }

    private SearchKey deleted(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsL(request);
        nextIsE(request);
        nextIsT(request);
        nextIsE(request);
        nextIsD(request);
        result = Deleted.apply();
        return result;
    }

    private SearchKey b(ImapRequestLineReader request, Charset charset) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'C':
            return bcc(request, charset);
        case 'E':
            return before(request);
        case 'O':
            return body(request, charset);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey body(ImapRequestLineReader request, Charset charset) throws DecodingException {
        final SearchKey result;
        nextIsD(request);
        nextIsY(request);
        nextIsSpace(request);
        final String value = request.astring(charset);
        result = Body.apply(value);
        return result;
    }

    private SearchKey on(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsSpace(request);
        final DayMonthYear value = request.date();
        result = On.apply(value);
        return result;
    }

    private SearchKey sentBefore(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsE(request);
        nextIsF(request);
        nextIsO(request);
        nextIsR(request);
        nextIsE(request);
        nextIsSpace(request);
        final DayMonthYear value = request.date();
        result = SentBefore.apply(value);
        return result;
    }

    private SearchKey sentSince(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsI(request);
        nextIsN(request);
        nextIsC(request);
        nextIsE(request);
        nextIsSpace(request);
        final DayMonthYear value = request.date();
        result = SentSince.apply(value);
        return result;
    }

    private SearchKey since(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsN(request);
        nextIsC(request);
        nextIsE(request);
        nextIsSpace(request);
        final DayMonthYear value = request.date();
        result = Since.apply(value);
        return result;
    }

    private SearchKey sentOn(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsN(request);
        nextIsSpace(request);
        final DayMonthYear value = request.date();
        result = SentOn.apply(value);
        return result;
    }

    private SearchKey before(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsF(request);
        nextIsO(request);
        nextIsR(request);
        nextIsE(request);
        nextIsSpace(request);
        final DayMonthYear value = request.date();
        result = Before.apply(value);
        return result;
    }

    private SearchKey bcc(ImapRequestLineReader request, Charset charset) throws DecodingException {
        final SearchKey result;
        nextIsC(request);
        nextIsSpace(request);
        final String value = request.astring(charset);
        result = Bcc.apply(value);
        return result;
    }

    private SearchKey text(ImapRequestLineReader request, Charset charset) throws DecodingException {
        final SearchKey result;
        nextIsX(request);
        nextIsT(request);
        nextIsSpace(request);
        final String value = request.astring(charset);
        result = Text.apply(value);
        return result;
    }

    private SearchKey uid(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsD(request);
        nextIsSpace(request);
        final List<UidRange> ranges = ImmutableList.copyOf(request.parseUidRange());
        result = Uid.apply(CollectionConverters.asScala(ranges).toSeq());
        return result;
    }

    private SearchKey sequenceSet(ImapSession session, ImapRequestLineReader request) throws DecodingException {
        final List<IdRange> ranges = ImmutableList.copyOf(request.parseIdRange(session));
        return SequenceNumbers.apply(CollectionConverters.asScala(ranges).toSeq());
    }

    private SearchKey to(ImapRequestLineReader request, Charset charset) throws DecodingException {
        final SearchKey result;
        nextIsSpace(request);
        final String value = request.astring(charset);
        result = To.apply(value);
        return result;
    }

    private SearchKey subject(ImapRequestLineReader request, Charset charset) throws DecodingException {
        final SearchKey result;
        nextIsB(request);
        nextIsJ(request);
        nextIsE(request);
        nextIsC(request);
        nextIsT(request);
        nextIsSpace(request);
        final String value = request.astring(charset);
        result = Subject.apply(value);
        return result;
    }

    private SearchKey a(ImapRequestLineReader request) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
        case 'L':
            return all(request);
        case 'N':
            return answered(request);
        default:
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey answered(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsS(request);
        nextIsW(request);
        nextIsE(request);
        nextIsR(request);
        nextIsE(request);
        nextIsD(request);
        result = Answered.apply();
        return result;
    }

    private SearchKey all(ImapRequestLineReader request) throws DecodingException {
        final SearchKey result;
        nextIsL(request);
        result = All.apply();
        return result;
    }

    private void nextIsSpace(ImapRequestLineReader request) throws DecodingException {
        final char next = request.consume();
        if (next != ' ') {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private void nextIsG(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'G', 'g');
    }

    private void nextIsM(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'M', 'm');
    }

    private void nextIsI(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'I', 'i');
    }

    private void nextIsN(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'N', 'n');
    }

    private void nextIsA(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'A', 'a');
    }

    private void nextIsT(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'T', 't');
    }

    private void nextIsY(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'Y', 'y');
    }

    private void nextIsX(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'X', 'x');
    }
    
    private void nextIsU(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'U', 'u');
    }
    
    private void nextIsO(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'O', 'o');
    }

    private void nextIsQ(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'Q', 'q');
    }

    private void nextIsF(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'F', 'f');
    }

    private void nextIsJ(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'J', 'j');
    }

    private void nextIsC(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'C', 'c');
    }

    private void nextIsD(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'D', 'd');
    }

    private void nextIsB(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'B', 'b');
    }

    private void nextIsR(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'R', 'r');
    }

    private void nextIsE(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'E', 'e');
    }

    private void nextIsW(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'W', 'w');
    }

    private void nextIsS(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'S', 's');
    }

    private void nextIsL(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'L', 'l');
    }


    private void nextIsV(ImapRequestLineReader request) throws DecodingException {
        nextIs(request, 'V', 'v');
    }

    private void nextIs(ImapRequestLineReader request, char upper, char lower) throws DecodingException {
        final char next = request.consume();
        if (next != upper && next != lower) {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    public SearchKey decode(ImapSession session, ImapRequestLineReader request) throws DecodingException, IllegalCharsetNameException, UnsupportedCharsetException {
        request.nextWordChar();
        final SearchKey firstKey = searchKey(session, request, null, true);
        final SearchKey result;
        if (request.nextChar() == ' ') {
            List<SearchKey> keys = new ArrayList<>();
            keys.add(firstKey);
            while (request.nextChar() == ' ') {
                request.nextWordChar();
                final SearchKey key = searchKey(session, request, null, false);
                keys.add(key);
            }
            result = And.apply(CollectionConverters.asScala(keys).toSeq());
        } else {
            result = firstKey;
        }
        request.eol();
        return result;
    }

    private ImapMessage unsupportedCharset(Tag tag) {
        final ResponseCode badCharset = StatusResponse.ResponseCode.badCharset();
        return taggedNo(tag, ImapConstants.SEARCH_COMMAND, HumanReadableText.BAD_CHARSET, badCharset);
    }

    /**
     * Parse the {@link SearchResultOption}'s which are used for ESEARCH
     */
    private List<SearchResultOption> parseOptions(ImapRequestLineReader reader) throws DecodingException {
        List<SearchResultOption> options = new ArrayList<>();
        reader.consumeChar('(');
        reader.nextWordChar();
        
        int cap = consumeAndCap(reader);

        while (cap != ')') {
            switch (cap) {
            case 'A':
                nextIsL(reader);
                nextIsL(reader);
                options.add(SearchResultOption.ALL);
                break;
            case 'C':
                nextIsO(reader);
                nextIsU(reader);
                nextIsN(reader);
                nextIsT(reader);
                options.add(SearchResultOption.COUNT);
                break;
            case 'M':
                final int c = consumeAndCap(reader);
                switch (c) {
                case 'A':
                    nextIsX(reader);
                    options.add(SearchResultOption.MAX);
                    break;
                case 'I':
                    nextIsN(reader);
                    options.add(SearchResultOption.MIN);
                    break;
                default:
                    throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
                }
                break;
            // Check for SAVE options which is part of the SEARCHRES extension
            case 'S':
                nextIsA(reader);
                nextIsV(reader);
                nextIsE(reader);
                options.add(SearchResultOption.SAVE);
                break;
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
            }
            reader.nextWordChar();
            cap = consumeAndCap(reader);
        }
        // if the options are empty then we parsed RETURN () which is a shortcut for ALL.
        // See http://www.faqs.org/rfcs/rfc4731.html 3.1
        if (options.isEmpty()) {
            options.add(SearchResultOption.ALL);
        }
        return options;
    }
    
    @Override
    protected ImapMessage decode(ImapRequestLineReader request, Tag tag, boolean useUids, ImapSession session) throws DecodingException {
        try {
            SearchKey recent = null;
            List<SearchResultOption> options = null;
            int c = ImapRequestLineReader.cap(request.nextWordChar());
            if (c == 'R') {
                // if we found a R its either RECENT or RETURN so consume it
                request.consume();
                
                nextIsE(request);
                c = consumeAndCap(request);

                switch (c) {
                case 'C':
                    recent = recent(request);
                    break;
                case 'T': 
                    nextIsU(request);
                    nextIsR(request);
                    nextIsN(request);
                    request.nextWordChar();
                    options = parseOptions(request);
                    break;

                default:
                    throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
                }
            }
            final SearchKey finalKey;

            if (recent != null) {
                if (request.nextChar() != ' ') {
                    request.eol();
                    finalKey = recent;
                } else {
                    // Parse the search term from the request
                    final SearchKey key = decode(session, request);
                    List<SearchKey> list = Arrays.asList(recent, key);
                    finalKey = And.apply(CollectionConverters.asScala(list).toSeq());
                }
            } else {
                // Parse the search term from the request
                finalKey = decode(session, request);
            }
           
            
            
            if (options == null) {
                options = new ArrayList<>();
            }

            return new SearchRequest(new SearchOperation(finalKey, options), useUids, tag);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            LOGGER.debug("Unable to decode request", e);
            return unsupportedCharset(tag);
        }
    }

}
