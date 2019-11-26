package org.apache.james.mailbox.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.stream.Stream;

import org.apache.james.mailbox.model.MessageResult.FetchGroup;
import org.apache.james.mailbox.model.MessageResult.FetchGroupEnum;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MessageResultTest {

    static Stream<Arguments> factoryMethodShouldReturnRightEnumSet() {
        return Stream.of(
            Arguments.arguments(FetchGroup.MINIMAL, EnumSet.noneOf(FetchGroupEnum.class)),
            Arguments.arguments(FetchGroup.MIME_DESCRIPTOR, EnumSet.of(FetchGroupEnum.MIME_DESCRIPTOR)),
            Arguments.arguments(FetchGroup.HEADERS, EnumSet.of(FetchGroupEnum.HEADERS)),
            Arguments.arguments(FetchGroup.FULL_CONTENT, EnumSet.of(FetchGroupEnum.FULL_CONTENT)),
            Arguments.arguments(FetchGroup.BODY_CONTENT, EnumSet.of(FetchGroupEnum.BODY_CONTENT)),
            Arguments.arguments(FetchGroup.MIME_HEADERS, EnumSet.of(FetchGroupEnum.MIME_HEADERS)),
            Arguments.arguments(FetchGroup.MIME_CONTENT, EnumSet.of(FetchGroupEnum.MIME_CONTENT)),
            Arguments.arguments(FetchGroup.HEADERS | FetchGroup.BODY_CONTENT, EnumSet.of(FetchGroupEnum.HEADERS, FetchGroupEnum.BODY_CONTENT)),
            Arguments.arguments(FetchGroup.HEADERS | FetchGroup.BODY_CONTENT | FetchGroup.MIME_DESCRIPTOR | FetchGroup.FULL_CONTENT | FetchGroup.MIME_HEADERS | FetchGroup.MIME_CONTENT,
                EnumSet.allOf(FetchGroupEnum.class))
        );
    }

    @ParameterizedTest
    @MethodSource
    void factoryMethodShouldReturnRightEnumSet(int bitfield, EnumSet<FetchGroupEnum> expected) {
        assertThat(FetchGroupEnum.of(bitfield)).isEqualTo(expected);
    }

}