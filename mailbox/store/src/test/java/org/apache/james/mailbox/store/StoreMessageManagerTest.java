package org.apache.james.mailbox.store;

import static org.apache.james.mailbox.manager.MailboxManagerFixture.MAILBOX_PATH1;
import static org.apache.james.mailbox.manager.MailboxManagerFixture.OTHER_USER;
import static org.apache.james.mailbox.manager.MailboxManagerFixture.THIRD_USER;
import static org.apache.james.mailbox.manager.MailboxManagerFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.mailbox.exception.UnsupportedRightException;
import org.apache.james.mailbox.manager.MailboxManagerFixture;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxACL.Right;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MessageAssert;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.junit.Test;

public class StoreMessageManagerTest {

    public static final long UID_VALIDITY = 3421l;

    @Test
    public void filteredForSessionShouldBeIdentityWhenOwner() throws UnsupportedRightException {
        MailboxACL acl = new MailboxACL()
            .apply(MailboxACL.command().rights(Right.Read, Right.Write).forUser(OTHER_USER).asAddition())
            .apply(MailboxACL.command().rights(Right.Read, Right.Write, Right.Administer).forUser(THIRD_USER).asAddition());
        MailboxACL actual = StoreMessageManager.filteredForSession(
            new SimpleMailbox(MAILBOX_PATH1, UID_VALIDITY), acl, new MockMailboxSession(USER));
        assertThat(actual).isEqualTo(acl);
    }


    @Test
    public void filteredForSessionShouldBeIdentityWhenAdmin() throws UnsupportedRightException {
        MailboxACL acl = new MailboxACL()
            .apply(MailboxACL.command().rights(Right.Read, Right.Write).forUser(OTHER_USER).asAddition())
            .apply(MailboxACL.command().rights(Right.Read, Right.Write, Right.Administer).forUser(THIRD_USER).asAddition());
        MailboxACL actual = StoreMessageManager.filteredForSession(
            new SimpleMailbox(MAILBOX_PATH1, UID_VALIDITY), acl, new MockMailboxSession(THIRD_USER));
        assertThat(actual).isEqualTo(acl);
    }

    @Test
    public void filteredForSessionShouldContainOnlyLoggedUserWhenReadWriteAccess() throws UnsupportedRightException {
        MailboxACL acl = new MailboxACL()
            .apply(MailboxACL.command().rights(Right.Read, Right.Write).forUser(OTHER_USER).asAddition())
            .apply(MailboxACL.command().rights(Right.Read, Right.Write, Right.Administer).forUser(THIRD_USER).asAddition());
        MailboxACL actual = StoreMessageManager.filteredForSession(
            new SimpleMailbox(MAILBOX_PATH1, UID_VALIDITY), acl, new MockMailboxSession(OTHER_USER));
        assertThat(actual.getEntries()).containsKey(MailboxACL.EntryKey.createUser(OTHER_USER));
    }
}