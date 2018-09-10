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

package org.apache.james.protocols.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import org.apache.james.protocols.api.future.FutureResponseImpl;
import org.apache.james.protocols.api.handler.LineHandler;
import org.junit.Test;

import com.github.steveash.guavate.Guavate;

/**
 * Test-case for PROTOCOLS-62
 *
 */
public class AbstractProtocolTransportTest {

    private static final String US_ASCII = "US-ASCII";
    
    @Test
    public void testWriteOrder() throws Exception {
        final List<Response> messages = IntStream.range(0, 2000)
            .mapToObj(i -> new TestResponse())
            .collect(Guavate.toImmutableList());

        checkWrittenResponses(messages);
    }
    
    @Test
    public void testWriteOrderFutureResponseAllReady() throws Exception {
        final List<Response> messages = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
                FutureResponseImpl r = new FutureResponseImpl();
                r.setResponse(new TestResponse());
                messages.add(r);
        }
        checkWrittenResponses(messages);
    }
        
       
    @Test
    public void testWriteOrderFutureResponse() throws Exception {
        final List<Response> messages = IntStream.range(0, 2000)
            .mapToObj(i -> new FutureResponseImpl())
            .collect(Guavate.toImmutableList());

        notifyFutureResponses(messages, false);
        
        checkWrittenResponses(messages);
    }

    @Test
    public void testWriteOrderFutureResponseReverseNotify() throws Exception {
        final List<Response> messages = IntStream.range(0, 2000)
            .mapToObj(i -> new FutureResponseImpl())
            .collect(Guavate.toImmutableList());

        notifyFutureResponses(messages, true);

        checkWrittenResponses(messages);
    }
    
    @Test
    public void testWriteOrderMixedResponse() throws Exception {
        final List<Response> messages = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            if (i % 2 == 0) {
                messages.add(new TestResponse());
            } else {
                messages.add(new FutureResponseImpl());
            }

        }
        notifyFutureResponses(messages, false);
        
        checkWrittenResponses(messages);
    }
    
    private void notifyFutureResponses(final List<Response> messages, final boolean reverse) {
        new Thread(() -> {
            try {
                Thread.sleep(200);
                List<Response> responses = new ArrayList<>(messages);
                if (reverse) {
                    Collections.reverse(responses);
                }

                for (Response r : responses) {
                    if (r instanceof FutureResponseImpl) {
                        ((FutureResponseImpl) r).setResponse(new TestResponse());
                    }
                }

            } catch (InterruptedException e) {
                throw new RuntimeException();
            }

        }).start();
    }
    
    private void checkWrittenResponses(List<Response> messages) throws InterruptedException, UnsupportedEncodingException {
        final List<byte[]> writtenMessages = new ArrayList<>();

        final CountDownLatch latch = new CountDownLatch(messages.size());

        AbstractProtocolTransport transport = new AbstractProtocolTransport() {

            @Override
            public void setReadable(boolean readable) {
                throw new UnsupportedOperationException();
            }

            
            @Override
            public void popLineHandler() {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public boolean isTLSStarted() {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public boolean isStartTLSSupported() {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public boolean isReadable() {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public InetSocketAddress getRemoteAddress() {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public int getPushedLineHandlerCount() {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public InetSocketAddress getLocalAddress() {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public String getId() {
                throw new UnsupportedOperationException();
            }
            
            @Override
            protected void writeToClient(InputStream in, ProtocolSession session, boolean startTLS) {
                throw new UnsupportedOperationException();
            }
            
            @Override
            protected void writeToClient(byte[] bytes, ProtocolSession session, boolean startTLS) {
                writtenMessages.add(bytes);
                latch.countDown();
            }
            
            @Override
            protected void close() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void pushLineHandler(LineHandler<? extends ProtocolSession> overrideCommandHandler, ProtocolSession session) {
                throw new UnsupportedOperationException();                
            }
        };
        for (Response message: messages) {
            transport.writeResponse(message, null);
        }
        latch.await();
        
        assertThat(writtenMessages.size()).isEqualTo(messages.size());
        
        for (int i = 0; i < messages.size(); i++) {
            Response response = messages.get(i);
            checkBytesEquals(response.getLines().get(0).toString().getBytes(US_ASCII), writtenMessages.get(i));
        }
    }
    
    private void checkBytesEquals(byte[] expected, byte[] received) throws UnsupportedEncodingException {
        
        assertThat(received.length - 2).describedAs("'" + new String(expected, US_ASCII) + "'=>'" + new String(received, US_ASCII) + "'").isEqualTo(expected.length);
        for (int i = 0; i < expected.length; i++) {
            assertThat(received[i]).describedAs("'" + new String(expected, US_ASCII) + "'=>'" + new String(received, US_ASCII) + "'").isEqualTo(expected[i]);
        }
    }
    
    private static final class TestResponse implements Response {

        private final String msg;

        public TestResponse() {
            this.msg =  UUID.randomUUID().toString();
        }
        
        @Override
        public String getRetCode() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<CharSequence> getLines() {
            return Arrays.asList((CharSequence)msg);
        }

        @Override
        public boolean isEndSession() {
            return false;
        }
    }
}
