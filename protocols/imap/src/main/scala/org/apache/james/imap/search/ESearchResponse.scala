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

package org.apache.james.imap.search

import org.apache.james.imap.api.Tag
import org.apache.james.imap.api.message.{IdRange, UidRange}
import org.apache.james.imap.api.message.request.SearchResultOption
import org.apache.james.imap.api.message.response.ImapResponseMessage
import org.apache.james.mailbox.ModSeq

case class ESearchResponse(minUid: Option[Long],
                           maxUid: Option[Long],
                           count: Option[Long],
                           all: Seq[IdRange],
                           allUids: Seq[UidRange],
                           highestModSeq: ModSeq,
                           tag: Tag,
                           useUid: Boolean,
                           options: Seq[SearchResultOption])
  extends ImapResponseMessage {
}