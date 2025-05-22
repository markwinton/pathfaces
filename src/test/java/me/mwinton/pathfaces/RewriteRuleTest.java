package me.mwinton.pathfaces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RewriteRuleTest {

    public static Stream<Arguments> basePathProvider() {
        return Stream.of(
                Arguments.of(RewriteRule.of("", "/something"), "/"),
                Arguments.of(RewriteRule.of("/", "/anything"), "/"),
                Arguments.of(RewriteRule.of("/foo", "/something"), "/foo"),
                Arguments.of(RewriteRule.of("/foo/", "/something"), "/foo"),
                Arguments.of(RewriteRule.of("/foo/bar#{baz}", "/something"), "/foo/bar"),
                Arguments.of(RewriteRule.of("/foo/bar?baz=qux", "/something"), "/foo/bar"),
                Arguments.of(RewriteRule.of("/foo/bar#baz", "/something"), "/foo/bar"),
                Arguments.of(RewriteRule.of("/foo/bar/#{baz}?qux", "/something"), "/foo/bar"),
                Arguments.of(RewriteRule.of("/foo/bar-#{baz}/qux", "/something"), "/foo/bar-"),
                Arguments.of(RewriteRule.of("/foo/bar-#{baz}/qux", "/something"), "/foo/bar-"),
                Arguments.of(RewriteRule.of("/foo/bar?baz=qux&x=#{baz}", "/something"), "/foo/bar"),
                Arguments.of(RewriteRule.of("/foo/#{bar}/#{baz}", "/something"), "/foo"),
                Arguments.of(RewriteRule.of("/#{foo}/bar", "/something"), "/")
        );
    }

    @ParameterizedTest
    @MethodSource("basePathProvider")
    void basePath(RewriteRule rule, String expectedBasePath) {
        assertThat(rule)
                .returns(expectedBasePath, RewriteRule::basePath);
    }

    public static Stream<Arguments> rewriteProvider() {
        return Stream.of(
                Arguments.of(RewriteRule.of("", "/something"), "/", "/something"),
                Arguments.of(RewriteRule.of("/", "/anything"), "/", "/anything"),
                Arguments.of(RewriteRule.of("/foo", "/something"), "/foo", "/something"),
                Arguments.of(RewriteRule.of("/foo/", "/something"), "/foo/", "/something"),
                Arguments.of(RewriteRule.of("/foo", "/something"), "/foo/", "/something"),
                Arguments.of(RewriteRule.of("/foo/", "/something"), "/foo", "/something"),
                Arguments.of(RewriteRule.of("/foo/#{bar}", "/x"), "/foo/qux", "/x?bar=qux"),
                Arguments.of(RewriteRule.of("/foo/bar#{xy}baz/#{z}-#{w}/#{v}/qux", "/inky/pinky"), "/foo/bara42baz/blinky-clyde/pacman/qux", "/inky/pinky?xy=a42&z=blinky&w=clyde&v=pacman"),
                Arguments.of(RewriteRule.of("/a/#{a}/a", "/b"), "/a/a/a", "/b?a=a"),
                Arguments.of(RewriteRule.of("/a/#{a}/a", "/b"), "/a/c/a", "/b?a=c")
        );
    }

    @ParameterizedTest
    @MethodSource("rewriteProvider")
    void basePath(RewriteRule rule, String requestUrl, String expectedRewrite) {
        assertThat(rule.rewrite(requestUrl))
                .isEqualTo(expectedRewrite);
    }

    @Test
    void insertPathParams() {
        final RewriteRule rule = RewriteRule.of("/foo/#{x}/#{bar}/bar", "index");
        final Map<String, List<String>> params = Map.of(
                "bar", List.of("baz"),
                "x", List.of("y")
        );
        assertThat(rule.insertPathParams(params))
                .returns("/foo/y/baz/bar", RewriteResult::rewrittenUrl)
                .returns(Map.of(), RewriteResult::parameters);
    }

    @Test
    void insertPathParamsMultipleValues() {
        final RewriteRule rule = RewriteRule.of("/foo/#{bar}", "index");
        final Map<String, List<String>> params = Map.of(
                "bar", List.of("qux", "baz")
        );
        assertThat(rule.insertPathParams(params))
                .returns("/foo/qux", RewriteResult::rewrittenUrl)
                .returns(Map.of(), RewriteResult::parameters);
    }

    @Test
    void insertPathParamsAdditionalParameters() {
        final RewriteRule rule = RewriteRule.of("/foo/#{bar}", "index");
        final Map<String, List<String>> params = Map.of(
                "bar", List.of("baz"),
                "inky", List.of("pinky"),
                "blinky", List.of("clyde", "pacman")
        );
        assertThat(rule.insertPathParams(params))
                .returns("/foo/baz", RewriteResult::rewrittenUrl)
                .returns(Map.of(
                        "inky", List.of("pinky"),
                        "blinky", List.of("clyde", "pacman")
                ), RewriteResult::parameters);
    }

    @Test
    void insertPathParamsNoMatchingParameter() {
        final RewriteRule rule = RewriteRule.of("/foo/#{bar}/baz", "index");
        final Map<String, List<String>> params = Map.of(
                "x", List.of("y")
        );
        assertThat(rule.insertPathParams(params))
                .returns("/foo//baz", RewriteResult::rewrittenUrl)
                .returns(params, RewriteResult::parameters);
    }

    @Test
    void insertPathParamsNullParams() {
        final RewriteRule rule = RewriteRule.of("/foo/#{bar}/baz", "index");
        assertThat(rule.insertPathParams(null))
                .returns("/foo//baz", RewriteResult::rewrittenUrl)
                .returns(Map.of(), RewriteResult::parameters);
    }
}