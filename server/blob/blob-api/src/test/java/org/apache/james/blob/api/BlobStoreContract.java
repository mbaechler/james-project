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

package org.apache.james.blob.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.common.base.Strings;

public interface BlobStoreContract {

    byte[] EMPTY_BYTEARRAY = {};
    byte[] SHORT_BYTEARRAY = "toto".getBytes(StandardCharsets.UTF_8);
    byte[] TEN_KILOBYTES = Strings.repeat("0123456789\n", 1000).getBytes(StandardCharsets.UTF_8);
    byte[] TWELVE_MEGABYTES = Strings.repeat("0123456789\r\n", 1024 * 1024).getBytes(StandardCharsets.UTF_8);

    BlobStore testee();

    BlobId.Factory blobIdFactory();

    @Test
    default void saveShouldThrowWhenNullData() {
        assertThatThrownBy(() -> testee().save((byte[]) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    default void saveShouldThrowWhenNullInputStream() {
        assertThatThrownBy(() -> testee().save((InputStream) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    default void saveShouldSaveEmptyData() {
        BlobId blobId = testee().save(EMPTY_BYTEARRAY).join();

        byte[] bytes = testee().readBytes(blobId).join();

        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    default void saveShouldSaveEmptyInputStream() {
        BlobId blobId = testee().save(new ByteArrayInputStream(EMPTY_BYTEARRAY)).join();

        byte[] bytes = testee().readBytes(blobId).join();

        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    default void saveShouldReturnBlobId() {
        BlobId blobId = testee().save(SHORT_BYTEARRAY).join();

        assertThat(blobId).isEqualTo(blobIdFactory().from("31f7a65e315586ac198bd798b6629ce4903d0899476d5741a9f32e2e521b6a66"));
    }

    @Test
    default void saveShouldReturnBlobIdOfInputStream() {
        BlobId blobId =
            testee().save(new ByteArrayInputStream(SHORT_BYTEARRAY)).join();

        assertThat(blobId).isEqualTo(blobIdFactory().from("31f7a65e315586ac198bd798b6629ce4903d0899476d5741a9f32e2e521b6a66"));
    }

    @Test
    default void readBytesShouldThrowWhenNoExisting() {
        assertThatThrownBy(() -> testee().readBytes(blobIdFactory().from("unknown")).join())
            .hasCauseInstanceOf(ObjectStoreException.class);
    }

    @Test
    default void readBytesShouldReturnSavedData() {
        BlobId blobId = testee().save(SHORT_BYTEARRAY).join();

        byte[] bytes = testee().readBytes(blobId).join();

        assertThat(bytes).isEqualTo(SHORT_BYTEARRAY);
    }

    @Test
    default void readBytesShouldReturnLongSavedData() {
        BlobId blobId = testee().save(TEN_KILOBYTES).join();

        byte[] bytes = testee().readBytes(blobId).join();

        assertThat(bytes).isEqualTo(TEN_KILOBYTES);
    }

    @Test
    default void readBytesShouldReturnBigSavedData() {
        BlobId blobId = testee().save(TWELVE_MEGABYTES).join();

        byte[] bytes = testee().readBytes(blobId).join();

        assertThat(bytes).isEqualTo(TWELVE_MEGABYTES);
    }

    @Test
    default void readShouldThrowWhenNoExistingStream() {
        assertThatThrownBy(() -> testee().read(blobIdFactory().from("unknown")))
            .isInstanceOf(ObjectStoreException.class);
    }

    @Test
    default void readShouldReturnSavedData() {
        BlobId blobId = testee().save(SHORT_BYTEARRAY).join();

        InputStream read = testee().read(blobId);

        assertThat(read).hasSameContentAs(new ByteArrayInputStream(SHORT_BYTEARRAY));
    }

    @Test
    default void readShouldReturnLongSavedData() {
        BlobId blobId = testee().save(TEN_KILOBYTES).join();

        InputStream read = testee().read(blobId);

        assertThat(read).hasSameContentAs(new ByteArrayInputStream(TEN_KILOBYTES));
    }

    @Test
    default void readShouldReturnBigSavedData() {
        // 12 MB of text
        BlobId blobId = testee().save(TWELVE_MEGABYTES).join();

        InputStream read = testee().read(blobId);

        assertThat(read).hasSameContentAs(new ByteArrayInputStream(TWELVE_MEGABYTES));
    }
}
