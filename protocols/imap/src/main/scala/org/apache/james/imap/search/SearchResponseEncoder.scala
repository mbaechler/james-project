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

import org.apache.james.imap.api.ImapConstants
import org.apache.james.imap.encode.{ImapResponseComposer, ImapResponseEncoder}

/**
 * Encoders IMAP4rev1 <code>SEARCH</code> responses.
 */
class SearchResponseEncoder extends ImapResponseEncoder[SearchResponse] {
  override def acceptableMessages: Class[SearchResponse] = classOf[SearchResponse]

  @throws[IOException]
  override def encode(response: SearchResponse, composer: ImapResponseComposer): Unit = {
    composer.untagged
    composer.commandName(ImapConstants.SEARCH_COMMAND)

    if (response.ids != null) response.ids.foreach(composer.message)

    response.highestModSeq.foreach(modseq => composer.openParen.message("MODSEQ").message(modseq.asLong).closeParen)
    
    composer.end
  }
}