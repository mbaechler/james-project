package org.apache.james.transport.matchers.dlp;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.regex.Pattern;

import org.apache.james.dlp.api.DLPConfigurationItem.Id;
import org.junit.jupiter.api.Test;

class DlpDomainRulesTest {

    private static final Pattern PATTERN_1 = Pattern.compile("1");
    private static final Pattern PATTERN_2 = Pattern.compile("2");

    @Test
    void builderShouldThrowWhenDuplicateIds() {
        assertThatThrownBy(() -> DlpDomainRules.builder()
                .senderRule(Id.of("1"), PATTERN_1)
                .senderRule(Id.of("1"), PATTERN_2)
                .build())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void builderShouldNotThrowWhenDuplicateIdsOnDifferentTypes() {
        assertThatCode(() -> DlpDomainRules.builder()
                .senderRule(Id.of("1"), PATTERN_1)
                .contentRule(Id.of("1"), PATTERN_2)
                .build())
            .doesNotThrowAnyException();
    }


    @Test
    void builderShouldNotThrowWhenEmpty() {
        assertThatCode(() -> DlpDomainRules.builder().build())
            .doesNotThrowAnyException();
    }

}