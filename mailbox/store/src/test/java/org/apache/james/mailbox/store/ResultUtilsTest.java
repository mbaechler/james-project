package org.apache.james.mailbox.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.stream.Stream;

import org.apache.james.mailbox.model.MessageResult.FetchGroupEnum;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ResultUtilsTest {

    static Stream<Arguments> detectUnsupportedFetchGroups() {
        return Stream.of(
            Arguments.of(EnumSet.noneOf(FetchGroupEnum.class), EnumSet.noneOf(FetchGroupEnum.class)),
            Arguments.of(EnumSet.allOf(FetchGroupEnum.class), EnumSet.of(FetchGroupEnum.MIME_HEADERS, FetchGroupEnum.MIME_CONTENT)),
            Arguments.of(EnumSet.of(FetchGroupEnum.BODY_CONTENT, FetchGroupEnum.HEADERS), EnumSet.noneOf(FetchGroupEnum.class))
        );
    }

    @ParameterizedTest
    @MethodSource
    void detectUnsupportedFetchGroups(EnumSet<FetchGroupEnum> fetchGroups, EnumSet<FetchGroupEnum> expected) {
        assertThat(ResultUtils.detectUnsupportedFetchGroups(fetchGroups)).isEqualTo(expected);
    }

}