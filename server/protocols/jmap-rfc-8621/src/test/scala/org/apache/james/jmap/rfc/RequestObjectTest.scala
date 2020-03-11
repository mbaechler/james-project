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

package org.apache.james.jmap.rfc

import org.apache.james.jmap.rfc.model.RequestObjectPOJO
import org.apache.james.jmap.rfc.model.RequestObjectPOJO.{Capability, Invocation}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsArray, JsObject, JsResult, JsString, JsSuccess, JsValue, Json}

class RequestObjectTest extends PlaySpec {

  "Serialize Capability" must {
    "succeed " in {
      val capabilityJsValue: JsValue = JsString("org.com.test.1")
      Json.fromJson[Capability](capabilityJsValue) must be(JsSuccess(Capability("org.com.test.1")))
    }

    "failed" in {
      val capabilityJsValue: JsValue = JsString("org.com.test.2")
      Json.fromJson[Capability](capabilityJsValue) must not be(JsSuccess(Capability("org.com.test.1")))
    }
  }

  "Serialize RequestObject" must {
    "succeed " in {
      RequestObjectPOJO.serialize(
        """
          |{
          |  "using": [ "urn:ietf:params:jmap:core"],
          |  "methodCalls": [
          |    [ "Core/echo", {
          |      "arg1": "arg1data",
          |      "arg2": "arg2data"
          |    }, "c1" ]
          |  ]
          |}
          |""".stripMargin) must be(
          RequestObjectPOJO.RequestObject(
            using = Seq(RequestObjectPOJO.Capability("urn:ietf:params:jmap:core")),
            methodCalls = Seq(Invocation(Json.parse(
              """[ "Core/echo", {
                |      "arg1": "arg1data",
                |      "arg2": "arg2data"
                |    }, "c1" ]
                |""".stripMargin).as[JsArray]))))
    }
  }
}
