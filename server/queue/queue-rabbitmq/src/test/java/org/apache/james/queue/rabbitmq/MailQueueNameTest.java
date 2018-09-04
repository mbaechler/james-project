package org.apache.james.queue.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class MailQueueNameTest {

    @Test
    void fromStringShouldThrowWhenNull() {
        assertThatThrownBy(() -> MailQueueName.fromString(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fromStringShouldReturnInstanceWhenEmptyString() {
        assertThat(MailQueueName.fromString("")).isNotNull();
    }

    @Test
    void fromStringShouldReturnInstanceWhenArbitraryString() {
        assertThat(MailQueueName.fromString("whatever")).isNotNull();
    }


    @Test
    void fromRabbitWorkQueueNameShouldReturnEmptyWhenArbitraryString() {
        assertThat(MailQueueName.fromRabbitWorkQueueName("whatever"))
            .isEmpty();
    }

    @Test
    void fromRabbitWorkQueueNameShouldReturnInstanceWhenPrefixOnlyString() {
        assertThat(MailQueueName.fromRabbitWorkQueueName(MailQueueName.WORKQUEUE_PREFIX))
            .contains(MailQueueName.fromString(""));
    }

    @Test
    void fromRabbitWorkQueueNameShouldReturnInstanceWhenValidQueueName() {
        assertThat(MailQueueName.fromRabbitWorkQueueName(MailQueueName.WORKQUEUE_PREFIX + "myQueue"))
            .contains(MailQueueName.fromString("myQueue"));
    }

    @Test
    void shouldConformToBeanContract() {
        EqualsVerifier.forClass(MailQueueName.class).verify();
    }

    @Test
    void fromRabbitWorkQueueNameShouldReturnIdentityWhenToRabbitWorkQueueName() {
        MailQueueName myQueue = MailQueueName.fromString("myQueue");
        assertThat(MailQueueName.fromRabbitWorkQueueName(myQueue.toRabbitWorkQueueName()))
            .contains(myQueue);

    }

}