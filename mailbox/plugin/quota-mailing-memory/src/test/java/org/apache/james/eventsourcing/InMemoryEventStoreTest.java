package org.apache.james.eventsourcing;

public class InMemoryEventStoreTest implements EventStoreTest {

    @Override
    public EventStore create() {
        return new InMemoryEventStore();
    }
}