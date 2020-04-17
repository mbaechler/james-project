package org.apache.james.imap.search

import org.apache.james.imap.api.message.request.SearchResultOption

case class SearchOperation(key: SearchKey, options: Seq[SearchResultOption])
