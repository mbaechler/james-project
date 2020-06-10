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
package org.apache.james.queue.reactiveActivemq

import org.apache.james.queue.reactiveActivemq.EnqueueId.EnqueueId
import org.junit.jupiter.api.Assertions._
import org.junit.jupiter.api.Test
import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json}

class MailMetadataTest {

  implicit val mailMetadataFormat: Format[MailMetadata] = Json.format[MailMetadata]
  implicit val headerFormat: Format[Header] = Json.format[Header]
  implicit val enqueueIdFormat: Format[EnqueueId] = new Format[EnqueueId] {
    override def writes(o: EnqueueId): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[EnqueueId] =
      json.validate[String].map(EnqueueId.apply).flatMap(_.fold(JsError.apply, JsSuccess(_)))
  }


  @Test def serializeDeserialize() = {
    Json.fromJson[MailMetadata](Json.toJson(MailMetadata("foo", Seq("bar"), "name", None, "state", "errorMessage", None, Map.empty, "remoteAddr", "remoteHOst", Map.empty, "headerblobid", "bodyblobid"))).get
  }
}