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

import java.io.IOException

import org.apache.james.imap.api.message.request.SearchResultOption
import org.apache.james.imap.encode.{ImapResponseComposer, ImapResponseEncoder}

import scala.jdk.javaapi.CollectionConverters

/**
 * Encoders IMAP4rev1 <code>ESEARCH</code> responses.
 */
class ESearchResponseEncoder extends ImapResponseEncoder[ESearchResponse] {
  override def acceptableMessages: Class[ESearchResponse] = classOf[ESearchResponse]

  @throws[IOException]
  override def encode(response: ESearchResponse, composer: ImapResponseComposer): Unit = {
    composer.untagged.message("ESEARCH").openParen.message("TAG").quote(response.tag.asString).closeParen
    if (response.useUid) composer.message("UID")
    if (response.minUid > -1 && response.options.contains(SearchResultOption.MIN))
      composer.message(SearchResultOption.MIN.name).message(response.minUid)
    if (response.maxUid > -1 && response.options.contains(SearchResultOption.MAX))
      composer.message(SearchResultOption.MAX.name).message(response.maxUid)
    if (response.options.contains(SearchResultOption.COUNT))
      composer.message(SearchResultOption.COUNT.name).message(response.count)
    if (!response.useUid && response.all != null && response.all.nonEmpty && response.options.contains(SearchResultOption.ALL)) {
      composer.message(SearchResultOption.ALL.name)
      composer.sequenceSet(response.all)
    }
    if (response.useUid && response.allUids != null && response.allUids.nonEmpty && response.options.contains(SearchResultOption.ALL)) {
      composer.message(SearchResultOption.ALL.name)
      composer.sequenceSet(response.allUids)
    }
    // Add the MODSEQ to the response if needed.
    //
    // see RFC4731 3.2.  Interaction with CONDSTORE extension
    if (response.highestModSeq != null) {
      composer.message("MODSEQ")
      composer.message(response.highestModSeq.asLong)
    }
    composer.end
  }
}