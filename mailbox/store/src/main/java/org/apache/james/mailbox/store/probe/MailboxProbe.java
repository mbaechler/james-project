package org.apache.james.mailbox.store.probe;

import java.io.InputStream;
import java.util.Collection;
import java.util.Date;

import javax.mail.Flags;

import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.Mailbox;

public interface MailboxProbe {

    void createMailbox(String namespace, String user, String name);

    Mailbox getMailbox(String namespace, String user, String name);

    Collection<String> listUserMailboxes(String user);

    void deleteMailbox(String namespace, String user, String name);

    ComposedMessageId appendMessage(String username, MailboxPath mailboxPath, InputStream message, Date internalDate,
            boolean isRecent, Flags flags) throws MailboxException;

    void copyMailbox(String srcBean, String dstBean) throws Exception;

    void deleteUserMailboxesNames(String user) throws Exception;

    void reIndexMailbox(String namespace, String user, String name) throws Exception;

    void reIndexAll() throws Exception;

}