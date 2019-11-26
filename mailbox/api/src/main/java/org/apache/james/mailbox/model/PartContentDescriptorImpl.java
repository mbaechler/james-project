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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

import org.apache.james.mailbox.model.MessageResult.FetchGroup.PartContentDescriptor;
import org.apache.james.mailbox.model.MessageResult.MimePath;


public class PartContentDescriptorImpl implements PartContentDescriptor {

    private final EnumSet<MessageResult.FetchGroupEnum> content;

    private final MimePath path;

    public PartContentDescriptorImpl(MimePath path) {
        this(EnumSet.noneOf(MessageResult.FetchGroupEnum.class), path);
    }

    public PartContentDescriptorImpl(EnumSet<MessageResult.FetchGroupEnum> content, MimePath path) {
        this.content = EnumSet.copyOf(content);
        this.path = path;
    }

    public void union(MessageResult.FetchGroupEnum... content) {
        this.content.addAll(Arrays.asList(content));
    }

    @Override
    public EnumSet<MessageResult.FetchGroupEnum> content() {
        return content;
    }

    @Override
    public MimePath path() {
        return path;
    }

    public int hashCode() {
        return Objects.hash(path);
    }

    public boolean equals(Object obj) {
        if (obj instanceof PartContentDescriptorImpl) {
            PartContentDescriptorImpl that = (PartContentDescriptorImpl) obj;
            return Objects.equals(this.path, that.path);
        }
        return false;
    }

}
