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

package org.apache.james.spamassassin;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.Username;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.util.ReactorUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.FileBackedOutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.Connection;
import reactor.netty.NettyInbound;
import reactor.netty.tcp.TcpClient;

/**
 * Sends the message through daemonized SpamAssassin (spamd), visit <a
 * href="SpamAssassin.org">SpamAssassin.org</a> for info on configuration.
 */
public class SpamAssassinInvoker {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpamAssassinInvoker.class);
    public static final int MAX_LINE_LENGTH = 8 * 1024;
    public static final int BUFFER_SIZE = 8 * 1024;
    public static final int FILE_THRESHOLD = 256 * 1024;

    enum MessageClass {
        HAM("ham"),
        SPAM("spam");

        private final String value;

        MessageClass(String value) {
            this.value = value;
        }
    }

    private static final int SPAM_INDEX = 1;
    private static final int HITS_INDEX = 3;
    private static final int REQUIRED_HITS_INDEX = 5;
    private static final String CRLF = "\r\n";

    private final MetricFactory metricFactory;
    private final String spamdHost;
    private final int spamdPort;

    /**
     * Init the spamassassin invoker
     *
     * @param spamdHost
     *            The host on which spamd runs
     * @param spamdPort
     */
    public SpamAssassinInvoker(MetricFactory metricFactory, String spamdHost, int spamdPort) {
        this.metricFactory = metricFactory;
        this.spamdHost = spamdHost;
        this.spamdPort = spamdPort;
    }

    /**
     * Scan a MimeMessage for spam by passing it to spamd.
     *
     * @param message
     *            The MimeMessage to scan
     * @return true if spam otherwise false
     * @throws MessagingException
     *             if an error on scanning is detected
     */
    public Mono<SpamAssassinResult> scanMail(MimeMessage message, Username username) {
        return metricFactory.runPublishingTimerMetric(
            "spamAssassin-check",
            Throwing.supplier(
                () -> scanMailWithAdditionalHeaders(message,
                    "User: " + username.asString()))
                .sneakyThrow());
    }

    public Mono<SpamAssassinResult> scanMail(MimeMessage message) {
        return Mono.from(metricFactory.runPublishingTimerMetric(
            "spamAssassin-check",
            scanMailWithoutAdditionalHeaders(message)));
    }

    private Mono<SpamAssassinResult> scanMailWithAdditionalHeaders(MimeMessage message, String... additionalHeaders) {
        return createConnection()
            .flatMap(connection ->
                sendScanMailRequest(message, connection, additionalHeaders)
                    .then(getSpamAssassinResult(connection.inbound())));
    }

    private Mono<? extends Connection> createConnection() {
        return TcpClient.create()
            .host(spamdHost)
            .port(spamdPort)
            .doOnConnected(c -> c.addHandlerLast("codec",
                new LineBasedFrameDecoder(MAX_LINE_LENGTH)))
            .connect();
    }

    private Mono<SpamAssassinResult> getSpamAssassinResult(NettyInbound inbound) {
        return inbound.receive().asString()
            .filter(this::isSpam)
            .map(this::processSpam)
            .singleOrEmpty()
            .switchIfEmpty(Mono.just(SpamAssassinResult.empty()));
    }

    private Mono<Void> sendScanMailRequest(MimeMessage message, Connection connection, String[] additionalHeaders) {
        return FluxOutputStream.transfer(outputStream -> writableMessage(message, outputStream),
            buffers -> sendRequestToSpamd(Mono.fromCallable(() -> headerAsString(additionalHeaders)), buffers.map(Unpooled::wrappedBuffer), connection));
    }

    private void writableMessage(MimeMessage message, OutputStream outputStream) {
        try {
            message.writeTo(outputStream);
        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Mono<Void> sendRequestToSpamd(Mono<String> header, Publisher<ByteBuf> message, Connection connection) {
        return connection.outbound()
            .sendString(header)
            .send(message)
            // the channel output need to be closed for spamassassin to send the response (TCP Half Closed Connection).
            .then(Mono.fromRunnable(() -> ((SocketChannel) connection.channel()).shutdownOutput()))
            .then()
            .subscribeOn(Schedulers.elastic())
            .doOnError(e -> LOGGER.error("Error when sending request to spamd", e));
    }

    private String headerAsString(String[] additionalHeaders) {
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("CHECK SPAMC/1.2");
        headerBuilder.append(CRLF);

        Arrays.stream(additionalHeaders)
            .forEach(header -> {
                headerBuilder.append(header);
                headerBuilder.append(CRLF);
            });
        headerBuilder.append(CRLF);
        return headerBuilder.toString();
    }

    private Mono<SpamAssassinResult> scanMailWithoutAdditionalHeaders(MimeMessage message) {
        return scanMailWithAdditionalHeaders(message);
    }

    private SpamAssassinResult processSpam(String line) {
        List<String> elements = Lists.newArrayList(Splitter.on(' ').split(line));

        return builderFrom(elements)
            .hits(elements.get(HITS_INDEX))
            .requiredHits(elements.get(REQUIRED_HITS_INDEX))
            .build();
    }

    private SpamAssassinResult.Builder builderFrom(List<String> elements) {
        if (spam(elements.get(SPAM_INDEX))) {
            return SpamAssassinResult.asSpam();
        } else {
            return SpamAssassinResult.asHam();
        }
    }

    private boolean spam(String string) {
        try {
            return Boolean.parseBoolean(string);
        } catch (Exception e) {
            LOGGER.warn("Fail parsing spamassassin answer: " + string, e);
            return false;
        }
    }

    private boolean isSpam(String line) {
        return line.startsWith("Spam:");
    }

    /**
     * Tell spamd that the given MimeMessage is a spam.
     * 
     * @param message
     *            The MimeMessage to tell
     * @throws MessagingException
     *             if an error occured during learning.
     */
    public Publisher<Boolean> learnAsSpam(MessageToLearn message, Username username) {
        return metricFactory.runPublishingTimerMetric(
            "spamAssassin-spam-report",
           reportMessageAs(message, username, MessageClass.SPAM));
    }

    /**
     * Tell spamd that the given MimeMessage is a ham.
     *
     * @param message
     *            The MimeMessage to tell
     * @throws MessagingException
     *             if an error occured during learning.
     */
    public Publisher<Boolean> learnAsHam(MessageToLearn message, Username username) {
        return metricFactory.runPublishingTimerMetric(
            "spamAssassin-ham-report",
            reportMessageAs(message, username, MessageClass.HAM));
    }

    private Mono<Boolean> reportMessageAs(MessageToLearn message, Username username, MessageClass messageClass) {
        return createConnection()
            .flatMap(connection -> {
                    LOGGER.debug("Report mail as {}", messageClass);

                    Publisher<ByteBuf> messageByteBuf = ReactorUtils.toChunks(message.getMessage(), BUFFER_SIZE)
                        .map(Unpooled::wrappedBuffer);

                    return sendRequestToSpamd(Mono.just(learnHamRequestHeader(message.getContentLength(), username, messageClass)), messageByteBuf, connection)
                        .then(connection.inbound()
                            .receive()
                            .asString()
                            .any(this::hasBeenSet)
                            .doOnNext(hasBeenSet -> {
                                if (hasBeenSet) {
                                    LOGGER.debug("Reported mail as {} succeeded", messageClass);
                                } else {
                                    LOGGER.debug("Reported mail as {} failed", messageClass);
                                }
                            }))
                        .onErrorMap(IOException.class, e -> new MessagingException("Error communicating with spamd on " + spamdHost + ":" + spamdPort, e));
                }
            );
    }

    private String learnHamRequestHeader(long contentLength, Username username, MessageClass messageClass) {
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("TELL SPAMC/1.2");
        headerBuilder.append(CRLF);
        headerBuilder.append("Content-length: " + contentLength);
        headerBuilder.append(CRLF);
        headerBuilder.append("Message-class: " + messageClass.value);
        headerBuilder.append(CRLF);
        headerBuilder.append("Set: local, remote");
        headerBuilder.append(CRLF);
        headerBuilder.append("User: " + username.asString());
        headerBuilder.append(CRLF);
        headerBuilder.append(CRLF);
        return headerBuilder.toString();
    }

    private boolean hasBeenSet(String line) {
        return line.startsWith("DidSet: ");
    }
    
    public static class FluxOutputStream {

        @FunctionalInterface
        public interface Writable {
            void writeTo(OutputStream outputStream) throws IOException;
        }

            @FunctionalInterface
        public interface Consumer {
            Mono<Void> consume(Flux<ByteBuffer> buffers);
        }

        public static Mono<Void> transfer(Writable w, Consumer consumer) {
            PipedInputStream pipedInputStream = new PipedInputStream();
            return Mono.fromCallable(() -> new PipedOutputStream(pipedInputStream))
                    .flatMapMany(pipedOutputStream -> Mono.zip(runWritable(w, pipedOutputStream).subscribeOn(Schedulers.elastic()), runConsumer(consumer, pipedInputStream).subscribeOn(Schedulers.elastic())))
                    .then();
        }

        private static Mono<Void> runWritable(Writable w, PipedOutputStream pipedOutputStream) {
            return Mono.fromRunnable(() -> {
                try {
                    w.writeTo(pipedOutputStream);
                } catch(IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    try {
                        pipedOutputStream.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        }

        private static Mono<Void> runConsumer(Consumer consumer, PipedInputStream inputStream) {
            return consumer.consume(ReactorUtils.toChunks(inputStream, 8000));
        }
}

}
