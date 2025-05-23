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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Singleton class to load and manage URL rewrite rules from an XML configuration file.
 */
public final class RewrittenURLs {

    private static final Logger LOG = LoggerFactory.getLogger(RewrittenURLs.class);
    private static final RewrittenURLs INSTANCE = new RewrittenURLs();
    private static final List<RewriteRule> REWRITE_RULES = new ArrayList<>();

    private RewrittenURLs() {
    }

    public static RewrittenURLs getInstance() {
        return INSTANCE;
    }

    public List<RewriteRule> getRewriteRules() {
        if (REWRITE_RULES.isEmpty()) {
            REWRITE_RULES.addAll(loadRewriteRules());
        }
        return REWRITE_RULES;
    }

    private synchronized List<RewriteRule> loadRewriteRules() {
        if (!REWRITE_RULES.isEmpty()) {
            return REWRITE_RULES;
        }
        try {
            final NodeList nodeList = getUrlMappingNodes()
                    .orElseThrow(() -> new IOException("Unable to load rewrite-url.xml"));
            return IntStream.range(0, nodeList.getLength())
                    .boxed()
                    .map(nodeList::item)
                    .filter(n -> n.getNodeType() == Node.ELEMENT_NODE)
                    .map(n -> getRewriteRules((Element) n))
                    .toList();
        }
        catch (ParserConfigurationException | IOException | SAXException e) {
            LOG.error("Pathfaces configuration error: %s".formatted(e.getMessage()), e);
        }
        return List.of();
    }

    private static RewriteRule getRewriteRules(final Element node) {
        final String id = node.getAttribute("id");
        final String pattern = getElementValue(node, "pattern");
        final String viewId = getElementValue(node, "view-id");

        if (!pattern.isBlank() && !viewId.isBlank()) {
            final RewriteRule rewriteRule = RewriteRule.of(pattern, viewId);
            LOG.debug("Adding URL mapping, id {}, pattern {} -> view-id {} ", id, pattern, viewId);
            return rewriteRule;
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

    private static Optional<NodeList> getUrlMappingNodes() throws ParserConfigurationException, SAXException, IOException {
        final InputStream input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("/META-INF/rewrite-url.xml");
        if (input == null) {
            return Optional.empty();
        }
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        final Document doc = dBuilder.parse(input);
        return Optional.of(doc.getElementsByTagName("url-mapping"));
    }
}