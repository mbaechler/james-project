package org.apache.james.jmap.model;

import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MailboxPropertyTest {

    @Test
    public void findPropertyShouldReturnEmptyWhenNoEnumEntryMatchGivenString() {
        assertThat(MailboxProperty.findProperty("should not match any entry")).isEmpty();
    }

    @Test
    public void findPropertyShouldThrowWhenNullString() {
        assertThatThrownBy(() -> MailboxProperty.findProperty(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void findPropertyShouldReturnMatchingEnumEntryWhenExistingValue() {
        assertThat(MailboxProperty.findProperty("name")).isEqualTo(Optional.of(MailboxProperty.NAME));
    }

}