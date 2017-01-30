package org.apache.james.cli.probe.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;

import javax.mail.Flags;
import javax.management.MalformedObjectNameException;

import org.apache.james.adapter.mailbox.MailboxCopierManagementMBean;
import org.apache.james.adapter.mailbox.MailboxManagerManagementMBean;
import org.apache.james.adapter.mailbox.ReIndexerManagementMBean;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.probe.MailboxProbe;

public class JmxMailboxProbe implements MailboxProbe, JmxProbe {
    
    private final static String MAILBOXCOPIER_OBJECT_NAME = "org.apache.james:type=component,name=mailboxcopier";
    private final static String MAILBOXMANAGER_OBJECT_NAME = "org.apache.james:type=component,name=mailboxmanagerbean";
    private final static String REINDEXER_OBJECT_NAME = "org.apache.james:type=component,name=reindexerbean";
    
    private MailboxCopierManagementMBean mailboxCopierManagement;
    private MailboxManagerManagementMBean mailboxManagerManagement;
    private ReIndexerManagementMBean reIndexerManagement;
    
    public JmxMailboxProbe connect(JmxConnection jmxc) throws IOException {
        try {
            mailboxCopierManagement = jmxc.retrieveBean(MailboxCopierManagementMBean.class, MAILBOXCOPIER_OBJECT_NAME);
            mailboxManagerManagement = jmxc.retrieveBean(MailboxManagerManagementMBean.class, MAILBOXMANAGER_OBJECT_NAME);
            reIndexerManagement = jmxc.retrieveBean(ReIndexerManagementMBean.class, REINDEXER_OBJECT_NAME);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Invalid ObjectName? Please report this as a bug.", e);
        }
        return this;
    }
    

    @Override
    public void copyMailbox(String srcBean, String dstBean) throws Exception {
        mailboxCopierManagement.copy(srcBean, dstBean);
    }

    @Override
    public void deleteUserMailboxesNames(String user) throws Exception {
        mailboxManagerManagement.deleteMailboxes(user);
    }

    @Override
    public void createMailbox(String namespace, String user, String name) {
        mailboxManagerManagement.createMailbox(namespace, user, name);
    }

    @Override
    public Collection<String> listUserMailboxes(String user) {
        return mailboxManagerManagement.listMailboxes(user);
    }

    @Override
    public void deleteMailbox(String namespace, String user, String name) {
        mailboxManagerManagement.deleteMailbox(namespace, user, name);
    }

    @Override
    public void reIndexMailbox(String namespace, String user, String name) throws Exception {
        reIndexerManagement.reIndex(namespace, user, name);
    }

    @Override
    public void reIndexAll() throws Exception {
        reIndexerManagement.reIndex();
    }


    @Override
    public Mailbox getMailbox(String namespace, String user, String name) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public ComposedMessageId appendMessage(String username, MailboxPath mailboxPath, InputStream message,
            Date internalDate, boolean isRecent, Flags flags) throws MailboxException {
        // TODO Auto-generated method stub
        return null;
    }

}