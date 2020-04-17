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

import org.apache.james.imap.api.message.{IdRange, UidRange}
import org.apache.james.imap.api.message.request.{DayMonthYear, SearchResultOption}
import org.apache.james.imap.api.{ImapConstants, Tag}
import org.apache.james.imap.message.request.AbstractImapRequest

import scala.collection.immutable.Iterable

private[search] final class SearchRequest(val operation: SearchOperation,
                    val useUids: Boolean,
                    private val tag: Tag)
  extends AbstractImapRequest(tag, ImapConstants.SEARCH_COMMAND) {
}

case class SearchOperation(key: SearchKey, options: Seq[SearchResultOption])

sealed trait SearchKey

private[search] final case class All() extends SearchKey

private[search] final case class And(keys: Seq[SearchKey]) extends SearchKey

private[search] final case class Answered() extends SearchKey

private[search] final case class Bcc(value: String) extends SearchKey

private[search] final case class Before(date: DayMonthYear) extends SearchKey

private[search] final case class Body(value: String) extends SearchKey

private[search] final case class Cc(value: String) extends SearchKey

private[search] final case class Deleted() extends SearchKey

private[search] final case class Draft() extends SearchKey

private[search] final case class Header(name: String, value: String) extends SearchKey

private[search] final case class Flagged() extends SearchKey

private[search] final case class From(value: String) extends SearchKey

private[search] final case class Keyword(value: String) extends SearchKey

private[search] final case class Larger(size: Long) extends SearchKey

private[search] final case class New() extends SearchKey

private[search] final case class ModSeq(modSeq: Long) extends SearchKey

private[search] final case class Not(key: SearchKey) extends SearchKey

private[search] final case class Old() extends SearchKey

private[search] final case class Older(seconds: Long) extends SearchKey

private[search] final case class Or(keyOne: SearchKey, keyTwo: SearchKey) extends SearchKey

private[search] final case class On(date: DayMonthYear) extends SearchKey

private[search] final case class Recent() extends SearchKey

private[search] final case class Seen() extends SearchKey

private[search] final case class SentBefore(date: DayMonthYear) extends SearchKey

private[search] final case class SentOn(date: DayMonthYear) extends SearchKey

private[search] final case class SentSince(date: DayMonthYear) extends SearchKey

private[search] final case class SequenceNumbers(ids: Seq[IdRange]) extends SearchKey

private[search] final case class Since(date: DayMonthYear) extends SearchKey

private[search] final case class Smaller(size: Long) extends SearchKey

private[search] final case class Subject(value: String) extends SearchKey

private[search] final case class Text(value: String) extends SearchKey

private[search] final case class To(value: String) extends SearchKey

private[search] final case class Uid(ids: Iterable[UidRange]) extends SearchKey

private[search] final case class UnAnswered() extends SearchKey

private[search] final case class UnDeleted() extends SearchKey

private[search] final case class UnDraft() extends SearchKey

private[search] final case class UnFlagged() extends SearchKey

private[search] final case class UnKeyword(value: String) extends SearchKey

private[search] final case class UnSeen() extends SearchKey

private[search] final case class Younger(seconds: Long) extends SearchKey

