package org.apache.james.eventsourcing;

import static org.apache.james.eventsourcing.EventStoreTest.TestAggregateId.testId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Comparator;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

interface EventStoreTest {

    TestAggregateId AGGREGATE_1 = testId(1);
    TestAggregateId AGGREGATE_2 = testId(2);

    class TestAggregateId implements AggregateId {

        static TestAggregateId testId(int id) {
            return new TestAggregateId(id);
        }

        final int id;

        private TestAggregateId(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestAggregateId that = (TestAggregateId) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("id", id)
                .toString();
        }
    }

    class TestEvent implements Event {

        final EventId id;
        final TestAggregateId aggregateId;
        final String data;

        public TestEvent(EventId id, TestAggregateId aggregateId, String data) {
            this.id = id;
            this.aggregateId = aggregateId;
            this.data = data;
        }

        @Override
        public EventId eventId() {
            return id;
        }

        @Override
        public TestAggregateId getAggregateId() {
            return aggregateId;
        }

        public String getData() {
            return data;
        }

        @Override
        public int compareTo(Event o) {
            return Comparator.<EventId>naturalOrder().compare(id, o.eventId());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestEvent testEvent = (TestEvent) o;
            return Objects.equals(id, testEvent.id) &&
                Objects.equals(aggregateId, testEvent.aggregateId) &&
                Objects.equals(data, testEvent.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, aggregateId, data);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("aggregateId", aggregateId)
                .add("data", data)
                .toString();
        }
    }

    EventStore create();

    @Test
    default void getEventsOfAggregateShouldThrowOnNullAggregateId() {
        EventStore testee = create();
        assertThatThrownBy(() -> testee.getEventsOfAggregate(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    default void appendShouldThrowWhenEventFromSeveralAggregates() {
        EventStore testee = create();
        TestEvent event1 = new TestEvent(EventId.first(), AGGREGATE_1, "first");
        TestEvent event2 = new TestEvent(event1.eventId().next(), AGGREGATE_2, "second");
        assertThatThrownBy(() -> testee.appendAll(event1, event2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    default void appendShouldDoNothingOnEmptyEventList() {
        EventStore testee = create();
        assertThatCode(testee::appendAll).doesNotThrowAnyException();
    }

    @Test
    default void appendShouldThrowWhenTryingToRewriteHistory() {
        EventStore testee = create();
        TestEvent event1 = new TestEvent(EventId.first(), AGGREGATE_1, "first");
        testee.append(event1);
        TestEvent event2 = new TestEvent(EventId.first(), AGGREGATE_1, "second");
        assertThatThrownBy(() -> testee.append(event2)).isInstanceOf(EventBus.EventStoreFailedException.class);
    }


    @Test
    default void getEventsOfAggregateShouldReturnEmptyHistoryWhenUnknown() {
        EventStore testee = create();
        assertThat(testee.getEventsOfAggregate(AGGREGATE_1)).isEqualTo(EventStore.History.empty());
    }

    @Test
    default void getEventsOfAggregateShouldReturnAppendedEvent() {
        EventStore testee = create();
        TestEvent event = new TestEvent(EventId.first(), AGGREGATE_1, "first");
        testee.append(event);
        assertThat(testee.getEventsOfAggregate(AGGREGATE_1))
            .isEqualTo(EventStore.History.of(EventId.first(), ImmutableList.of(event)));
    }

    @Test
    default void getEventsOfAggregateShouldReturnAppendedEvents() {
        EventStore testee = create();
        TestEvent event1 = new TestEvent(EventId.first(), AGGREGATE_1, "first");
        TestEvent event2 = new TestEvent(event1.eventId().next(), AGGREGATE_1, "second");
        testee.append(event1);
        testee.append(event2);
        assertThat(testee.getEventsOfAggregate(AGGREGATE_1))
            .isEqualTo(EventStore.History.of(event2.eventId(), ImmutableList.of(event1, event2)));
    }

}