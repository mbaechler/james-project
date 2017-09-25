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

package org.apache.james.jmap.model.mailbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.james.mailbox.model.SimpleMailboxACL;
import org.apache.james.mailbox.model.SimpleMailboxACL.Rfc4314Rights;
import org.apache.james.mailbox.model.SimpleMailboxACL.SimpleMailboxACLEntryKey;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;

public class RightsTest {

    public static final boolean NEGATIVE = true;

    @Test
    public void rightsShouldMatchBeanContract() {
        EqualsVerifier.forClass(Rights.class)
            .allFieldsShouldBeUsed()
            .verify();
    }

    @Test
    public void usernameShouldMatchBeanContract() {
        EqualsVerifier.forClass(Rights.Username.class)
            .allFieldsShouldBeUsed()
            .verify();
    }

    @Test
    public void forCharShouldReturnRightWhenA() {
        assertThat(Rights.Right.forChar('a'))
            .isEqualTo(Rights.Right.Administer);
    }

    @Test
    public void forCharShouldReturnRightWhenE() {
        assertThat(Rights.Right.forChar('e'))
            .isEqualTo(Rights.Right.Expunge);
    }

    @Test
    public void forCharShouldReturnRightWhenI() {
        assertThat(Rights.Right.forChar('i'))
            .isEqualTo(Rights.Right.Insert);
    }

    @Test
    public void forCharShouldReturnRightWhenL() {
        assertThat(Rights.Right.forChar('l'))
            .isEqualTo(Rights.Right.Lookup);
    }

    @Test
    public void forCharShouldReturnRightWhenR() {
        assertThat(Rights.Right.forChar('r'))
            .isEqualTo(Rights.Right.Read);
    }

    @Test
    public void forCharShouldReturnRightWhenW() {
        assertThat(Rights.Right.forChar('w'))
            .isEqualTo(Rights.Right.Write);
    }

    @Test
    public void forCharShouldReturnRightWhenT() {
        assertThat(Rights.Right.forChar('t'))
            .isEqualTo(Rights.Right.T_Delete);
    }

    @Test
    public void forCharShouldThrowOnUnsupportedRight() {
        assertThatThrownBy(() -> Rights.Right.forChar('k'))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void existsShouldReturnTrueOnManagedRights() {
        assertThat(Rights.Right.exists('t'))
            .isTrue();
    }

    @Test
    public void existsShouldReturnFalseOnUnManagedRights() {
        assertThat(Rights.Right.exists('k'))
            .isFalse();
    }

    @Test
    public void fromACLShouldFilterOutGroups() throws Exception {
        SimpleMailboxACL acl = new SimpleMailboxACL(ImmutableMap.of(
            SimpleMailboxACLEntryKey.createGroup("group"), new Rfc4314Rights("aet")));

        assertThat(Rights.fromACL(acl))
            .isEqualTo(Rights.EMPTY);
    }

    @Test
    public void fromACLShouldFilterNegatedUsers() throws Exception {
        SimpleMailboxACL acl = new SimpleMailboxACL(ImmutableMap.of(
            SimpleMailboxACLEntryKey.createUser("user", NEGATIVE), new Rfc4314Rights("aet")));

        assertThat(Rights.fromACL(acl))
            .isEqualTo(Rights.EMPTY);
    }

    @Test
    public void fromACLShouldAcceptUsers() throws Exception {
        SimpleMailboxACL acl = new SimpleMailboxACL(ImmutableMap.of(
            SimpleMailboxACLEntryKey.createUser("user"), new Rfc4314Rights("aet")));

        assertThat(Rights.fromACL(acl))
            .isEqualTo(Rights.builder()
                .delegateTo(new Rights.Username("user"), Rights.Right.Administer, Rights.Right.Expunge, Rights.Right.T_Delete)
                .build());
    }

    @Test
    public void fromACLShouldFilterOutUnknownRights() throws Exception {
        SimpleMailboxACL acl = new SimpleMailboxACL(ImmutableMap.of(
            SimpleMailboxACLEntryKey.createUser("user"), new Rfc4314Rights("aetpk")));

        assertThat(Rights.fromACL(acl))
            .isEqualTo(Rights.builder()
                .delegateTo(new Rights.Username("user"), Rights.Right.Administer, Rights.Right.Expunge, Rights.Right.T_Delete)
                .build());
    }

    @Test
    public void toMailboxAclShouldReturnEmptyAclWhenEmpty() {
        Rights rights = Rights.EMPTY;

        assertThat(rights.toMailboxAcl())
            .isEqualTo(new SimpleMailboxACL());
    }

    @Test
    public void toMailboxActShouldReturnActConversion() throws Exception {
        String user1 = "user1";
        String user2 = "user2";
        Rights rights = Rights.builder()
            .delegateTo(new Rights.Username(user1), Rights.Right.Administer, Rights.Right.T_Delete)
            .delegateTo(new Rights.Username(user2), Rights.Right.Expunge, Rights.Right.Lookup)
            .build();

        assertThat(rights.toMailboxAcl())
            .isEqualTo(new SimpleMailboxACL(ImmutableMap.of(
                SimpleMailboxACLEntryKey.createUser(user1), new Rfc4314Rights("at"),
                SimpleMailboxACLEntryKey.createUser(user2), new Rfc4314Rights("el"))));
    }

}