package org.apache.james.mailbox.jpa.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.FlagsUpdateCalculator;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;

import com.google.common.base.Optional;

public class TransactionalMessageMapper implements MessageMapper {

    private JPAMessageMapper messageMapper;

    public TransactionalMessageMapper(JPAMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public final <T> T execute(Transaction<T> transaction) throws MailboxException {
        return messageMapper.execute(transaction);
    }

    @Override
    public long getHighestModSeq(Mailbox mailbox) throws MailboxException {
        return messageMapper.getHighestModSeq(mailbox);
    }

    @Override
    public Optional<MessageUid> getLastUid(Mailbox mailbox) throws MailboxException {
        return messageMapper.getLastUid(mailbox);
    }

    @Override
    public Iterator<UpdatedFlags> updateFlags(Mailbox mailbox, FlagsUpdateCalculator flagsUpdateCalculator,
            MessageRange set) throws MailboxException {
        return messageMapper.updateFlags(mailbox, flagsUpdateCalculator, set);
    }

    @Override
    public MessageMetaData add(Mailbox mailbox, MailboxMessage message) throws MailboxException {
        return messageMapper.add(mailbox, message);
    }

    @Override
    public void endRequest() {
        messageMapper.endRequest();
    }

    @Override
    public MessageMetaData copy(Mailbox mailbox, MailboxMessage original) throws MailboxException {
        return messageMapper.copy(mailbox, original);
    }

    @Override
    public Iterator<MailboxMessage> findInMailbox(Mailbox mailbox, MessageRange set, FetchType fType, int max)
            throws MailboxException {
        return messageMapper.findInMailbox(mailbox, set, fType, max);
    }

    @Override
    public long countMessagesInMailbox(Mailbox mailbox) throws MailboxException {
        return messageMapper.countMessagesInMailbox(mailbox);
    }

    @Override
    public long countUnseenMessagesInMailbox(Mailbox mailbox) throws MailboxException {
        return messageMapper.countUnseenMessagesInMailbox(mailbox);
    }

    @Override
    public void delete(Mailbox mailbox, MailboxMessage message) throws MailboxException {
        messageMapper.delete(mailbox, message);
    }

    @Override
    public MessageUid findFirstUnseenMessageUid(Mailbox mailbox) throws MailboxException {
        return messageMapper.findFirstUnseenMessageUid(mailbox);
    }

    @Override
    public List<MessageUid> findRecentMessageUidsInMailbox(Mailbox mailbox) throws MailboxException {
        return messageMapper.findRecentMessageUidsInMailbox(mailbox);
    }

    @Override
    public Map<MessageUid, MessageMetaData> expungeMarkedForDeletionInMailbox(Mailbox mailbox, MessageRange set)
            throws MailboxException {
        return messageMapper.expungeMarkedForDeletionInMailbox(mailbox, set);
    }

    @Override
    public MessageMetaData move(Mailbox mailbox, MailboxMessage original) throws MailboxException {
        return messageMapper.move(mailbox, original);
    }


}
