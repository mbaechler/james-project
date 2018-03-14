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


package org.apache.james.transport.matchers;

import java.util.Collection;

import javax.mail.MessagingException;

import org.apache.james.core.MailAddress;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMatcher;

public class SMTPAuthUserIsTest extends AbstractHasMailAttributeTest {

    @Override
    protected String getHasMailAttribute() {
        return "test@james.apache.org";
    }

    @Override
    protected GenericMatcher createMatcher() {
        return new SMTPAuthUserIs();
    }

    @Override
    protected String getMatcherName() {
        return "SMTPAuthUserIs";
    }
    
    @Override
    protected void init() {
        super.init();
        setMailAttributeName(Mail.SMTP_AUTH_USER_ATTRIBUTE_NAME);
        setMailAttributeValue("test@james.apache.org");
    }
    
    
    // test if the mail attribute was not matched
    @Override
    public void testAttributeIsNotMatched() throws MessagingException {
        setupAll();
        setMailAttributeValue("notmatched@james.apache.org");

        Collection<MailAddress> matchedRecipients = matcher.match(mockedMail);

        assertNull(matchedRecipients);
    }

}
