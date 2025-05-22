package dev.markwinton.pathfaces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record RewriteRule(
        String prettyUrl,
        String targetPath,
        String urlPatternRegex
) {

    private static final Logger LOG = LoggerFactory.getLogger(RewriteRule.class);
    private static final String PLACEHOLDER_REGEX = "#\\{(.*?)}";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(PLACEHOLDER_REGEX);

    public static RewriteRule of(final String unmodifiedPath, final String targetPath) {
        final String prettyUrlPatternRegex = unmodifiedPath.replaceAll(PLACEHOLDER_REGEX, "([^/]+)");
        return new RewriteRule(unmodifiedPath, targetPath, prettyUrlPatternRegex);
    }

    public String basePath() {
        final String pathWithoutPlaceholdersAndParams = pathWithoutPlaceholdersAndParams();
        if (pathWithoutPlaceholdersAndParams.isEmpty()) {
            return "/";
        }
        if (pathWithoutPlaceholdersAndParams.endsWith("/") && pathWithoutPlaceholdersAndParams.length() > 1) {
            return pathWithoutPlaceholdersAndParams.substring(0, pathWithoutPlaceholdersAndParams.length() - 1);
        }
        return pathWithoutPlaceholdersAndParams;
    }

    private String pathWithoutPlaceholdersAndParams() {
        final int hashPosition = prettyUrl.indexOf('#');
        final int queryPosition = prettyUrl.indexOf('?');
        if (hashPosition > 0 && queryPosition > 0) {
            return prettyUrl.substring(0, Math.min(hashPosition, queryPosition));
        }
        else if (hashPosition > 0) {
            return prettyUrl.substring(0, hashPosition);
        }
        else if (queryPosition > 0) {
            return prettyUrl.substring(0, queryPosition);
        }
        return prettyUrl;
    }

    public String rewrite(String requestUrl) {
        // Build a regex to match the request URL
        final String urlPatternRegex = urlPatternRegex();
        final Pattern urlPattern = Pattern.compile(urlPatternRegex);
        final Matcher urlMatcher = urlPattern.matcher(requestUrl);
        if (!urlMatcher.matches()) {
            return targetPath;
        }
        // Extract placeholders from the pattern
        final Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(prettyUrl);
        // Extract placeholder values from the request URL
        final Map<String, String> redirectParams = getRedirectUrlParameters(placeholderMatcher, urlMatcher);
        return createRedirectUrlForRequest(redirectParams);
    }

    private static Map<String, String> getRedirectUrlParameters(final Matcher placeholderMatcher, final Matcher urlMatcher) {
        final Map<String, String> placeholderValues = new LinkedHashMap<>();
        int groupIndex = 1;
        while (placeholderMatcher.find()) {
            final String placeholder = placeholderMatcher.group(1);
            final String value = urlMatcher.group(groupIndex++);
            placeholderValues.put(placeholder, value);
        }
        return placeholderValues;
    }

    private String createRedirectUrlForRequest(final Map<String, String> placeholderValues) {
        // Build the rewritten URL
        final StringBuilder rewrittenUrl = new StringBuilder(targetPath);
        if (!placeholderValues.isEmpty()) {
            rewrittenUrl.append("?");
            placeholderValues.forEach((key, value) ->
                    rewrittenUrl.append(key).append("=").append(value).append("&")
            );
            // Remove the final trailing '&'
            rewrittenUrl.setLength(rewrittenUrl.length() - 1);
        }
        return rewrittenUrl.toString();
    }

    RewriteResult insertPathParams(Map<String, List<String>> paramsIn) {
        // params are going to be modified by this method, so we need to copy them
        final Map<String, List<String>> params = new LinkedHashMap<>();
        if (paramsIn != null) {
            params.putAll(paramsIn);
        }
        final StringBuilder result = new StringBuilder();
        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(prettyUrl);
        while (matcher.find()) {
            final String placeholder = matcher.group(1);
            final String replacement = getReplacementValue(params, placeholder);
            params.remove(placeholder);
            // TODO - does this need to encode?
            final String encodedPathParam = URLEncoder.encode(replacement, StandardCharsets.UTF_8);
            final String paramReplacement = Matcher.quoteReplacement(encodedPathParam);
            matcher.appendReplacement(result, paramReplacement);
        }
        matcher.appendTail(result);
        final String rewrittenUrl = result.toString();
        return new RewriteResult(rewrittenUrl, params);
    }

    private static String getReplacementValue(
            final Map<String, List<String>> params, final String placeholder
    ) {
        final List<String> replacementParamValues = params.get(placeholder);
        if (replacementParamValues == null) {
            LOG.warn("No replacement value found for placeholder {}", placeholder);
            return "";
        }
        if (replacementParamValues.size() > 1) {
            LOG.warn("Found multiple parameters for placeholder {}, will use first value from {}",
                    placeholder, replacementParamValues);
        }
        return replacementParamValues.get(0);
    }
}
