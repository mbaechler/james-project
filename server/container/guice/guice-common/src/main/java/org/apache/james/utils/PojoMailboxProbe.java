package org.apache.james.utils;

import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.mail.Flags;

import org.apache.commons.lang.NotImplementedException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MailboxMapperFactory;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.probe.MailboxProbe;

import com.google.common.base.Throwables;

public class PojoMailboxProbe implements GuiceProbe, MailboxProbe {

    private final MailboxManager mailboxManager;
    private final MailboxMapperFactory mailboxMapperFactory;

    @Inject
    private PojoMailboxProbe(MailboxManager mailboxManager, MailboxMapperFactory mailboxMapperFactory) {
        this.mailboxManager = mailboxManager;
        this.mailboxMapperFactory = mailboxMapperFactory;
    }

    @Override
    public void createMailbox(String namespace, String user, String name) {
        MailboxSession mailboxSession = null;
        try {
            mailboxSession = mailboxManager.createSystemSession(user, PojoSieveProbe.LOGGER);
            mailboxManager.startProcessingRequest(mailboxSession);
            mailboxManager.createMailbox(new MailboxPath(namespace, user, name), mailboxSession);
        } catch (MailboxException e) {
            throw Throwables.propagate(e);
        } finally {
            closeSession(mailboxSession);
        }
    }


    @Override
    public Mailbox getMailbox(String namespace, String user, String name) {
        MailboxSession mailboxSession = null;
        try {
            mailboxSession = mailboxManager.createSystemSession(user, PojoSieveProbe.LOGGER);
            MailboxMapper mailboxMapper = mailboxMapperFactory.getMailboxMapper(mailboxSession);
            return mailboxMapper.findMailboxByPath(new MailboxPath(namespace, user, name));
        } catch (MailboxException e) {
            throw Throwables.propagate(e);
        } finally {
            closeSession(mailboxSession);
        }
    }

    private void closeSession(MailboxSession session) {
        if (session != null) {
            mailboxManager.endProcessingRequest(session);
            try {
                mailboxManager.logout(session, true);
            } catch (MailboxException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    @Override
    public Collection<String> listUserMailboxes(String user) {
        MailboxSession mailboxSession = null;
        try {
            mailboxSession = mailboxManager.createSystemSession(user, PojoSieveProbe.LOGGER);
            mailboxManager.startProcessingRequest(mailboxSession);
            return searchUserMailboxes(user, mailboxSession)
                    .stream()
                    .map(MailboxMetaData::getPath)
                    .map(MailboxPath::getName)
                    .collect(Collectors.toList());
        } catch (MailboxException e) {
            throw Throwables.propagate(e);
        } finally {
            closeSession(mailboxSession);
        }
    }

    private List<MailboxMetaData> searchUserMailboxes(String username, MailboxSession session) throws MailboxException {
        return mailboxManager.search(
            new MailboxQuery(new MailboxPath(MailboxConstants.USER_NAMESPACE, username, ""),
                "*",
                session.getPathDelimiter()),
            session);
    }


    @Override
    public void deleteMailbox(String namespace, String user, String name) {
        MailboxSession mailboxSession = null;
        try {
            mailboxSession = mailboxManager.createSystemSession(user, PojoSieveProbe.LOGGER);
            mailboxManager.startProcessingRequest(mailboxSession);
            mailboxManager.deleteMailbox(new MailboxPath(namespace, user, name), mailboxSession);
        } catch (MailboxException e) {
            throw Throwables.propagate(e);
        } finally {
            closeSession(mailboxSession);
        }
    }

    @Override
    public ComposedMessageId appendMessage(String username, MailboxPath mailboxPath, InputStream message, Date internalDate, boolean isRecent, Flags flags) 
            throws MailboxException {
        
        MailboxSession mailboxSession = mailboxManager.createSystemSession(username, PojoSieveProbe.LOGGER);
        MessageManager messageManager = mailboxManager.getMailbox(mailboxPath, mailboxSession);
        return messageManager.appendMessage(message, internalDate, mailboxSession, isRecent, flags);
    }

    @Override
    public void copyMailbox(String srcBean, String dstBean) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public void deleteUserMailboxesNames(String user) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public void reIndexMailbox(String namespace, String user, String name) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public void reIndexAll() throws Exception {
        throw new NotImplementedException();
    }

}