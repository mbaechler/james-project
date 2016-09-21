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

package org.apache.james.imap.api.message;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.MessageRange;

import com.google.common.base.Objects;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

public final class UidRange implements Iterable<MessageUid> {

    public static List<UidRange> mergeRanges(List<UidRange> ranges) {
        if (ranges.isEmpty()) {
            return ranges;
        }

        RangeSet<MessageUid> rangeSet = TreeRangeSet.create();
        for (UidRange range: ranges) {
            rangeSet.add(Range.closed(range.getLowVal(), range.getHighVal()));
        }
        List<UidRange> result = new ArrayList<UidRange>();
        for (Range<MessageUid> range: rangeSet.asRanges()) {
            result.add(new UidRange(range.lowerEndpoint(), range.upperEndpoint()));
        }
        return result;
    }
    
    private MessageRange range;

    public UidRange(MessageUid singleVal) {
        this.range = singleVal.toRange();
    }

    public UidRange(MessageUid minValue, MessageUid messageUid) {
        if (minValue.compareTo(messageUid) > 0) {
            throw new IllegalArgumentException("LowVal must be <= HighVal");
        }
        this.range = MessageRange.range(minValue, messageUid);
    }

    public MessageUid getLowVal() {
        return range.getUidFrom();
    }

    public MessageUid getHighVal() {
        return range.getUidTo();
    }

    public boolean includes(MessageUid value) {
        return range.includes(value);
    }

    public int hashCode() {
        return range.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof UidRange) {
            UidRange other = (UidRange) obj;
            return Objects.equal(this.range, other.range);
        }
        return false;
    }

    public String toString() {
        return "IdRange : " + range.toString();
    }

    public String getFormattedString() {
        if (range.getUidFrom().equals(range.getUidTo())) {
            return String.valueOf(range.getUidFrom().asLong());
        } else {
            return String.format("%d:%d", range.getUidFrom().asLong(), range.getUidTo().asLong());
        }
    }

    @Override
    public Iterator<MessageUid> iterator() {
        return range.iterator();
    }

    public MessageRange toMessageRange() {
        return range;
    }

}
