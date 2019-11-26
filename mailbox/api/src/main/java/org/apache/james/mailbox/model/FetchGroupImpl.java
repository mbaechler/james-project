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
import java.util.HashSet;
import java.util.Set;

import org.apache.james.mailbox.model.MessageResult.MimePath;

/**
 * Specifies a fetch group.
 */
public class FetchGroupImpl implements MessageResult.FetchGroup {

    public static final MessageResult.FetchGroup MINIMAL = new FetchGroupImpl(EnumSet.noneOf(MessageResult.FetchGroupEnum.class));

    public static final MessageResult.FetchGroup HEADERS = new FetchGroupImpl(EnumSet.of(MessageResult.FetchGroupEnum.HEADERS));

    public static final MessageResult.FetchGroup FULL_CONTENT = new FetchGroupImpl(EnumSet.of(MessageResult.FetchGroupEnum.FULL_CONTENT));

    public static final MessageResult.FetchGroup BODY_CONTENT = new FetchGroupImpl(EnumSet.of(MessageResult.FetchGroupEnum.BODY_CONTENT));

    private EnumSet<MessageResult.FetchGroupEnum> content;

    private Set<PartContentDescriptor> partContentDescriptors;

    public FetchGroupImpl() {
        this(EnumSet.noneOf(MessageResult.FetchGroupEnum.class), new HashSet<>());
    }

    public FetchGroupImpl(EnumSet<MessageResult.FetchGroupEnum> content) {
        this(content, new HashSet<>());
    }

    public FetchGroupImpl(EnumSet<MessageResult.FetchGroupEnum> content, Set<PartContentDescriptor> partContentDescriptors) {
        this.content = EnumSet.copyOf(content);
        this.partContentDescriptors = partContentDescriptors;
    }

    @Override
    public EnumSet<MessageResult.FetchGroupEnum> content() {
        return EnumSet.copyOf(content);
    }

    public void union(MessageResult.FetchGroupEnum... additions) {
        this.content.addAll(Arrays.asList(additions));
    }

    public void union(EnumSet<MessageResult.FetchGroupEnum> addition) {
        this.content.addAll(addition);
    }

    public String toString() {
        return "Fetch " + content;
    }

    @Override
    public Set<PartContentDescriptor> getPartContentDescriptors() {
        return partContentDescriptors;
    }

    /**
     * Adds content for the particular part.
     * 
     * @param path
     *            <code>MimePath</code>, not null
     * @param content
     *            bitwise content constant
     */
    public void addPartContent(MimePath path, MessageResult.FetchGroupEnum content) {
        if (partContentDescriptors == null) {
            partContentDescriptors = new HashSet<>();
        }
        PartContentDescriptorImpl currentDescriptor = (PartContentDescriptorImpl) partContentDescriptors.stream()
            .filter(descriptor -> path.equals(descriptor.path()))
            .findFirst()
            .orElseGet(() -> {
                PartContentDescriptorImpl result = new PartContentDescriptorImpl(path);
                partContentDescriptors.add(result);
                return result;
            });

        currentDescriptor.union(content);
    }
}
