package org.apache.james.jmap.methods;

import java.util.Collection;

import org.apache.james.mailbox.exception.MailboxException;

public class MailboxSendingNotAllowedException extends MailboxException {

    private Collection<String> allowedFroms;

    public MailboxSendingNotAllowedException(Collection<String> allowedFroms) {
        super();
        this.allowedFroms = allowedFroms;
    }
    
    public Collection<String> getAllowedFroms() {
        return allowedFroms;
    }
}
