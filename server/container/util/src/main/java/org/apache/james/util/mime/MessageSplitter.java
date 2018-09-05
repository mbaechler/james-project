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

package org.apache.james.util.mime;

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.util.BodyOffsetInputStream;

public class MessageSplitter {
    public static Pair<byte[], byte[]> splitHeaderBody(MimeMessage message) throws IOException, MessagingException {
        byte[] messageAsArray = messageToArray(message);
        int bodyStartOctet = computeBodyStartOctet(messageAsArray);

        return Pair.of(
            getHeaderBytes(messageAsArray, bodyStartOctet),
            getBodyBytes(messageAsArray, bodyStartOctet));
    }

    private static byte[] messageToArray(MimeMessage message) throws IOException, MessagingException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        message.writeTo(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private static byte[] getHeaderBytes(byte[] messageContentAsArray, int bodyStartOctet) {
        ByteBuffer headerContent = ByteBuffer.wrap(messageContentAsArray, 0, bodyStartOctet);
        byte[] headerBytes = new byte[bodyStartOctet];
        headerContent.get(headerBytes);
        return headerBytes;
    }

    private static byte[] getBodyBytes(byte[] messageContentAsArray, int bodyStartOctet) {
        if (bodyStartOctet < messageContentAsArray.length) {
            ByteBuffer bodyContent = ByteBuffer.wrap(messageContentAsArray,
                bodyStartOctet,
                messageContentAsArray.length - bodyStartOctet);
            byte[] bodyBytes = new byte[messageContentAsArray.length - bodyStartOctet];
            bodyContent.get(bodyBytes);
            return bodyBytes;
        } else {
            return new byte[] {};
        }
    }

    private static int computeBodyStartOctet(byte[] messageAsArray) throws IOException {
        try (BodyOffsetInputStream bodyOffsetInputStream =
                 new BodyOffsetInputStream(new ByteArrayInputStream(messageAsArray))) {
            consume(bodyOffsetInputStream);

            if (bodyOffsetInputStream.getBodyStartOffset() == -1) {
                return 0;
            }
            return (int) bodyOffsetInputStream.getBodyStartOffset();
        }
    }

    private static void consume(InputStream in) throws IOException {
        IOUtils.copy(in, NULL_OUTPUT_STREAM);
    }
}
