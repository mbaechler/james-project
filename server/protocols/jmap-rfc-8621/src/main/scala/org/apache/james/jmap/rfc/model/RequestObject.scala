/** **************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                 *
 * *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 * ***************************************************************/

package org.apache.james.jmap.rfc.model

import play.api.libs.json.JsArray

object RequestObjectPOJO {

  case class Capability(value: String) extends AnyVal
  case class ClientId(value: String) extends AnyVal
  case class ServerId(value: String) extends AnyVal
  case class Invocation(value: JsArray) extends AnyVal
  case class RequestObject(using: Seq[Capability], methodCalls: Seq[Invocation])

  import play.api.libs.json._

  implicit val capabilityFormat = Json.valueFormat[Capability]
  implicit val clientIdFormat = Json.valueFormat[ClientId]
  implicit val serverIdFormat = Json.valueFormat[ServerId]
  implicit val methodCallFormat = Json.valueFormat[Invocation]
  implicit val requestObjectFormat = Json.format[RequestObject]

  def serialize(input: String): RequestObject = {
    Json.parse(input).as[RequestObject]
  }
}


