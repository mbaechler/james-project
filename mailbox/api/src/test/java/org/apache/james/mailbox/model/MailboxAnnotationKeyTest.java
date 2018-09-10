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

package org.apache.james.mailbox.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class MailboxAnnotationKeyTest {

    @Test
    public void newInstanceShouldThrowsExceptionWhenKeyDoesNotStartWithSlash() throws Exception {
        assertThatThrownBy(() -> new MailboxAnnotationKey("shared")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void newInstanceShouldThrowsExceptionWhenKeyContainsAsterisk() throws Exception {
        assertThatThrownBy(() -> new MailboxAnnotationKey("/private/key*comment")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void newInstanceShouldThrowsExceptionWhenKeyContainsPercent() throws Exception {
        assertThatThrownBy(() -> new MailboxAnnotationKey("/private/key%comment")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void validKeyShouldThrowsExceptionWhenKeyContainsTwoConsecutiveSlash() throws Exception {
        assertThatThrownBy(() -> new MailboxAnnotationKey("/private//keycomment")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void validKeyShouldThrowsExceptionWhenKeyEndsWithSlash() throws Exception {
        assertThatThrownBy(() -> new MailboxAnnotationKey("/private/keycomment/")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void validKeyShouldThrowsExceptionWhenKeyContainsNonASCII() throws Exception {
        assertThatThrownBy(() -> new MailboxAnnotationKey("/private/key┬ácomment")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void validKeyShouldThrowsExceptionWhenKeyContainsTabCharacter() throws Exception {
        assertThatThrownBy(() -> new MailboxAnnotationKey("/private/key\tcomment")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void newInstanceShouldThrowsExceptionWithEmptyKey() throws Exception {
        assertThatThrownBy(() -> new MailboxAnnotationKey("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void newInstanceShouldThrowsExceptionWithBlankKey() throws Exception {
        assertThatThrownBy(() -> new MailboxAnnotationKey("    ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void newInstanceShouldReturnRightKeyValue() throws Exception {
        MailboxAnnotationKey annotationKey = new MailboxAnnotationKey("/private/comment");
        assertThat(annotationKey.asString()).isEqualTo("/private/comment");
    }

    @Test
    public void keyValueShouldBeCaseInsensitive() throws Exception {
        MailboxAnnotationKey annotationKey = new MailboxAnnotationKey("/private/comment");
        MailboxAnnotationKey anotherAnnotationKey = new MailboxAnnotationKey("/PRIVATE/COMMENT");

        assertThat(annotationKey).isEqualTo(anotherAnnotationKey);
    }

    @Test
    public void newInstanceShouldThrowsExceptionWhenKeyContainsPunctuationCharacters() throws Exception {
        assertThatThrownBy(() -> new MailboxAnnotationKey("/private/+comment")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void countSlashShouldReturnRightNumberOfSlash() throws Exception {
        MailboxAnnotationKey annotationKey = new MailboxAnnotationKey("/private/comment/user/name");
        assertThat(annotationKey.countComponents()).isEqualTo(4);
    }

    @Test
    public void keyMustContainAtLeastTwoComponents() throws Exception {
        assertThatThrownBy(() -> new MailboxAnnotationKey("/private")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void keyVendorShouldThrowsExceptionWithTwoComponents() throws Exception {
        assertThatThrownBy(() -> new MailboxAnnotationKey("/private/vendor")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void keyVendorShouldThrowsExceptionWithThreeComponents() throws Exception {
        assertThatThrownBy(() -> new MailboxAnnotationKey("/shared/vendor/token")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void keyVendorShouldOKWithFourComponents() throws Exception {
        new MailboxAnnotationKey("/shared/vendor/token/comment");
    }

    @Test
    public void isParentOrIsEqualShouldReturnTrueWhenSameKey() {
        MailboxAnnotationKey key1 = new MailboxAnnotationKey("/private/comment");

        assertThat(key1.isParentOrIsEqual(key1)).isTrue();
    }

    @Test
    public void isParentOrIsEqualShouldReturnTrueWhenParent() {
        MailboxAnnotationKey key1 = new MailboxAnnotationKey("/private/comment");
        MailboxAnnotationKey key2 = new MailboxAnnotationKey("/private/comment/toto");

        assertThat(key1.isParentOrIsEqual(key2)).isTrue();
    }

    @Test
    public void isParentOrIsEqualShouldReturnFalseWhenChild() {
        MailboxAnnotationKey key1 = new MailboxAnnotationKey("/private/comment");
        MailboxAnnotationKey key2 = new MailboxAnnotationKey("/private/comment/toto");

        assertThat(key2.isParentOrIsEqual(key1)).isFalse();
    }

    @Test
    public void isParentOrIsEqualShouldReturnFalseWhenGrandParent() {
        MailboxAnnotationKey key1 = new MailboxAnnotationKey("/private/comment");
        MailboxAnnotationKey key2 = new MailboxAnnotationKey("/private/comment/toto/tata");

        assertThat(key1.isParentOrIsEqual(key2)).isFalse();
    }

    @Test
    public void isParentOrIsEqualShouldReturnFalseWhenCousin() {
        MailboxAnnotationKey key1 = new MailboxAnnotationKey("/private/comment/tutu");
        MailboxAnnotationKey key2 = new MailboxAnnotationKey("/private/comment/toto/tata");

        assertThat(key1.isParentOrIsEqual(key2)).isFalse();
    }


    @Test
    public void isAncestorOrIsEqualShouldReturnTrueWhenSameKey() {
        MailboxAnnotationKey key1 = new MailboxAnnotationKey("/private/comment");

        assertThat(key1.isAncestorOrIsEqual(key1)).isTrue();
    }

    @Test
    public void isAncestorOrIsEqualShouldReturnTrueWhenParent() {
        MailboxAnnotationKey key1 = new MailboxAnnotationKey("/private/comment");
        MailboxAnnotationKey key2 = new MailboxAnnotationKey("/private/comment/toto");

        assertThat(key1.isAncestorOrIsEqual(key2)).isTrue();
    }

    @Test
    public void isAncestorOrIsEqualShouldReturnFalseWhenChild() {
        MailboxAnnotationKey key1 = new MailboxAnnotationKey("/private/comment");
        MailboxAnnotationKey key2 = new MailboxAnnotationKey("/private/comment/toto");

        assertThat(key2.isAncestorOrIsEqual(key1)).isFalse();
    }

    @Test
    public void isAncestorOrIsEqualShouldReturnTrueWhenGrandParent() {
        MailboxAnnotationKey key1 = new MailboxAnnotationKey("/private/comment");
        MailboxAnnotationKey key2 = new MailboxAnnotationKey("/private/comment/toto/tata");

        assertThat(key1.isAncestorOrIsEqual(key2)).isTrue();
    }

    @Test
    public void isAncestorOrIsEqualShouldReturnFalseWhenCousin() {
        MailboxAnnotationKey key1 = new MailboxAnnotationKey("/private/comment/tutu");
        MailboxAnnotationKey key2 = new MailboxAnnotationKey("/private/comment/toto/tata");

        assertThat(key1.isAncestorOrIsEqual(key2)).isFalse();
    }

    @Test
    public void isAncestorOrIsEqualShouldWorkOnCousinKeyUsingKeyAsAPrefix() {
        MailboxAnnotationKey key1 = new MailboxAnnotationKey("/private/comment/tutu");
        MailboxAnnotationKey key2 = new MailboxAnnotationKey("/private/comment/tututata");

        assertThat(key1.isAncestorOrIsEqual(key2)).isFalse();
    }

    @Test
    public void isParentOrIsEqualShouldWorkOnCousinKeyUsingKeyAsAPrefix() {
        MailboxAnnotationKey key1 = new MailboxAnnotationKey("/private/comment/tutu");
        MailboxAnnotationKey key2 = new MailboxAnnotationKey("/private/comment/tututata");

        assertThat(key1.isParentOrIsEqual(key2)).isFalse();
    }
}