package io.github.markwinton.pathfaces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Singleton class to load and manage URL rewrite rules from an XML configuration file.
 */
public final class RewrittenURLs {

    private static final Logger LOG = LoggerFactory.getLogger(RewrittenURLs.class);
    private static RewriteConfig rewriteConfig;

    private RewrittenURLs() {
    }

    public static RewriteConfig getRewriteConfig() {
        if (rewriteConfig == null) {
            rewriteConfig = loadRewriteRules();
        }
        return rewriteConfig;
    }

    private static synchronized RewriteConfig loadRewriteRules() {
        if (rewriteConfig != null) {
            return rewriteConfig;
        }
        try {
            final Document loadedXmlConfig = loadRewriteConfigFile();
            final List<RewriteRule> rewriteRules
                    = parseRewriteRules(loadedXmlConfig, "url-mapping", n -> getRewriteRules((Element) n));
            final List<IgnoredPath> ignoredPaths
                    = parseRewriteRules(loadedXmlConfig, "ignored-path", n -> getIgnoredPaths((Element) n));
            return new RewriteConfig(rewriteRules, ignoredPaths);
        }
        catch (ParserConfigurationException | IOException | SAXException e) {
            LOG.error("Pathfaces configuration error: %s".formatted(e.getMessage()), e);
        }
        return RewriteConfig.empty();
    }

    private static <T> List<T> parseRewriteRules(
            final Document loadedXmlConfig, final String tagName, Function<Node, T> elementParser
    ) {
        final NodeList nodeList = loadedXmlConfig.getElementsByTagName(tagName);
        return IntStream.range(0, nodeList.getLength())
                .boxed()
                .map(nodeList::item)
                .filter(n -> n.getNodeType() == Node.ELEMENT_NODE)
                .map(elementParser)
                .toList();
    }

    private static RewriteRule getRewriteRules(final Element node) {
        final String id = node.getAttribute("id");
        final String pattern = getElementValue(node, "pattern");
        final String viewId = getElementValue(node, "view-id");

        if (!pattern.isBlank() && !viewId.isBlank()) {
            final RewriteRule rewriteRule = RewriteRule.of(id, pattern, viewId);
            LOG.debug("Adding URL mapping, id {}, pattern {} -> view-id {} ", id, pattern, viewId);
            return rewriteRule;
        }
        return null;
    }

    private static IgnoredPath getIgnoredPaths(final Element node) {
        final String id = node.getAttribute("id");
        final String path = getElementValue(node, "path");
        final boolean isExact = Boolean.parseBoolean(getElementValue(node, "is-exact"));
        if (!path.isBlank()) {
            final IgnoredPath ignoredPath = IgnoredPath.of(id, path, isExact);
            LOG.debug("Excluding path from Pathfaces URL rewrites, id {}, path {}, exact matches only {}",
                    id, path, isExact);
            return ignoredPath;
        }
        return null;
    }

    /**
     * Obtain the specified <code>tagName</code> from the given <code>node</code>; returns
     * an empty string if the tag is not found.
     * @param node The node to search; cannot be null.
     * @param tagName The tag name to search for; cannot be null.
     * @return Possibly empty, never null.
     */
    private static String getElementValue(final Element node, final String tagName) {
        final NodeList patternNodeList = node.getElementsByTagName(tagName);
        if (patternNodeList.getLength() > 0) {
            final Node patternNode = patternNodeList.item(0);
            if (patternNode.getNodeType() == Node.ELEMENT_NODE) {
                final Element patternElement = (Element) patternNode;
                return patternElement.getAttribute("value");
            }
        }
        return "";
    }

    private static Document loadRewriteConfigFile()
            throws ParserConfigurationException, SAXException, IOException {
        final InputStream input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("/META-INF/rewrite-url.xml");
        if (input == null) {
            throw new IOException("Unable to load rewrite-url.xml");
        }
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        return dBuilder.parse(input);
    }

}