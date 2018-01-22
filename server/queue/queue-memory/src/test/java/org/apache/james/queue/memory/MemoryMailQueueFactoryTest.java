package org.apache.james.queue.memory;

import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.queue.api.MailQueueFactoryContract;
import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.queue.api.ManageableMailQueueFactoryContract;
import org.apache.james.queue.api.RawMailQueueItemDecoratorFactory;
import org.junit.jupiter.api.BeforeEach;

class MemoryMailQueueFactoryTest implements MailQueueFactoryContract<ManageableMailQueue>, ManageableMailQueueFactoryContract {

    MemoryMailQueueFactory memoryMailQueueFactory;

    @BeforeEach
    void setup() {
        memoryMailQueueFactory = new MemoryMailQueueFactory(new RawMailQueueItemDecoratorFactory());
    }

    @Override
    public MailQueueFactory<ManageableMailQueue> getMailQueueFactory() {
        return memoryMailQueueFactory;
    }
}