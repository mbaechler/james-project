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
package org.apache.james.mailbox.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class BatchSizesTest {

    @Test
    public void shouldRespectJavaBeanContract() {
        EqualsVerifier.forClass(BatchSizes.class).verify();
    }

    @Test
    public void defaultValuesShouldReturnDefaultForEachParameters() {
        BatchSizes batchSizes = BatchSizes.defaultValues();
        assertThat(batchSizes.getFetchMetadata()).isEqualTo(BatchSizes.DEFAULT_BATCH_SIZE);
        assertThat(batchSizes.getFetchHeaders()).isEqualTo(BatchSizes.DEFAULT_BATCH_SIZE);
        assertThat(batchSizes.getFetchBody()).isEqualTo(BatchSizes.DEFAULT_BATCH_SIZE);
        assertThat(batchSizes.getFetchFull()).isEqualTo(BatchSizes.DEFAULT_BATCH_SIZE);
        assertThat(batchSizes.getCopyBatchSize()).isEqualTo(BatchSizes.DEFAULT_BATCH_SIZE);
        assertThat(batchSizes.getMoveBatchSize()).isEqualTo(BatchSizes.DEFAULT_BATCH_SIZE);
    }

    @Test
    public void uniqueBatchSizeShouldSetTheSameValueToAllAttributes() {
        int batchSize = 10;
        BatchSizes batchSizes = BatchSizes.uniqueBatchSize(batchSize);
        assertThat(batchSizes.getFetchMetadata()).isEqualTo(batchSize);
        assertThat(batchSizes.getFetchHeaders()).isEqualTo(batchSize);
        assertThat(batchSizes.getFetchBody()).isEqualTo(batchSize);
        assertThat(batchSizes.getFetchFull()).isEqualTo(batchSize);
        assertThat(batchSizes.getCopyBatchSize()).isEqualTo(batchSize);
        assertThat(batchSizes.getMoveBatchSize()).isEqualTo(batchSize);
    }

    @Test
    public void fetchMetadataShouldThrowWhenNegative() {
        assertThatThrownBy(() -> BatchSizes.builder()
                .fetchMetadata(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fetchMetadataShouldThrowWhenZero() {
        assertThatThrownBy(() -> BatchSizes.builder()
                .fetchMetadata(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fetchHeadersShouldThrowWhenNegative() {
        assertThatThrownBy(() -> BatchSizes.builder()
                .fetchHeaders(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fetchHeadersShouldThrowWhenZero() {
        assertThatThrownBy(() -> BatchSizes.builder()
                .fetchHeaders(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fetchBodyShouldThrowWhenNegative() {
        assertThatThrownBy(() -> BatchSizes.builder()
                .fetchBody(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fetchBodyShouldThrowWhenZero() {
        assertThatThrownBy(() -> BatchSizes.builder()
                .fetchBody(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fetchFullShouldThrowWhenNegative() {
        assertThatThrownBy(() -> BatchSizes.builder()
                .fetchFull(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fetchFullShouldThrowWhenZero() {
        assertThatThrownBy(() -> BatchSizes.builder()
                .fetchFull(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void copyBatchSizeShouldThrowWhenNegative() {
        assertThatThrownBy(() -> BatchSizes.builder()
                .copyBatchSize(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void copyBatchSizeShouldThrowWhenZero() {
        assertThatThrownBy(() -> BatchSizes.builder()
                .copyBatchSize(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void moveBatchSizeShouldThrowWhenNegative() {
        assertThatThrownBy(() -> BatchSizes.builder()
                .moveBatchSize(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void moveBatchSizeShouldThrowWhenZero() {
        assertThatThrownBy(() -> BatchSizes.builder()
                .moveBatchSize(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void buildShouldSetDefaultValueToFetchMetadataWhenNotGiven() {
        BatchSizes batchSizes = BatchSizes.builder()
                .build();
        assertThat(batchSizes.getFetchMetadata()).isEqualTo(BatchSizes.DEFAULT_BATCH_SIZE);
    }

    @Test
    public void buildShouldSetValueToFetchMetadataWhenGiven() {
        int expected = 123;
        BatchSizes batchSizes = BatchSizes.builder()
                .fetchMetadata(expected)
                .build();
        assertThat(batchSizes.getFetchMetadata()).isEqualTo(expected);
    }

    @Test
    public void buildShouldSetDefaultValueToFetchHeadersWhenNotGiven() {
        BatchSizes batchSizes = BatchSizes.builder()
                .build();
        assertThat(batchSizes.getFetchHeaders()).isEqualTo(BatchSizes.DEFAULT_BATCH_SIZE);
    }

    @Test
    public void buildShouldSetValueToFetchHeadersWhenGiven() {
        int expected = 123;
        BatchSizes batchSizes = BatchSizes.builder()
                .fetchHeaders(expected)
                .build();
        assertThat(batchSizes.getFetchHeaders()).isEqualTo(expected);
    }

    @Test
    public void buildShouldSetDefaultValueToFetchBodyWhenNotGiven() {
        BatchSizes batchSizes = BatchSizes.builder()
                .build();
        assertThat(batchSizes.getFetchBody()).isEqualTo(BatchSizes.DEFAULT_BATCH_SIZE);
    }

    @Test
    public void buildShouldSetValueToFetchBodyWhenGiven() {
        int expected = 123;
        BatchSizes batchSizes = BatchSizes.builder()
                .fetchBody(expected)
                .build();
        assertThat(batchSizes.getFetchBody()).isEqualTo(expected);
    }

    @Test
    public void buildShouldSetDefaultValueToFetchFullWhenNotGiven() {
        BatchSizes batchSizes = BatchSizes.builder()
                .build();
        assertThat(batchSizes.getFetchFull()).isEqualTo(BatchSizes.DEFAULT_BATCH_SIZE);
    }

    @Test
    public void buildShouldSetValueToFetchFullWhenGiven() {
        int expected = 123;
        BatchSizes batchSizes = BatchSizes.builder()
                .fetchFull(expected)
                .build();
        assertThat(batchSizes.getFetchFull()).isEqualTo(expected);
    }

    @Test
    public void buildShouldSetDefaultValueToCopyBatchSizeWhenNotGiven() {
        BatchSizes batchSizes = BatchSizes.builder()
                .build();
        assertThat(batchSizes.getCopyBatchSize()).isEqualTo(BatchSizes.DEFAULT_BATCH_SIZE);
    }

    @Test
    public void buildShouldSetValueToCopyBatchSizeWhenGiven() {
        int expected = 123;
        BatchSizes batchSizes = BatchSizes.builder()
                .copyBatchSize(expected)
                .build();
        assertThat(batchSizes.getCopyBatchSize()).isEqualTo(expected);
    }

    @Test
    public void buildShouldSetDefaultValueToMoveBatchSizeWhenNotGiven() {
        BatchSizes batchSizes = BatchSizes.builder()
                .build();
        assertThat(batchSizes.getMoveBatchSize()).isEqualTo(BatchSizes.DEFAULT_BATCH_SIZE);
    }

    @Test
    public void buildShouldSetValueToMoveBatchSizeWhenGiven() {
        int expected = 123;
        BatchSizes batchSizes = BatchSizes.builder()
                .moveBatchSize(expected)
                .build();
        assertThat(batchSizes.getMoveBatchSize()).isEqualTo(expected);
    }
}
