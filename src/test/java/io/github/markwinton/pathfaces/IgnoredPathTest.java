package io.github.markwinton.pathfaces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class IgnoredPathTest {

    @Test
    void of() {
        assertThat(IgnoredPath.of("foo", "/bar"))
                .returns(false, IgnoredPath::isExactMatch);
    }

    @ParameterizedTest
    @MethodSource("matchTestArguments")
    void matches(IgnoredPath ignoredPath, String action, boolean expectedMatch) {
        assertThat(ignoredPath.matches(action))
                .isEqualTo(expectedMatch);
    }

    public static Stream<Arguments> matchTestArguments() {
        return Stream.of(
                Arguments.of(IgnoredPath.of("foo", "/bar"), "/bar", true),
                Arguments.of(IgnoredPath.of("foo", "/bar"), "/bar/", true),
                Arguments.of(IgnoredPath.of("foo", "/bar", true), "/bar", true),
                Arguments.of(IgnoredPath.of("foo", "/bar", true), "/bar/", false)
        );
    }

}