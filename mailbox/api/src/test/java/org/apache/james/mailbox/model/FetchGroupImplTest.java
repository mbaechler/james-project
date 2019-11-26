package org.apache.james.mailbox.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.stream.Stream;

import org.apache.james.mailbox.model.MessageResult.FetchGroupEnum;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FetchGroupImplTest {

    static Stream<Arguments> unionShouldAddNewValuesInSet() {
        return Stream.of(
            Arguments.arguments(EnumSet.noneOf(FetchGroupEnum.class), EnumSet.noneOf(FetchGroupEnum.class), EnumSet.noneOf(FetchGroupEnum.class)),
            Arguments.arguments(EnumSet.noneOf(FetchGroupEnum.class), EnumSet.allOf(FetchGroupEnum.class), EnumSet.allOf(FetchGroupEnum.class)),
            Arguments.arguments(EnumSet.allOf(FetchGroupEnum.class), EnumSet.noneOf(FetchGroupEnum.class), EnumSet.allOf(FetchGroupEnum.class)),
            Arguments.arguments(EnumSet.noneOf(FetchGroupEnum.class), EnumSet.of(FetchGroupEnum.BODY_CONTENT, FetchGroupEnum.FULL_CONTENT), EnumSet.of(FetchGroupEnum.BODY_CONTENT, FetchGroupEnum.FULL_CONTENT)),
            Arguments.arguments(EnumSet.of(FetchGroupEnum.BODY_CONTENT), EnumSet.of(FetchGroupEnum.FULL_CONTENT), EnumSet.of(FetchGroupEnum.BODY_CONTENT, FetchGroupEnum.FULL_CONTENT)),
            Arguments.arguments(EnumSet.of(FetchGroupEnum.BODY_CONTENT), EnumSet.of(FetchGroupEnum.BODY_CONTENT), EnumSet.of(FetchGroupEnum.BODY_CONTENT)),
            Arguments.arguments(EnumSet.of(FetchGroupEnum.BODY_CONTENT, FetchGroupEnum.FULL_CONTENT), EnumSet.of(FetchGroupEnum.BODY_CONTENT, FetchGroupEnum.FULL_CONTENT), EnumSet.of(FetchGroupEnum.BODY_CONTENT, FetchGroupEnum.FULL_CONTENT)),
            Arguments.arguments(EnumSet.of(FetchGroupEnum.BODY_CONTENT, FetchGroupEnum.FULL_CONTENT), EnumSet.of(FetchGroupEnum.BODY_CONTENT), EnumSet.of(FetchGroupEnum.BODY_CONTENT, FetchGroupEnum.FULL_CONTENT))
        );
    }

    @ParameterizedTest
    @MethodSource
    void unionShouldAddNewValuesInSet(EnumSet<FetchGroupEnum> initial,
                                      EnumSet<FetchGroupEnum> addition,
                                      EnumSet<FetchGroupEnum> expected) {
        FetchGroupImpl fetchGroup = new FetchGroupImpl(initial);
        fetchGroup.union(addition);
        assertThat(fetchGroup.content()).isEqualTo(expected);
    }

}