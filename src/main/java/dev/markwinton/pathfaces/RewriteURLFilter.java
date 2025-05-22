package dev.markwinton.pathfaces;

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

// TODO class needs a refactor
@WebFilter(urlPatterns = {"/*"}, asyncSupported = true)
public class RewriteURLFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RewriteURLFilter.class);
    private List<RewriteRule> rewriteRules;

    @Override
    public void init(FilterConfig filterConfig) {
        LOGGER.info("Initializing the RewriteURLFilter class");
        rewriteRules = RewrittenURLs.getInstance().getRewriteRules();

        if (rewriteRules != null && !rewriteRules.isEmpty()) {
            LOGGER.info("URLs to be rewritten have been detected");
            rewriteRules.forEach(rule -> LOGGER.info(rule.toString()));
        }
        else {
            LOGGER.info("No URLs to be rewritten have been detected");
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("Destroying the RewriteURLFilter class");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper((HttpServletRequest) request);
        final String original = wrapper.getRequestURI().substring(wrapper.getContextPath().length());

        final RequestDetails requestDetails = getRequestDetails(original);
        final String rewriteUrl = getRewrittenUrl(rewriteRules, requestDetails);
        if (rewriteUrl != null && !rewriteUrl.isBlank()) {
            final RequestDispatcher dispatcher = wrapper.getRequestDispatcher(rewriteUrl);
            dispatcher.forward(request, response);
            return;
        }
        chain.doFilter(wrapper, response);
    }

    static RequestDetails getRequestDetails(final String original) {
        final String requestUrlWithoutParam;
        final String requestParams;

        int indexOfParam = original.indexOf('?');
        if (indexOfParam != -1) {
            requestUrlWithoutParam = original.substring(0, indexOfParam);
            requestParams = original.substring(indexOfParam);
        }
        else {
            requestUrlWithoutParam = original;
            requestParams = "";
        }
        return new RequestDetails(requestUrlWithoutParam, requestParams);
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