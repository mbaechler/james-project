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

package org.apache.james.blob.cassandra;

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.backends.cassandra.init.configuration.CassandraConfiguration;
import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.blob.api.HashBlobId;
import org.apache.james.blob.cassandra.BlobTable.BlobParts;
import org.apache.james.blob.cassandra.utils.DataChunker;
import org.apache.james.blob.cassandra.utils.PipedStreamSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.github.fge.lambdas.Throwing;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Bytes;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class CassandraBlobsDAO implements BlobStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraBlobsDAO.class);
    private static final int PREFETCH = 4;
    private static final int MAX_CONCURRENCY = 4;
    private final CassandraAsyncExecutor cassandraAsyncExecutor;
    private final PreparedStatement insert;
    private final PreparedStatement insertPart;
    private final PreparedStatement select;
    private final PreparedStatement selectPart;
    private final DataChunker dataChunker;
    private final CassandraConfiguration configuration;
    private final HashBlobId.Factory blobIdFactory;

    @Inject
    public CassandraBlobsDAO(Session session, CassandraConfiguration cassandraConfiguration, HashBlobId.Factory blobIdFactory) {
        this.cassandraAsyncExecutor = new CassandraAsyncExecutor(session);
        this.configuration = cassandraConfiguration;
        this.blobIdFactory = blobIdFactory;
        this.dataChunker = new DataChunker();
        this.insert = prepareInsert(session);
        this.select = prepareSelect(session);

        this.insertPart = prepareInsertPart(session);
        this.selectPart = prepareSelectPart(session);
    }

    @VisibleForTesting
    public CassandraBlobsDAO(Session session) {
        this(session, CassandraConfiguration.DEFAULT_CONFIGURATION, new HashBlobId.Factory());
    }

    private PreparedStatement prepareSelect(Session session) {
        return session.prepare(select()
            .from(BlobTable.TABLE_NAME)
            .where(eq(BlobTable.ID, bindMarker(BlobTable.ID))));
    }

    private PreparedStatement prepareSelectPart(Session session) {
        return session.prepare(select()
            .from(BlobParts.TABLE_NAME)
            .where(eq(BlobTable.ID, bindMarker(BlobTable.ID)))
            .and(eq(BlobParts.CHUNK_NUMBER, bindMarker(BlobParts.CHUNK_NUMBER))));
    }

    private PreparedStatement prepareInsert(Session session) {
        return session.prepare(insertInto(BlobTable.TABLE_NAME)
            .value(BlobTable.ID, bindMarker(BlobTable.ID))
            .value(BlobTable.NUMBER_OF_CHUNK, bindMarker(BlobTable.NUMBER_OF_CHUNK)));
    }

    private PreparedStatement prepareInsertPart(Session session) {
        return session.prepare(insertInto(BlobParts.TABLE_NAME)
            .value(BlobTable.ID, bindMarker(BlobTable.ID))
            .value(BlobParts.CHUNK_NUMBER, bindMarker(BlobParts.CHUNK_NUMBER))
            .value(BlobParts.DATA, bindMarker(BlobParts.DATA)));
    }

    @Override
    public CompletableFuture<BlobId> save(byte[] data) {
        Preconditions.checkNotNull(data);

        return saveAsMono(data).toFuture();
    }

    private Mono<BlobId> saveAsMono(byte[] data) {
        BlobId blobId = blobIdFactory.forPayload(data);
        return saveBlobParts(data, blobId)
            .flatMap(numberOfChunk -> saveBlobPartsReferences(blobId, numberOfChunk));
    }

    private Mono<Integer> saveBlobParts(byte[] data, BlobId blobId) {
        Stream<Pair<Integer, ByteBuffer>> chunks = dataChunker.chunk(data, configuration.getBlobPartSize());
        return Flux.fromStream(chunks)
            .publishOn(Schedulers.elastic(), PREFETCH)
            .flatMap(pair -> writePart(pair.getValue(), blobId, getChunkNum(pair)))
            .collect(Collectors.maxBy(Comparator.comparingInt(x -> x)))
            .flatMap(Mono::justOrEmpty)
            .map(this::numToCount)
            .defaultIfEmpty(0);
    }

    private int numToCount(int number) {
        return number + 1;
    }

    private Integer getChunkNum(Pair<Integer, ByteBuffer> pair) {
        return pair.getKey();
    }

    private Mono<Integer> writePart(ByteBuffer data, BlobId blobId, int position) {
        return Mono.fromCompletionStage(cassandraAsyncExecutor.executeVoid(
            insertPart.bind()
                .setString(BlobTable.ID, blobId.asString())
                .setInt(BlobParts.CHUNK_NUMBER, position)
                .setBytes(BlobParts.DATA, data))
            .thenApply(ignored -> position));
    }

    private Mono<BlobId> saveBlobPartsReferences(BlobId blobId, int numberOfChunk) {
        return Mono.fromCompletionStage(cassandraAsyncExecutor.executeVoid(insert.bind()
                .setString(BlobTable.ID, blobId.asString())
                .setInt(BlobTable.NUMBER_OF_CHUNK, numberOfChunk))
            .thenApply(ignored -> blobId));
    }

    @Override
    public CompletableFuture<byte[]> readBytes(BlobId blobId) {
        return readBlobParts(blobId)
            .collectList()
            .map(parts -> Bytes.concat(parts.toArray(new byte[0][])))
            .toFuture();
    }

    private Mono<Integer> selectRowCount(BlobId blobId) {
        return Mono.fromCompletionStage(
            cassandraAsyncExecutor.executeSingleRow(
                select.bind()
                    .setString(BlobTable.ID, blobId.asString())))
            .flatMap(Mono::justOrEmpty)
            .map(row -> row.getInt(BlobTable.NUMBER_OF_CHUNK));
    }

    private byte[] rowToData(Row row) {
        byte[] data = new byte[row.getBytes(BlobParts.DATA).remaining()];
        row.getBytes(BlobParts.DATA).get(data);
        return data;
    }

    private Mono<BlobPart> readPart(BlobId blobId, int position) {
        return Mono.fromCompletionStage(cassandraAsyncExecutor.executeSingleRow(
            selectPart.bind()
                .setString(BlobTable.ID, blobId.asString())
                .setInt(BlobParts.CHUNK_NUMBER, position)))
            .map(row ->
                row.map(this::rowToData)
                    .orElseThrow(() -> new IllegalStateException(
                        String.format("Missing blob part for blobId %s and position %d", blobId, position))))
            .flatMap(Mono::justOrEmpty)
            .map(bytes -> new BlobPart(position, bytes));
    }

    private static class BlobPart {
        private final int position;
        private final byte[] bytes;

        public BlobPart(int position, byte[] bytes) {
            Preconditions.checkArgument(position >= 0, "position need to be positive");
            this.position = position;
            this.bytes = bytes;
        }
    }

    @Override
    public InputStream read(BlobId blobId) {
            PipedInputStream pipedInputStream = new PipedInputStream();
            readBlobParts(blobId)
                .subscribe(new PipedStreamSubscriber(pipedInputStream));
            return pipedInputStream;
    }

    private Flux<byte[]> readBlobParts(BlobId blobId) {
        return selectRowCount(blobId)
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException(String.format("Could not retrieve blob metadata for %s", blobId))))
            .publishOn(Schedulers.elastic())
            .flatMapMany(rowCount -> Flux.range(0, rowCount))
            .flatMapSequential(partIndex -> readPart(blobId, partIndex), MAX_CONCURRENCY, PREFETCH)
            .publishOn(Schedulers.elastic(), PREFETCH)
            .map(blobPart -> blobPart.bytes);
    }

    @Override
    public CompletableFuture<BlobId> save(InputStream data) {
        Preconditions.checkNotNull(data);
        return Mono.fromSupplier(Throwing.supplier(() -> IOUtils.toByteArray(data)).sneakyThrow())
            .publishOn(Schedulers.elastic())
            .flatMap(this::saveAsMono)
            .toFuture();
    }
}
