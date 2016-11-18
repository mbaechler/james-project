package org.apache.james.mailbox.store.mail.model;

import org.apache.james.mailbox.model.MessageId;

public class DefaultMessageId implements MessageId {

    public static class Factory implements MessageId.Factory {

        @Override
        public MessageId generate() {
            return new DefaultMessageId();
        }

        @Override
        public MessageId fromString(String serialized) {
            throw new IllegalStateException("Capabilities should prevent calling this method");
        }
        
    }

    @Override
    public String serialize() {
        throw new IllegalStateException("Capabilities should prevent calling this method");
    }
    
    @Override
    public final boolean equals(Object obj) {
        throw new IllegalStateException("Capabilities should prevent calling this method");
    }
    
    @Override
    public final int hashCode() {
        throw new IllegalStateException("Capabilities should prevent calling this method");
    }
}
