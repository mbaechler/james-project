package org.apache.james.queue.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class MailQueueNameTest {

    @Test
    void fromStringShouldThrowOnNull() {
        assertThatThrownBy(() -> MailQueueName.fromString(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fromStringShouldReturnInstanceOnEmptyString() {
        assertThat(MailQueueName.fromString("")).isNotNull();
    }

    @Test
    void fromStringShouldReturnInstanceOnArbitraryString() {
        assertThat(MailQueueName.fromString("whatever")).isNotNull();
    }


    @Test
    void fromRabbitWorkQueueNameShouldReturnEmptyOnArbitraryString() {
        assertThat(MailQueueName.fromRabbitWorkQueueName("whatever"))
            .isEmpty();
    }

    @Test
    void fromRabbitWorkQueueNameShouldReturnInstanceOnPrefixOnlyString() {
        assertThat(MailQueueName.fromRabbitWorkQueueName(MailQueueName.WORKQUEUE_PREFIX))
            .contains(MailQueueName.fromString(""));
    }

    @Test
    void fromRabbitWorkQueueNameShouldReturnInstanceOnValidQueueName() {
        assertThat(MailQueueName.fromRabbitWorkQueueName(MailQueueName.WORKQUEUE_PREFIX + "myQueue"))
            .contains(MailQueueName.fromString("myQueue"));
    }

    @Test
    void shouldConformToBeanContract() {
        EqualsVerifier.forClass(MailQueueName.class).verify();
    }

    @Test
    void fromRabbitWorkQueueNameShouldReturnIdentityOnToRabbitWorkQueueName() {
        MailQueueName myQueue = MailQueueName.fromString("myQueue");
        assertThat(MailQueueName.fromRabbitWorkQueueName(myQueue.toRabbitWorkQueueName()))
            .contains(myQueue);

    }

}