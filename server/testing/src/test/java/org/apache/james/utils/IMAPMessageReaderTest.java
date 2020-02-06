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

package org.apache.james.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.utils.IMAPMessageReader.Utf8IMAPClient;
import org.junit.jupiter.api.Test;

class IMAPMessageReaderTest {
    static final Utf8IMAPClient NULL_IMAP_CLIENT = null;
    IMAPMessageReader testee = new IMAPMessageReader(NULL_IMAP_CLIENT);

    @Test
    void userReceivedMessageWithFlagsInMailboxShouldReturnTrueWhenSingleFlag() throws Exception {
        String replyString = "* 1 FETCH (FLAGS (\\Flagged) )\n" +
            "AAAC OK FETCH completed.";

        assertThat(testee.isCompletedWithFlags("\\Flagged", replyString))
            .isTrue();
    }

    @Test
    void userReceivedMessageWithFlagsInMailboxShouldReturnFalseWhenCompletedButNoFlag() throws Exception {
        String replyString = "* 1 FETCH (FLAGS (\\Seen) )\n" +
            "AAAC OK FETCH completed.";

        assertThat(testee.isCompletedWithFlags("\\Flagged", replyString))
            .isFalse();
    }

    @Test
    void userReceivedMessageWithFlagsInMailboxShouldReturnTrueWhenSeveralFlags() throws Exception {
        String replyString = "* 1 FETCH (FLAGS (\\Flagged \\Seen) )\n" +
            "AAAC OK FETCH completed.";

        assertThat(testee.isCompletedWithFlags("\\Flagged \\Seen", replyString))
            .isTrue();
    }

    @Test
    void userReceivedMessageWithFlagsInMailboxShouldReturnTrueWhenSeveralFlagsInAnyOrder() throws Exception {
        String replyString = "* 1 FETCH (FLAGS (\\Flagged \\Seen) )\n" +
            "AAAC OK FETCH completed.";

        assertThat(testee.isCompletedWithFlags("\\Seen \\Flagged", replyString))
            .isTrue();
    }
}