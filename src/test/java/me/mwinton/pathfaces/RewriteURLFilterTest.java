package me.mwinton.pathfaces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RewriteURLFilterTest {

    @Test
    void getRewrittenUrlMatch() {
        final List<RewriteRule> rules = List.of(
                RewriteRule.of("/foo", "/a")
        );
        final RequestDetails requestDetails = new RequestDetails("/foo", "");
        assertThat(RewriteURLFilter.getRewrittenUrl(rules, requestDetails))
                .isEqualTo("/a");
    }

    @Test
    void getRewrittenUrlMatchWithQueryParameters() {
        final List<RewriteRule> rules = List.of(
                RewriteRule.of("/foo", "/a")
        );
        final RequestDetails requestDetails = new RequestDetails("/foo", "?bar=qux");
        assertThat(RewriteURLFilter.getRewrittenUrl(rules, requestDetails))
                .isEqualTo("/a?bar=qux");
    }

    @Test
    void getRewrittenUrlMatchWithPathParameters() {
        final List<RewriteRule> rules = List.of(
                RewriteRule.of("/foo/#{bar}", "/a")
        );
        final RequestDetails requestDetails = new RequestDetails("/foo/baz", "");
        assertThat(RewriteURLFilter.getRewrittenUrl(rules, requestDetails))
                .isEqualTo("/a?bar=baz");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/foo/qux", "/bar"})
    void getRewrittenUrlNoMatch(String url) {
        final List<RewriteRule> rules = List.of(
                RewriteRule.of("/foo", "/a")
        );
        final RequestDetails requestDetails = new RequestDetails(url, "");
        assertThat(RewriteURLFilter.getRewrittenUrl(rules, requestDetails))
                .isNull();
    }

    @Test
    void getRewrittenUrlExactMatchPrecedence() {
        final List<RewriteRule> rules = List.of(
                RewriteRule.of("/foo/#{bar}", "/a"),
                RewriteRule.of("/foo/baz", "/b")
        );
        final RequestDetails requestDetails = new RequestDetails("/foo/baz", "");
        assertThat(RewriteURLFilter.getRewrittenUrl(rules, requestDetails))
                .isEqualTo("/b");
    }

    @Test
    void getRewrittenUrlDeclaredOrderPrecedence() {
        final List<RewriteRule> rules = List.of(
                RewriteRule.of("/foo/#{bar}", "/a"),
                RewriteRule.of("/foo/#{bar}qux", "/b")
        );
        final RequestDetails requestDetails = new RequestDetails("/foo/bazqux", "");
        assertThat(RewriteURLFilter.getRewrittenUrl(rules, requestDetails))
                .isEqualTo("/a?bar=bazqux");
    }

    @ParameterizedTest
    @MethodSource("requestDetails")
    void getRequestDetails(
            String originalUrl, String requestUrlWithoutParam, String requestParams
    ) {
        assertThat(RewriteURLFilter.getRequestDetails(originalUrl))
                .returns(requestUrlWithoutParam, RequestDetails::requestUrlWithoutParam)
                .returns(requestParams, RequestDetails::requestParams);
    }

    public static Stream<Arguments> requestDetails() {
        return Stream.of(
                Arguments.of("", "", ""),
                Arguments.of("/", "/", ""),
                Arguments.of("/foo", "/foo", ""),
                Arguments.of("/foo?bar=baz&inky=pinky", "/foo", "?bar=baz&inky=pinky"),
                Arguments.of("/foo?", "/foo", "?")
        );
    }
}