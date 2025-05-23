package io.github.markwinton.pathfaces;

import jakarta.faces.application.ViewHandler;
import jakarta.faces.application.ViewHandlerWrapper;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>Rewrites links created by, say, h:link to point to the pretty URL.</p>
 * <p>Needs to be registered via the faces-config.xml file - see the README.md file for details.</p>
 */
public class RewriteViewHandler extends ViewHandlerWrapper {

    private static final List<RewriteRule> REWRITE_RULES = new ArrayList<>();

    public RewriteViewHandler(final ViewHandler wrapped) {
        super(wrapped);
        final List<RewriteRule> rewrittenUrls = RewrittenURLs.getInstance().getRewriteRules();
        if (rewrittenUrls != null) {
            REWRITE_RULES.addAll(rewrittenUrls);
        }
    }

    /**
     * <p>Rewrites the URL to the pretty URL, if a rewrite rule exists.</p>
     * <p>Parameters are inserted into the URL's path and removed from the
     * parameters map in the process; any remaining parameters are appended as a query string.</p>
     */
    @Override
    public String getBookmarkableURL(
            final FacesContext context, final String viewId, final Map<String, List<String>> parameters,
            final boolean includeViewParams
    ) {
        final String actionEncodedViewId = getActionURL(context, viewId);
        final String contextPath = context.getExternalContext().getRequestContextPath();
        final RewriteResult rewrittenUrl = getRewrittenUrl(contextPath, actionEncodedViewId, parameters);

        final ExternalContext externalContext = context.getExternalContext();
        final String bookmarkEncodedURL = externalContext
                .encodeBookmarkableURL(rewrittenUrl.rewrittenUrl(), rewrittenUrl.parameters());
        return externalContext.encodeActionURL(bookmarkEncodedURL);
    }

    private static RewriteResult getRewrittenUrl(
            final String contextPath, final String actionURL,
            final Map<String, List<String>> params
    ) {
        final ActionDetails actionDetails = getActionDetails(actionURL);
        final String action = actionDetails.baseUrl()
                .substring(contextPath.length());

        return REWRITE_RULES.stream()
                .filter(rule -> rule.targetPath().equals(action))
                .findFirst()
                .map(rule -> rule.insertPathParams(params))
                .orElse(new RewriteResult(actionURL, params));
    }

    private static ActionDetails getActionDetails(final String actionURL) {
        int indexOfParam = actionURL.indexOf('?');
        if (indexOfParam == -1) {
            return new ActionDetails(actionURL, "");
        }
        final String baseUrl = actionURL.substring(indexOfParam);
        final String params = actionURL.substring(0, indexOfParam);
        return new ActionDetails(baseUrl, params);
    }

    private record ActionDetails(
            String baseUrl,
            String params
    ) {
    }
}