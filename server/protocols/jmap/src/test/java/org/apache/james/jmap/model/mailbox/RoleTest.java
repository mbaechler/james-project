package org.apache.james.jmap.model.mailbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.jmap.model.mailbox.Role;
import org.junit.Test;

import java.util.Locale;
import java.util.Optional;

public class RoleTest {

    @Test
    public void fromShouldReturnInbox() {
        assertThat(Role.from("inbox")).isEqualTo(Optional.of(Role.INBOX));
    }

    @Test
    public void fromShouldReturnEmptyWhenUnknownValue() {
        assertThat(Role.from("jjjj")).isEqualTo(Optional.empty());
    }

    @Test
    public void fromShouldReturnInboxWhenContainsUppercaseValue() {
        assertThat(Role.from("InBox")).isEqualTo(Optional.of(Role.INBOX));
    }

    @Test
    public void fromShouldReturnInboxWhenContainsUppercaseValueInTurkish() {
        Locale previousLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr"));
        try {
            assertThat(Role.from("InBox")).isEqualTo(Optional.of(Role.INBOX));
        } finally {
            Locale.setDefault(previousLocale);
        }
    }

}