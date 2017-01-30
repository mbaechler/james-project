package org.apache.james.mailbox.store.probe;

import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.mail.model.SerializableQuota;

public interface QuotaProbe {

    String getQuotaRoot(String namespace, String user, String name) throws MailboxException;

    SerializableQuota getMessageCountQuota(String quotaRoot) throws MailboxException;

    SerializableQuota getStorageQuota(String quotaRoot) throws MailboxException;

    long getMaxMessageCount(String quotaRoot) throws MailboxException;

    long getMaxStorage(String quotaRoot) throws MailboxException;

    long getDefaultMaxMessageCount() throws MailboxException;

    long getDefaultMaxStorage() throws MailboxException;

    void setMaxMessageCount(String quotaRoot, long maxMessageCount) throws MailboxException;

    void setMaxStorage(String quotaRoot, long maxSize) throws MailboxException;

    void setDefaultMaxMessageCount(long maxDefaultMessageCount) throws MailboxException;

    void setDefaultMaxStorage(long maxDefaultSize) throws MailboxException;

}