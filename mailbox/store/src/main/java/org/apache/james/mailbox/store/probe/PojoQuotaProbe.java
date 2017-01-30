package org.apache.james.mailbox.store.probe;

import org.apache.commons.lang.NotImplementedException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.mail.model.SerializableQuota;

public class PojoQuotaProbe implements QuotaProbe {

    @Override
    public String getQuotaRoot(String namespace, String user, String name) throws MailboxException {
        throw new NotImplementedException();
    }

    @Override
    public SerializableQuota getMessageCountQuota(String quotaRoot) throws MailboxException {
        throw new NotImplementedException();
    }

    @Override
    public SerializableQuota getStorageQuota(String quotaRoot) throws MailboxException {
        throw new NotImplementedException();
    }

    @Override
    public long getMaxMessageCount(String quotaRoot) throws MailboxException {
        throw new NotImplementedException();
    }


    @Override
    public long getMaxStorage(String quotaRoot) throws MailboxException {
        throw new NotImplementedException();
    }


    @Override
    public long getDefaultMaxMessageCount() throws MailboxException {
        throw new NotImplementedException();
    }


    @Override
    public long getDefaultMaxStorage() throws MailboxException {
        throw new NotImplementedException();
    }


    @Override
    public void setMaxMessageCount(String quotaRoot, long maxMessageCount) throws MailboxException {
        throw new NotImplementedException();
    }


    @Override
    public void setMaxStorage(String quotaRoot, long maxSize) throws MailboxException {
        throw new NotImplementedException();
    }


    @Override
    public void setDefaultMaxMessageCount(long maxDefaultMessageCount) throws MailboxException {
        throw new NotImplementedException();
    }


    @Override
    public void setDefaultMaxStorage(long maxDefaultSize) throws MailboxException {
        throw new NotImplementedException();
    }

}
