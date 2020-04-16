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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.UidRange;
import org.apache.james.imap.api.message.request.All;
import org.apache.james.imap.api.message.request.Answered;
import org.apache.james.imap.api.message.request.Bcc;
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
import org.apache.james.imap.api.message.request.New;
import org.apache.james.imap.api.message.request.Not;
import org.apache.james.imap.api.message.request.Old;
import org.apache.james.imap.api.message.request.On;
import org.apache.james.imap.api.message.request.Or;
import org.apache.james.imap.api.message.request.Recent;
import org.apache.james.imap.api.message.request.SearchKey;
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
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.decode.DecodingException;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.decode.ImapRequestStreamLineReader;
import org.apache.james.mailbox.MessageUid;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import scala.jdk.javaapi.CollectionConverters;

public class SearchCommandParserSearchKeyTest {

    private static final DayMonthYear DATE = new DayMonthYear(1, 1, 2000);

    SearchCommandParser parser;
    ImapCommand command;
    ImapMessage message;

    @Before
    public void setUp() throws Exception {
        parser = new SearchCommandParser(mock(StatusResponseFactory.class));
        command = ImapCommand.anyStateCommand("Command");
    }

    @Test
    public void testShouldParseAll() throws Exception {
        SearchKey key = All.apply();
        checkValid("ALL\r\n", key);
        checkValid("all\r\n", key);
        checkValid("alL\r\n", key);
        checkInvalid("al\r\n", key);
        checkInvalid("alm\r\n", key);
        checkInvalid("alm\r\n", key);
    }

    @Test
    public void testShouldParseAnswered() throws Exception {
        SearchKey key = Answered.apply();
        checkValid("ANSWERED\r\n", key);
        checkValid("answered\r\n", key);
        checkValid("aNSWEred\r\n", key);
        checkInvalid("a\r\n", key);
        checkInvalid("an\r\n", key);
        checkInvalid("ans\r\n", key);
        checkInvalid("answ\r\n", key);
        checkInvalid("answe\r\n", key);
        checkInvalid("answer\r\n", key);
        checkInvalid("answere\r\n", key);
    }

    @Test
    public void testShouldParseBcc() throws Exception {
        SearchKey key = Bcc.apply("Somebody");
        checkValid("BCC Somebody\r\n", key);
        checkValid("BCC \"Somebody\"\r\n", key);
        checkValid("bcc Somebody\r\n", key);
        checkValid("bcc \"Somebody\"\r\n", key);
        checkValid("Bcc Somebody\r\n", key);
        checkValid("Bcc \"Somebody\"\r\n", key);
        checkInvalid("b\r\n", key);
        checkInvalid("bc\r\n", key);
        checkInvalid("bg\r\n", key);
        checkInvalid("bccc\r\n", key);
    }

    @Test
    public void testShouldParseOn() throws Exception {
        SearchKey key = On.apply(DATE);
        checkValid("ON 1-Jan-2000\r\n", key);
        checkValid("on 1-Jan-2000\r\n", key);
        checkValid("oN 1-Jan-2000\r\n", key);
        checkInvalid("o\r\n", key);
        checkInvalid("om\r\n", key);
        checkInvalid("oni\r\n", key);
        checkInvalid("on \r\n", key);
        checkInvalid("on 1\r\n", key);
        checkInvalid("on 1-\r\n", key);
        checkInvalid("on 1-J\r\n", key);
        checkInvalid("on 1-Ja\r\n", key);
        checkInvalid("on 1-Jan\r\n", key);
        checkInvalid("on 1-Jan-\r\n", key);
    }

    @Test
    public void testShouldParseSentBefore() throws Exception {
        SearchKey key = SentBefore.apply(DATE);
        checkValid("SENTBEFORE 1-Jan-2000\r\n", key);
        checkValid("sentbefore 1-Jan-2000\r\n", key);
        checkValid("SentBefore 1-Jan-2000\r\n", key);
        checkInvalid("s\r\n", key);
        checkInvalid("se\r\n", key);
        checkInvalid("sent\r\n", key);
        checkInvalid("sentb \r\n", key);
        checkInvalid("sentbe \r\n", key);
        checkInvalid("sentbef \r\n", key);
        checkInvalid("sentbefo \r\n", key);
        checkInvalid("sentbefor \r\n", key);
        checkInvalid("sentbefore \r\n", key);
        checkInvalid("sentbefore 1\r\n", key);
        checkInvalid("sentbefore 1-\r\n", key);
        checkInvalid("sentbefore 1-J\r\n", key);
        checkInvalid("sentbefore 1-Ja\r\n", key);
        checkInvalid("sentbefore 1-Jan\r\n", key);
        checkInvalid("sentbefore 1-Jan-\r\n", key);
    }

    @Test
    public void testShouldParseSentOn() throws Exception {
        SearchKey key = SentOn.apply(DATE);
        checkValid("SENTON 1-Jan-2000\r\n", key);
        checkValid("senton 1-Jan-2000\r\n", key);
        checkValid("SentOn 1-Jan-2000\r\n", key);
        checkInvalid("s\r\n", key);
        checkInvalid("se\r\n", key);
        checkInvalid("sent\r\n", key);
        checkInvalid("sento \r\n", key);
        checkInvalid("senton \r\n", key);
        checkInvalid("senton 1\r\n", key);
        checkInvalid("senton 1-\r\n", key);
        checkInvalid("senton 1-J\r\n", key);
        checkInvalid("senton 1-Ja\r\n", key);
        checkInvalid("senton 1-Jan\r\n", key);
        checkInvalid("senton 1-Jan-\r\n", key);
    }

    @Test
    public void testShouldParseSentSince() throws Exception {
        SearchKey key = SentSince.apply(DATE);
        checkValid("SENTSINCE 1-Jan-2000\r\n", key);
        checkValid("sentsince 1-Jan-2000\r\n", key);
        checkValid("SentSince 1-Jan-2000\r\n", key);
        checkInvalid("s\r\n", key);
        checkInvalid("se\r\n", key);
        checkInvalid("sent\r\n", key);
        checkInvalid("sents \r\n", key);
        checkInvalid("sentsi \r\n", key);
        checkInvalid("sentsin \r\n", key);
        checkInvalid("sentsinc \r\n", key);
        checkInvalid("sentsince \r\n", key);
        checkInvalid("sentsince 1\r\n", key);
        checkInvalid("sentsince 1-\r\n", key);
        checkInvalid("sentsince 1-J\r\n", key);
        checkInvalid("sentsince 1-Ja\r\n", key);
        checkInvalid("sentsince 1-Jan\r\n", key);
        checkInvalid("sentsince 1-Jan-\r\n", key);
    }

    @Test
    public void testShouldParseSince() throws Exception {
        SearchKey key = Since.apply(DATE);
        checkValid("SINCE 1-Jan-2000\r\n", key);
        checkValid("since 1-Jan-2000\r\n", key);
        checkValid("Since 1-Jan-2000\r\n", key);
        checkInvalid("s \r\n", key);
        checkInvalid("si \r\n", key);
        checkInvalid("sin \r\n", key);
        checkInvalid("sinc \r\n", key);
        checkInvalid("since \r\n", key);
        checkInvalid("since 1\r\n", key);
        checkInvalid("since 1-\r\n", key);
        checkInvalid("since 1-J\r\n", key);
        checkInvalid("since 1-Ja\r\n", key);
        checkInvalid("since 1-Jan\r\n", key);
        checkInvalid("since 1-Jan-\r\n", key);
    }

    @Test
    public void testShouldParseBefore() throws Exception {
        SearchKey key = org.apache.james.imap.api.message.request.Before.apply(DATE);
        checkValid("BEFORE 1-Jan-2000\r\n", key);
        checkValid("before 1-Jan-2000\r\n", key);
        checkValid("BEforE 1-Jan-2000\r\n", key);
        checkInvalid("b\r\n", key);
        checkInvalid("B\r\n", key);
        checkInvalid("BE\r\n", key);
        checkInvalid("BEf\r\n", key);
        checkInvalid("BEfo\r\n", key);
        checkInvalid("BEfor\r\n", key);
        checkInvalid("BEforE\r\n", key);
        checkInvalid("BEforEi\r\n", key);
        checkInvalid("BEforE \r\n", key);
        checkInvalid("BEforE 1\r\n", key);
        checkInvalid("BEforE 1-\r\n", key);
        checkInvalid("BEforE 1-J\r\n", key);
        checkInvalid("BEforE 1-Ja\r\n", key);
        checkInvalid("BEforE 1-Jan\r\n", key);
        checkInvalid("BEforE 1-Jan-\r\n", key);
    }

    @Test
    public void testShouldParseBody() throws Exception {
        SearchKey key = Body.apply("Text");
        checkValid("BODY Text\r\n", key);
        checkValid("BODY \"Text\"\r\n", key);
        checkValid("body Text\r\n", key);
        checkValid("body \"Text\"\r\n", key);
        checkValid("BodY Text\r\n", key);
        checkValid("BodY \"Text\"\r\n", key);
        checkInvalid("b\r\n", key);
        checkInvalid("Bo\r\n", key);
        checkInvalid("Bod\r\n", key);
        checkInvalid("Bodd\r\n", key);
        checkInvalid("Bodym\r\n", key);
    }

    @Test
    public void testShouldParseTo() throws Exception {
        SearchKey key = To.apply("AnAddress");
        checkValid("TO AnAddress\r\n", key);
        checkValid("TO \"AnAddress\"\r\n", key);
        checkValid("to AnAddress\r\n", key);
        checkValid("to \"AnAddress\"\r\n", key);
        checkValid("To AnAddress\r\n", key);
        checkValid("To \"AnAddress\"\r\n", key);
        checkInvalid("t\r\n", key);
        checkInvalid("to\r\n", key);
        checkInvalid("too\r\n", key);
        checkInvalid("to \r\n", key);
    }

    @Test
    public void testShouldParseText() throws Exception {
        SearchKey key = Text.apply("SomeText");
        checkValid("TEXT SomeText\r\n", key);
        checkValid("TEXT \"SomeText\"\r\n", key);
        checkValid("text SomeText\r\n", key);
        checkValid("text \"SomeText\"\r\n", key);
        checkValid("Text SomeText\r\n", key);
        checkValid("Text \"SomeText\"\r\n", key);
        checkInvalid("t\r\n", key);
        checkInvalid("te\r\n", key);
        checkInvalid("tex\r\n", key);
        checkInvalid("text\r\n", key);
        checkInvalid("text \r\n", key);
    }

    @Test
    public void testShouldParseSubject() throws Exception {
        SearchKey key = Subject.apply("ASubject");
        checkValid("SUBJECT ASubject\r\n", key);
        checkValid("SUBJECT \"ASubject\"\r\n", key);
        checkValid("subject ASubject\r\n", key);
        checkValid("subject \"ASubject\"\r\n", key);
        checkValid("Subject ASubject\r\n", key);
        checkValid("Subject \"ASubject\"\r\n", key);
        checkInvalid("s\r\n", key);
        checkInvalid("su\r\n", key);
        checkInvalid("sub\r\n", key);
        checkInvalid("subj\r\n", key);
        checkInvalid("subje\r\n", key);
        checkInvalid("subjec\r\n", key);
        checkInvalid("subject\r\n", key);
        checkInvalid("subject \r\n", key);
    }

    @Test
    public void testShouldParseCc() throws Exception {
        SearchKey key = Cc.apply("SomeText");
        checkValid("CC SomeText\r\n", key);
        checkValid("CC \"SomeText\"\r\n", key);
        checkValid("cc SomeText\r\n", key);
        checkValid("cc \"SomeText\"\r\n", key);
        checkValid("Cc SomeText\r\n", key);
        checkValid("Cc \"SomeText\"\r\n", key);
        checkInvalid("c\r\n", key);
        checkInvalid("cd\r\n", key);
        checkInvalid("ccc\r\n", key);
    }

    @Test
    public void testShouldParseFrom() throws Exception {
        SearchKey key = From.apply("Someone");
        checkValid("FROM Someone\r\n", key);
        checkValid("FROM \"Someone\"\r\n", key);
        checkValid("from Someone\r\n", key);
        checkValid("from \"Someone\"\r\n", key);
        checkValid("FRom Someone\r\n", key);
        checkValid("FRom \"Someone\"\r\n", key);
        checkInvalid("f\r\n", key);
        checkInvalid("fr\r\n", key);
        checkInvalid("ftom\r\n", key);
        checkInvalid("froml\r\n", key);
    }

    @Test
    public void testShouldParseKeyword() throws Exception {
        SearchKey key = Keyword.apply("AFlag");
        checkValid("KEYWORD AFlag\r\n", key);
        checkInvalid("KEYWORD \"AFlag\"\r\n", key);
        checkValid("keyword AFlag\r\n", key);
        checkInvalid("keyword \"AFlag\"\r\n", key);
        checkValid("KEYword AFlag\r\n", key);
        checkInvalid("KEYword \"AFlag\"\r\n", key);
        checkInvalid("k\r\n", key);
        checkInvalid("ke\r\n", key);
        checkInvalid("key\r\n", key);
        checkInvalid("keyw\r\n", key);
        checkInvalid("keywo\r\n", key);
        checkInvalid("keywor\r\n", key);
        checkInvalid("keywordi\r\n", key);
        checkInvalid("keyword \r\n", key);
    }

    @Test
    public void testShouldParseUnKeyword() throws Exception {
        SearchKey key = UnKeyword.apply("AFlag");
        checkValid("UNKEYWORD AFlag\r\n", key);
        checkInvalid("UNKEYWORD \"AFlag\"\r\n", key);
        checkValid("unkeyword AFlag\r\n", key);
        checkInvalid("unkeyword \"AFlag\"\r\n", key);
        checkValid("UnKEYword AFlag\r\n", key);
        checkInvalid("UnKEYword \"AFlag\"\r\n", key);
        checkInvalid("u\r\n", key);
        checkInvalid("un\r\n", key);
        checkInvalid("unk\r\n", key);
        checkInvalid("unke\r\n", key);
        checkInvalid("unkey\r\n", key);
        checkInvalid("unkeyw\r\n", key);
        checkInvalid("unkeywo\r\n", key);
        checkInvalid("unkeywor\r\n", key);
        checkInvalid("unkeywordi\r\n", key);
        checkInvalid("unkeyword \r\n", key);
    }

    @Test
    public void testShouldParseHeader() throws Exception {
        SearchKey key = Header.apply("Field", "Value");
        checkValid("HEADER Field Value\r\n", key);
        checkValid("HEADER \"Field\" \"Value\"\r\n", key);
        checkValid("header Field Value\r\n", key);
        checkValid("header \"Field\" \"Value\"\r\n", key);
        checkValid("HEAder Field Value\r\n", key);
        checkValid("HEAder \"Field\" \"Value\"\r\n", key);
        checkInvalid("h\r\n", key);
        checkInvalid("he\r\n", key);
        checkInvalid("hea\r\n", key);
        checkInvalid("head\r\n", key);
        checkInvalid("heade\r\n", key);
        checkInvalid("header\r\n", key);
        checkInvalid("header field\r\n", key);
        checkInvalid("header field \r\n", key);
    }

   
    private void checkValid(String input, SearchKey key) throws Exception {
        ImapRequestLineReader reader = new ImapRequestStreamLineReader(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.US_ASCII)),
                new ByteArrayOutputStream());

        assertThat(parser.searchKey(null, reader, null, false)).isEqualTo(key);
    }

    @Test
    public void testShouldParseDeleted() throws Exception {
        SearchKey key = Deleted.apply();
        checkValid("DELETED\r\n", key);
        checkValid("deleted\r\n", key);
        checkValid("deLEteD\r\n", key);
        checkInvalid("d\r\n", key);
        checkInvalid("de\r\n", key);
        checkInvalid("del\r\n", key);
        checkInvalid("dele\r\n", key);
        checkInvalid("delet\r\n", key);
        checkInvalid("delete\r\n", key);
    }

    @Test
    public void testEShouldBeInvalid() throws Exception {
        SearchKey key = Deleted.apply();
        checkInvalid("e\r\n", key);
        checkInvalid("ee\r\n", key);
    }

    @Test
    public void testGShouldBeInvalid() throws Exception {
        SearchKey key = Deleted.apply();
        checkInvalid("g\r\n", key);
        checkInvalid("G\r\n", key);
    }

    @Test
    public void testIShouldBeInvalid() throws Exception {
        SearchKey key = Deleted.apply();
        checkInvalid("i\r\n", key);
        checkInvalid("I\r\n", key);
    }

    @Test
    public void testJShouldBeInvalid() throws Exception {
        SearchKey key = Deleted.apply();
        checkInvalid("j\r\n", key);
        checkInvalid("J\r\n", key);
    }

    @Test
    public void testMShouldBeInvalid() throws Exception {
        SearchKey key = Deleted.apply();
        checkInvalid("m\r\n", key);
        checkInvalid("M\r\n", key);
    }
    
    @Test
    public void testPShouldBeInvalid() throws Exception {
        SearchKey key = Deleted.apply();
        checkInvalid("p\r\n", key);
        checkInvalid("Pp\r\n", key);
    }

    @Test
    public void testQShouldBeInvalid() throws Exception {
        SearchKey key = Deleted.apply();
        checkInvalid("q\r\n", key);
        checkInvalid("Qq\r\n", key);
    }

    @Test
    public void testWShouldBeInvalid() throws Exception {
        SearchKey key = Deleted.apply();
        checkInvalid("w\r\n", key);
        checkInvalid("ww\r\n", key);
    }

    @Test
    public void testVShouldBeInvalid() throws Exception {
        SearchKey key = Deleted.apply();
        checkInvalid("v\r\n", key);
        checkInvalid("vv\r\n", key);
    }

    @Test
    public void testXShouldBeInvalid() throws Exception {
        SearchKey key = Deleted.apply();
        checkInvalid("x\r\n", key);
        checkInvalid("xx\r\n", key);
    }

    @Test
    public void testYShouldBeInvalid() throws Exception {
        SearchKey key = Deleted.apply();
        checkInvalid("y\r\n", key);
        checkInvalid("yy\r\n", key);
    }

    @Test
    public void testZShouldBeInvalid() throws Exception {
        SearchKey key = Deleted.apply();
        checkInvalid("z\r\n", key);
        checkInvalid("zz\r\n", key);
    }

    @Test
    public void testShouldParseRecent() throws Exception {
        SearchKey key = Recent.apply();
        checkValid("RECENT\r\n", key);
        checkValid("recent\r\n", key);
        checkValid("reCENt\r\n", key);
        checkInvalid("r\r\n", key);
        checkInvalid("re\r\n", key);
        checkInvalid("rec\r\n", key);
        checkInvalid("rece\r\n", key);
        checkInvalid("recen\r\n", key);
    }

    @Test
    public void testShouldParseDraft() throws Exception {
        SearchKey key = Draft.apply();
        checkValid("DRAFT\r\n", key);
        checkValid("draft\r\n", key);
        checkValid("DRaft\r\n", key);
        checkInvalid("D\r\n", key);
        checkInvalid("DR\r\n", key);
        checkInvalid("DRA\r\n", key);
        checkInvalid("DRAF\r\n", key);
    }

    @Test
    public void testShouldParseUnanswered() throws Exception {
        SearchKey key = UnAnswered.apply();
        checkValid("UNANSWERED\r\n", key);
        checkValid("unanswered\r\n", key);
        checkValid("UnAnswered\r\n", key);
        checkInvalid("u\r\n", key);
        checkInvalid("un\r\n", key);
        checkInvalid("una\r\n", key);
        checkInvalid("unan\r\n", key);
        checkInvalid("unans\r\n", key);
        checkInvalid("unansw\r\n", key);
        checkInvalid("unanswe\r\n", key);
        checkInvalid("unanswer\r\n", key);
        checkInvalid("unanswere\r\n", key);
    }

    @Test
    public void testShouldParseUndeleted() throws Exception {
        SearchKey key = UnDeleted.apply();
        checkValid("UNDELETED\r\n", key);
        checkValid("undeleted\r\n", key);
        checkValid("UnDeleted\r\n", key);
        checkInvalid("u\r\n", key);
        checkInvalid("un\r\n", key);
        checkInvalid("und\r\n", key);
        checkInvalid("unde\r\n", key);
        checkInvalid("undel\r\n", key);
        checkInvalid("undele\r\n", key);
        checkInvalid("undelet\r\n", key);
        checkInvalid("undelete\r\n", key);
    }

    @Test
    public void testShouldParseUnseen() throws Exception {
        SearchKey key = UnSeen.apply();
        checkValid("UNSEEN\r\n", key);
        checkValid("unseen\r\n", key);
        checkValid("UnSeen\r\n", key);
        checkInvalid("u\r\n", key);
        checkInvalid("un\r\n", key);
        checkInvalid("uns\r\n", key);
        checkInvalid("unse\r\n", key);
        checkInvalid("unsee\r\n", key);
    }

    @Test
    public void testShouldParseUndraft() throws Exception {
        SearchKey key = UnDraft.apply();
        checkValid("UNDRAFT\r\n", key);
        checkValid("undraft\r\n", key);
        checkValid("UnDraft\r\n", key);
        checkInvalid("u\r\n", key);
        checkInvalid("un\r\n", key);
        checkInvalid("und\r\n", key);
        checkInvalid("undr\r\n", key);
        checkInvalid("undra\r\n", key);
        checkInvalid("undraf\r\n", key);
    }

    @Test
    public void testShouldParseUnflagged() throws Exception {
        SearchKey key = UnFlagged.apply();
        checkValid("UNFLAGGED\r\n", key);
        checkValid("unflagged\r\n", key);
        checkValid("UnFlagged\r\n", key);
        checkInvalid("u\r\n", key);
        checkInvalid("un\r\n", key);
        checkInvalid("unf\r\n", key);
        checkInvalid("unfl\r\n", key);
        checkInvalid("unfla\r\n", key);
        checkInvalid("unflag\r\n", key);
        checkInvalid("unflagg\r\n", key);
        checkInvalid("unflagge\r\n", key);
    }

    @Test
    public void testShouldParseSeen() throws Exception {
        SearchKey key = Seen.apply();
        checkValid("SEEN\r\n", key);
        checkValid("seen\r\n", key);
        checkValid("SEen\r\n", key);
        checkInvalid("s\r\n", key);
        checkInvalid("se\r\n", key);
        checkInvalid("see\r\n", key);
    }

    @Test
    public void testShouldParseNew() throws Exception {
        SearchKey key = New.apply();
        checkValid("NEW\r\n", key);
        checkValid("new\r\n", key);
        checkValid("NeW\r\n", key);
        checkInvalid("n\r\n", key);
        checkInvalid("ne\r\n", key);
        checkInvalid("nwe\r\n", key);
    }

    @Test
    public void testShouldParseOld() throws Exception {
        SearchKey key = Old.apply();
        checkValid("OLD\r\n", key);
        checkValid("old\r\n", key);
        checkValid("oLd\r\n", key);
        checkInvalid("o\r\n", key);
        checkInvalid("ol\r\n", key);
        checkInvalid("olr\r\n", key);
    }

    @Test
    public void testShouldParseFlagged() throws Exception {
        SearchKey key = Flagged.apply();
        checkValid("FLAGGED\r\n", key);
        checkValid("flagged\r\n", key);
        checkValid("FLAGged\r\n", key);
        checkInvalid("F\r\n", key);
        checkInvalid("FL\r\n", key);
        checkInvalid("FLA\r\n", key);
        checkInvalid("FLAG\r\n", key);
        checkInvalid("FLAGG\r\n", key);
        checkInvalid("FLAGGE\r\n", key);
        checkInvalid("FLoas\r\n", key);
    }

    @Test
    public void testShouldParseSmaller() throws Exception {
        SearchKey key = Smaller.apply(1729);
        checkValid("SMALLER 1729\r\n", key);
        checkValid("smaller 1729\r\n", key);
        checkValid("SMaller 1729\r\n", key);
        checkInvalid("s\r\n", key);
        checkInvalid("sm\r\n", key);
        checkInvalid("sma\r\n", key);
        checkInvalid("smal\r\n", key);
        checkInvalid("small\r\n", key);
        checkInvalid("smalle\r\n", key);
        checkInvalid("smaller \r\n", key);
        checkInvalid("smaller peach\r\n", key);
    }

    @Test
    public void testShouldParseLarger() throws Exception {
        SearchKey key = Larger.apply(1234);
        checkValid("LARGER 1234\r\n", key);
        checkValid("lArGEr 1234\r\n", key);
        checkValid("larger 1234\r\n", key);
        checkInvalid("l\r\n", key);
        checkInvalid("la\r\n", key);
        checkInvalid("lar\r\n", key);
        checkInvalid("larg\r\n", key);
        checkInvalid("large\r\n", key);
        checkInvalid("larger\r\n", key);
        checkInvalid("larger \r\n", key);
        checkInvalid("larger peach\r\n", key);
    }

    @Test
    public void testShouldParseUid() throws Exception {
        List<UidRange> range = ImmutableList.of(new UidRange(MessageUid.of(1)));
        SearchKey key = Uid.apply(CollectionConverters.asScala(range).toSeq());
        checkValid("UID 1\r\n", key);
        checkValid("Uid 1\r\n", key);
        checkValid("uid 1\r\n", key);
        checkInvalid("u\r\n", key);
        checkInvalid("ui\r\n", key);
        checkInvalid("uid\r\n", key);
        checkInvalid("uid \r\n", key);
    }

    @Test
    public void testShouldParseNot() throws Exception {
        SearchKey notdKey = Seen.apply();
        SearchKey key = Not.apply(notdKey);
        checkValid("NOT SEEN\r\n", key);
        checkValid("Not seen\r\n", key);
        checkValid("not Seen\r\n", key);
        checkInvalid("n\r\n", key);
        checkInvalid("no\r\n", key);
        checkInvalid("not\r\n", key);
        checkInvalid("not \r\n", key);
    }

    @Test
    public void testShouldParseOr() throws Exception {
        SearchKey oneKey = Seen.apply();
        SearchKey twoKey = Draft.apply();
        SearchKey key = Or.apply(oneKey, twoKey);
        checkValid("OR SEEN DRAFT\r\n", key);
        checkValid("oR seen draft\r\n", key);
        checkValid("or Seen drAFT\r\n", key);
        checkInvalid("o\r\n", key);
        checkInvalid("or\r\n", key);
        checkInvalid("or \r\n", key);
        checkInvalid("or seen\r\n", key);
        checkInvalid("or seen \r\n", key);
    }

    @Test
    public void testShouldParseSequenceSet() throws Exception {
        checkSequenceSet(1);
        checkSequenceSet(2);
        checkSequenceSet(3);
        checkSequenceSet(4);
        checkSequenceSet(5);
        checkSequenceSet(6);
        checkSequenceSet(7);
        checkSequenceSet(8);
        checkSequenceSet(9);
        checkSequenceSet(10);
        checkSequenceSet(121);
        checkSequenceSet(11354);
        checkSequenceSet(145644656);
        checkSequenceSet(1456452213);
    }

    private void checkSequenceSet(int number) throws Exception {
        List<IdRange> range = ImmutableList.of(new IdRange(number));
        SearchKey key = SequenceNumbers.apply(CollectionConverters.asScala(range).toSeq());
        checkValid(number + "\r\n", key);
    }

    private void checkInvalid(String input, SearchKey key)
            throws Exception {
        ImapRequestLineReader reader = new ImapRequestStreamLineReader(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.US_ASCII)),
                new ByteArrayOutputStream());

        try {
            parser.searchKey(null, reader, null, false);
            fail("Expected protocol exception to be throw since input is invalid");
        } catch (DecodingException e) {
            // expected
        }
    }
}
