package io.github.markwinton.pathfaces;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@WebFilter(urlPatterns = {"/*"}, asyncSupported = true)
public class RewriteURLFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(RewriteURLFilter.class);
    private RewriteConfig rewriteConfig;

    @Override
    public void init(FilterConfig filterConfig) {
        rewriteConfig = RewrittenURLs.getRewriteConfig();
        if (rewriteConfig != null && !rewriteConfig.rewriteRules().isEmpty()) {
            LOG.info("Pathfaces configuration detected:");
            rewriteConfig.rewriteRules().forEach(rule -> LOG.info(rule.toString()));

            final List<IgnoredPath> ignoredPaths = rewriteConfig.ignoredPaths();
            if (!ignoredPaths.isEmpty()) {
                LOG.info("Pathfaces rewrite will be ignored for the following paths:");
                ignoredPaths.forEach(rule -> LOG.info(rule.toString()));
            }
        }
        else {
            LOG.warn("No Pathfaces rules have been configured");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper((HttpServletRequest) request);
        final int contextPathLength = wrapper.getContextPath().length();
        final String originalPath = wrapper.getRequestURI()
                .substring(contextPathLength);
        final RequestDetails requestDetails = getRequestDetails(originalPath);
        final String rewriteUrl = getRewrittenUrl(rewriteConfig, requestDetails);
        if (rewriteUrl != null && !rewriteUrl.isBlank()) {
            final RequestDispatcher dispatcher = wrapper.getRequestDispatcher(rewriteUrl);
            dispatcher.forward(request, response);
            return;
        }
        chain.doFilter(wrapper, response);
    }

    static RequestDetails getRequestDetails(final String original) {
        final int indexOfParam = original.indexOf('?');
        if (indexOfParam == -1) {
            return new RequestDetails(original, "");
        }
        final String requestUrlWithoutParam = original.substring(0, indexOfParam);
        final String requestParams = original.substring(indexOfParam);
        return new RequestDetails(requestUrlWithoutParam, requestParams);
    }

    static String getRewrittenUrl(
            final RewriteConfig rewriteConfig, final RequestDetails requestDetails
    ) {
        final String requestUrlWithoutParam = requestDetails.requestUrlWithoutParam();
        // Is the path one being ignored by Pathfaces?
        if (rewriteConfig.ignoredPaths().stream()
                .anyMatch(ignoredPath -> ignoredPath.matches(requestUrlWithoutParam))) {
            return null;
        }

        final List<RewriteRule> rewriteRules = rewriteConfig.rewriteRules();
        return getRewrittenUrl(rewriteRules, requestDetails);
    }

    static String getRewrittenUrl(
            final List<RewriteRule> rewriteRules, final RequestDetails requestDetails
    ) {
        final String requestUrlWithoutParam = requestDetails.requestUrlWithoutParam();
        final String requestParams = requestDetails.requestParams();

        // Do any of the base paths exactly match the URL - if so that takes precedence
        for (final RewriteRule rewriteRule : rewriteRules) {
            final String unmodifiedPath = rewriteRule.prettyUrl();
            if (unmodifiedPath.equalsIgnoreCase(requestUrlWithoutParam)) {
                return getRewrittenUrl(rewriteRule, requestUrlWithoutParam, requestParams);
            }
        }

        // Now look for Regex matches - first match in the list takes precedence
        for (RewriteRule rewriteRule : rewriteRules) {
            final String pathRegex = rewriteRule.urlPatternRegex();
            if (requestUrlWithoutParam.matches(pathRegex)) {
                return getRewrittenUrl(rewriteRule, requestUrlWithoutParam, requestParams);
            }
        }

        return null;
    }

    private static String getRewrittenUrl(
            final RewriteRule rewriteRule,
            final String requestUrlWithoutParam, final String requestParams
    ) {
        return rewriteRule.rewrite(requestUrlWithoutParam) + requestParams;
    }

}