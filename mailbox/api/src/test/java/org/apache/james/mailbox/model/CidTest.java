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

import java.util.Optional;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class CidTest {

    @Test
    public void fromShouldThrowWhenNull() {
        assertThatThrownBy(() -> Cid.from(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fromShouldThrowWhenEmpty() {
        assertThatThrownBy(() -> Cid.from("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fromShouldThrowWhenBlank() {
        assertThatThrownBy(() -> Cid.from("    ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fromShouldThrowWhenEmptyAfterRemoveTags() {
        assertThatThrownBy(() -> Cid.from("<>")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fromShouldThrowWhenBlankAfterRemoveTags() {
        assertThatThrownBy(() -> Cid.from("<   >")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fromShouldRemoveTagsWhenExists() {
        Cid cid = Cid.from("<123>");
        assertThat(cid.getValue()).isEqualTo("123");
    }

    @Test
    public void fromShouldNotRemoveTagsWhenNone() {
        Cid cid = Cid.from("123");
        assertThat(cid.getValue()).isEqualTo("123");
    }

    @Test
    public void fromShouldNotRemoveTagsWhenNotEndTag() {
        Cid cid = Cid.from("<123");
        assertThat(cid.getValue()).isEqualTo("<123");
    }

    @Test
    public void fromShouldNotRemoveTagsWhenNotStartTag() {
        Cid cid = Cid.from("123>");
        assertThat(cid.getValue()).isEqualTo("123>");
    }

    @Test
    public void fromRelaxedNoUnwrapShouldReturnAbsentWhenNull() {
        assertThat(Cid.parser()
            .relaxed()
            .parse(null))
            .isEmpty();
    }

    @Test
    public void fromRelaxedNoUnwrapShouldReturnAbsentWhenEmpty() {
        assertThat(Cid.parser()
            .relaxed()
            .parse(""))
            .isEmpty();
    }

    @Test
    public void fromRelaxedNoUnwrapShouldReturnAbsentWhenBlank() {
        assertThat(Cid.parser()
            .relaxed()
            .parse("     "))
            .isEmpty();
    }

    @Test
    public void fromRelaxedNoUnwrapShouldReturnCidWhenEmptyAfterRemoveTags() {
        Optional<Cid> actual = Cid.parser()
            .relaxed()
            .parse("<>");
        assertThat(actual).isPresent();
        assertThat(actual.get().getValue()).isEqualTo("<>");
    }

    @Test
    public void fromRelaxedNoUnwrapShouldReturnCidWhenBlankAfterRemoveTags() {
        Optional<Cid> actual = Cid.parser()
            .relaxed()
            .parse("<   >");
        assertThat(actual).isPresent();
        assertThat(actual.get().getValue()).isEqualTo("<   >");
    }

    @Test
    public void fromRelaxedNoUnwrapShouldNotRemoveTagsWhenExists() {
        Optional<Cid> actual = Cid.parser()
            .relaxed()
            .parse("<123>");
        assertThat(actual).isPresent();
        assertThat(actual.get().getValue()).isEqualTo("<123>");
    }

    @Test
    public void fromRelaxedNoUnwrapShouldNotRemoveTagsWhenNone() {
        assertThat(Cid.parser()
            .relaxed()
            .parse("123"))
            .contains(Cid.from("123"));
    }

    @Test
    public void fromRelaxedNoUnwrapShouldNotRemoveTagsWhenNotEndTag() {
        assertThat(Cid.parser()
            .relaxed()
            .parse("<123"))
            .contains(Cid.from("<123"));
    }

    @Test
    public void fromRelaxedNoUnwrapShouldNotRemoveTagsWhenNotStartTag() {
        assertThat(Cid.parser()
            .relaxed()
            .parse("123>"))
            .contains(Cid.from("123>"));
    }


    @Test
    public void fromRelaxedUnwrapShouldReturnAbsentWhenNull() {
        assertThat(Cid.parser()
            .relaxed()
            .unwrap()
            .parse(null))
            .isEmpty();
    }

    @Test
    public void fromRelaxedUnwrapShouldReturnAbsentWhenEmpty() {
        assertThat(Cid.parser()
            .relaxed()
            .unwrap()
            .parse(""))
            .isEmpty();
    }

    @Test
    public void fromRelaxedUnwrapShouldReturnAbsentWhenBlank() {
        assertThat(Cid.parser()
            .relaxed()
            .unwrap()
            .parse("     "))
            .isEmpty();
    }

    @Test
    public void fromRelaxedUnwrapShouldReturnAbsentWhenEmptyAfterRemoveTags() {
        assertThat(Cid.parser()
            .relaxed()
            .unwrap()
            .parse("<>"))
            .isEmpty();
    }

    @Test
    public void fromRelaxedUnwrapShouldReturnAbsentWhenBlankAfterRemoveTags() {
        assertThat(Cid.parser()
            .relaxed()
            .unwrap()
            .parse("<   >"))
            .isEmpty();
    }

    @Test
    public void fromRelaxedUnwrapShouldRemoveTagsWhenExists() {
        Optional<Cid> actual = Cid.parser()
            .relaxed()
            .unwrap()
            .parse("<123>");
        assertThat(actual).isPresent();
        assertThat(actual.get().getValue()).isEqualTo("123");
    }

    @Test
    public void fromRelaxedUnwrapShouldNotRemoveTagsWhenNone() {
        assertThat(Cid.parser()
            .relaxed()
            .unwrap()
            .parse("123"))
            .contains(Cid.from("123"));
    }

    @Test
    public void fromRelaxedUnwrapShouldNotRemoveTagsWhenNotEndTag() {
        assertThat(Cid.parser()
            .relaxed()
            .unwrap()
            .parse("<123"))
            .contains(Cid.from("<123"));
    }

    @Test
    public void fromRelaxedUnwrapShouldNotRemoveTagsWhenNotStartTag() {
        assertThat(Cid.parser()
            .relaxed()
            .unwrap()
            .parse("123>"))
            .contains(Cid.from("123>"));
    }

    @Test
    public void fromStrictNoUnwrapShouldThrowWhenNull() {
        assertThatThrownBy(() -> Cid.parser()
                .strict()
                .parse(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fromStrictNoUnwrapShouldThrowWhenEmpty() {
        assertThatThrownBy(() -> Cid.parser()
                .strict()
                .parse(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fromStrinctNoUnwrapShouldThrowWhenBlank() {
        assertThatThrownBy(() -> Cid.parser()
                .strict()
                .parse("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fromStrictNoUnwrapShouldNotRemoveTagWhenEmptyAfterRemoveTags() {
        Optional<Cid> actual = Cid.parser()
            .strict()
            .parse("<>");
        assertThat(actual).isPresent();
        assertThat(actual.get().getValue()).isEqualTo("<>");
    }

    @Test
    public void fromStrictNoUnwrapShouldNotRemoveTagWhenBlankAfterRemoveTags() {
        Optional<Cid> actual = Cid.parser()
            .strict()
            .parse("<   >");
        assertThat(actual).isPresent();
        assertThat(actual.get().getValue()).isEqualTo("<   >");
    }

    @Test
    public void fromStrictNoUnwrapShouldNotRemoveTagsWhenExists() {
        Optional<Cid> actual = Cid.parser()
            .strict()
            .parse("<123>");
        assertThat(actual).isPresent();
        assertThat(actual.get().getValue()).isEqualTo("<123>");
    }

    @Test
    public void fromStrictNoUnwrapShouldNotRemoveTagsWhenNone() {
        assertThat(Cid.parser()
            .strict()
            .parse("123"))
            .contains(Cid.from("123"));
    }

    @Test
    public void fromStrictNoUnwrapShouldNotRemoveTagsWhenNotEndTag() {
        assertThat(Cid.parser()
            .strict()
            .parse("<123"))
            .contains(Cid.from("<123"));
    }

    @Test
    public void fromStrictNoUnwrapShouldNotRemoveTagsWhenNotStartTag() {
        assertThat(Cid.parser()
            .strict()
            .parse("123>"))
            .contains(Cid.from("123>"));
    }

    @Test
    public void shouldRespectJavaBeanContract() {
        EqualsVerifier.forClass(Cid.class).verify();
    }
}
