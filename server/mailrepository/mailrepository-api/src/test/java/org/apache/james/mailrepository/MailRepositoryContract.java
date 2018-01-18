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

package org.apache.james.mailrepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.james.junit.ExecutorExtension;
import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.server.core.MailImpl;
import org.apache.james.utils.Partition;
import org.apache.mailet.Mail;
import org.apache.mailet.PerRecipientHeaders;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.hash.Hashing;
import io.vavr.CheckedRunnable;
import io.vavr.collection.Seq;
import io.vavr.concurrent.Future;

@ExtendWith(ExecutorExtension.class)
public interface MailRepositoryContract {

    String TEST_ATTRIBUTE = "testAttribute";

    default MailImpl createMail(String name) throws MessagingException {
        return createMail(name, "original body");
    }

    default MailImpl createMail(String name, String body) throws MessagingException {
        MimeMessage mailContent = generateMailContent(body);
        List<MailAddress> recipients = ImmutableList
            .of(new MailAddress("rec1@domain.com"),
                new MailAddress("rec2@domain.com"));
        MailAddress sender = new MailAddress("sender@domain.com");
        MailImpl mail = new MailImpl(name, sender, recipients, mailContent);
        mail.setAttribute(TEST_ATTRIBUTE, "testValue");
        return mail;
    }


    default MimeMessage generateMailContent(String body) throws MessagingException {
        return MimeMessageBuilder.mimeMessageBuilder()
            .setSubject("test")
            .setText(body)
            .build();
    }

    default void checkMailEquality(Mail actual, Mail expected) {
        assertAll(
            () -> assertThat(actual.getMessage().getContent()).isEqualTo(expected.getMessage().getContent()),
            () -> assertThat(actual.getMessageSize()).isEqualTo(expected.getMessageSize()),
            () -> assertThat(actual.getName()).isEqualTo(expected.getName()),
            () -> assertThat(actual.getState()).isEqualTo(expected.getState()),
            () -> assertThat(actual.getAttribute(TEST_ATTRIBUTE)).isEqualTo(expected.getAttribute(TEST_ATTRIBUTE)),
            () -> assertThat(actual.getErrorMessage()).isEqualTo(expected.getErrorMessage()),
            () -> assertThat(actual.getRemoteHost()).isEqualTo(expected.getRemoteHost()),
            () -> assertThat(actual.getRemoteAddr()).isEqualTo(expected.getRemoteAddr()),
            () -> assertThat(actual.getLastUpdated()).isEqualTo(expected.getLastUpdated()),
            () -> assertThat(actual.getPerRecipientSpecificHeaders()).isEqualTo(expected.getPerRecipientSpecificHeaders())
        );
    }

    MailRepository retrieveRepository() throws Exception;

    @Test
    default void storeRegularMailShouldNotFail() throws Exception {
        MailRepository testee = retrieveRepository();
        Mail mail = createMail("mail1");

        testee.store(mail);
    }

    @Test
    default void storeBigMailShouldNotFail() throws Exception {
        MailRepository testee = retrieveRepository();
        String bigString = Strings.repeat("my mail is big 🐋", 1_000_000);
        Mail mail = createMail("mail1", bigString);

        testee.store(mail);
    }


    @Test
    default void retrieveShouldGetStoredMail() throws Exception {
        MailRepository testee = retrieveRepository();
        String key1 = "mail1";
        Mail mail = createMail(key1);

        testee.store(mail);

        assertThat(testee.retrieve(key1)).satisfies(actual -> checkMailEquality(actual, mail));
    }

    @Test
    default void retrieveShouldGetStoredEmojiMail() throws Exception {
        MailRepository testee = retrieveRepository();
        String key1 = "mail1";
        Mail mail = createMail(key1, "my content contains 🐋");

        testee.store(mail);

        assertThat(testee.retrieve(key1).getMessage().getContent()).isEqualTo("my content contains 🐋");
    }

    @Test
    default void retrieveBigMailShouldHaveSameHash() throws Exception {
        MailRepository testee = retrieveRepository();
        String bigString = Strings.repeat("my mail is big 🐋", 1_000_000);
        Mail mail = createMail("mail1", bigString);
        testee.store(mail);

        Mail actual = testee.retrieve("mail1");

        assertThat(Hashing.sha256().hashString((String)actual.getMessage().getContent(), StandardCharsets.UTF_8))
            .isEqualTo(Hashing.sha256().hashString(bigString, StandardCharsets.UTF_8));
    }


    @Test
    default void retrieveShouldReturnAllMailProperties() throws Exception {
        MailRepository testee = retrieveRepository();
        String key1 = "mail1";
        MailImpl mail = createMail(key1);
        mail.setErrorMessage("Error message");
        mail.setRemoteAddr("172.5.2.3");
        mail.setRemoteHost("smtp@domain.com");
        mail.setLastUpdated(new Date());
        mail.addSpecificHeaderForRecipient(PerRecipientHeaders.Header.builder()
            .name("name")
            .value("value")
            .build(),
            new MailAddress("bob@domain.com"));

        testee.store(mail);

        assertThat(testee.retrieve(key1)).satisfies(actual -> checkMailEquality(actual, mail));
    }

    @Test
    default void newlyCreatedRepositoryShouldNotContainAnyMail() throws Exception {
        MailRepository testee = retrieveRepository();

        assertThat(testee.list()).isEmpty();
    }

    @Test
    default void retrievingUnknownMailShouldReturnNull() throws Exception {
        MailRepository testee = retrieveRepository();

        assertThat(testee.retrieve("random")).isNull();
    }

    @Test
    default void removingUnknownMailShouldHaveNoEffect() throws Exception {
        MailRepository testee = retrieveRepository();

        testee.remove("random");
    }

    @Test
    default void retrieveShouldReturnNullWhenKeyWasRemoved() throws Exception {
        MailRepository testee = retrieveRepository();
        String key = "mail1";
        testee.store(createMail(key));

        testee.remove(key);

        assertThat(retrieveRepository().list()).doesNotContain(key);
        assertThat(retrieveRepository().retrieve(key)).isNull();
    }

    @Test
    default void removeShouldnotAffectUnrelatedMails() throws Exception {
        MailRepository testee = retrieveRepository();
        String key1 = "mail1";
        testee.store(createMail(key1));
        String key2 = "mail2";
        testee.store(createMail(key2));

        testee.remove(key1);

        assertThat(retrieveRepository().list()).contains(key2);
    }

    @Test
    default void removedMailsShouldNotBeListed() throws Exception {
        MailRepository testee = retrieveRepository();

        String key1 = "mail1";
        String key2 = "mail2";
        String key3 = "mail3";
        Mail mail1 = createMail(key1);
        Mail mail2 = createMail(key2);
        Mail mail3 = createMail(key3);
        retrieveRepository().store(mail1);
        retrieveRepository().store(mail2);
        retrieveRepository().store(mail3);

        testee.remove(ImmutableList.of(mail1, mail3));

        assertThat(retrieveRepository().list())
            .contains(key2)
            .doesNotContain(key1, key3);
    }

    @Test
    default void removedMailShouldNotBeListed() throws Exception {
        MailRepository testee = retrieveRepository();

        String key1 = "mail1";
        String key2 = "mail2";
        String key3 = "mail3";
        Mail mail1 = createMail(key1);
        Mail mail2 = createMail(key2);
        Mail mail3 = createMail(key3);
        retrieveRepository().store(mail1);
        retrieveRepository().store(mail2);
        retrieveRepository().store(mail3);

        testee.remove(mail2);

        assertThat(retrieveRepository().list())
            .contains(key1, key3)
            .doesNotContain(key2);
    }

    @Test
    default void removeShouldHaveNoEffectForUnknownMails() throws Exception {
        MailRepository testee = retrieveRepository();

        testee.remove(ImmutableList.of(createMail("unknown")));

        assertThat(retrieveRepository().list()).isEmpty();
    }

    @Test
    default void removeShouldHaveNoEffectForUnknownMail() throws Exception {
        MailRepository testee = retrieveRepository();

        testee.remove(createMail("unknown"));

        assertThat(retrieveRepository().list()).isEmpty();
    }

    @Test
    default void listShouldReturnStoredMailsKeys() throws Exception {
        MailRepository testee = retrieveRepository();
        String key1 = "mail1";
        String key2 = "mail2";
        testee.store(createMail(key1));

        testee.store(createMail(key2));

        assertThat(testee.list()).containsOnly(key1, key2);
    }

    @Test
    default void storingMessageWithSameKeyTwiceShouldUpdateMessageContent() throws Exception {
        MailRepository testee = retrieveRepository();
        String key = "mail1";
        testee.store(createMail(key));

        Mail updatedMail = createMail(key, "modified content");
        testee.store(updatedMail);

        assertThat(testee.list()).hasSize(1);
        assertThat(testee.retrieve(key)).satisfies(actual -> checkMailEquality(actual, updatedMail));
    }

    @Test
    default void storingMessageWithSameKeyTwiceShouldUpdateMessageAttributes() throws Exception {
        MailRepository testee = retrieveRepository();
        String key = "mail1";
        Mail mail = createMail(key);
        testee.store(mail);

        mail.setAttribute(TEST_ATTRIBUTE, "newValue");
        testee.store(mail);

        assertThat(testee.list()).hasSize(1);
        assertThat(testee.retrieve(key)).satisfies(actual -> checkMailEquality(actual, mail));
    }


    @Test
    default void storingAndRemovingMessagesConcurrentlyShouldLeadToConsistentResult(ExecutorService executorService) throws Exception {
        MailRepository testee = retrieveRepository();

        ConcurrentHashMap.KeySetView<String, Boolean> expectedResult = ConcurrentHashMap.newKeySet();

        int nbKeys = 200;
        Function<Integer, CheckedRunnable> add = (Integer i) -> () -> {
            String key = computeKey(nbKeys, i);
            testee.store(createMail(key));
            expectedResult.add(key);
        };

        Function<Integer, CheckedRunnable> remove = (Integer i) -> () -> {
            String key = computeKey(nbKeys, i);
            testee.remove(key);
            expectedResult.remove(key);
        };

        int nbIterations = 1000;
        Future.sequence(
            Partition
                .create(ImmutableMultimap.of(2, add, 6, remove))
                .generateRandomStream()
                .zipWithIndex()
                .map(x -> x._1.apply(x._2))
                .take(nbIterations)
                .map(runnable -> io.vavr.concurrent.Future.run(executorService, runnable)))
            .await();

        assertThat(testee.list()).containsOnlyElementsOf(expectedResult);
    }

    @NotNull
    default String computeKey(int nbKeys, Integer i) {
        int keyIndex = computeKeyIndex(nbKeys, i);
        return "mail" + keyIndex;
    }

    default int computeKeyIndex(int nbKeys, Integer i) {
        return i % nbKeys;
    }

}
