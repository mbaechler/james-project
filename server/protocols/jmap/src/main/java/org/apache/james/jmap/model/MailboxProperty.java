package org.apache.james.jmap.model;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Optional;

public enum MailboxProperty implements Property {
    ID("id"),
    NAME("name"),
    PARENT_ID("parentId"),
    ROLE("role"),
    SORT_ORDER("sortOrder"),
    MUST_BE_ONLY_MAILBOX("mustBeOnlyMailbox"),
    MAY_READ_ITEMS("mayReadItems"),
    MAY_ADD_ITEMS("mayAddItems"),
    MAY_REMOVE_ITEMS("mayRemoveItems"),
    MAY_CREATE_CHILD("mayCreateChild"),
    MAY_RENAME("mayRename"),
    MAY_DELETE("mayDelete"),
    TOTAL_MESSAGES("totalMessages"),
    UNREAD_MESSAGES("unreadMessages"),
    TOTAL_THREADS("totalThreads"),
    UNREAD_THREADS("unreadThreads");

    private final String fieldName;

    MailboxProperty(String fieldName) {
        this.fieldName = fieldName;
    }

    public String asFieldName() {
        return fieldName;
    }

    public static Optional<MailboxProperty> findProperty(String value) {
        Preconditions.checkNotNull(value);
        return Arrays.stream(values())
            .filter(element -> element.fieldName.equals(value))
            .findAny();
    }
}
