package dev.markwinton.pathfaces;

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
import java.util.stream.IntStream;

// TODO class needs a refactor
public class RewrittenURLs {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RewrittenURLs.class);

    private static final RewrittenURLs INSTANCE = new RewrittenURLs();
    private static final List<RewriteRule> REWRITE_RULES = new ArrayList<>();

    private RewrittenURLs() {
    }

    public static RewrittenURLs getInstance() {
        return INSTANCE;
    }

    public List<RewriteRule> getRewriteRules() {
        if (REWRITE_RULES.isEmpty()) {
            REWRITE_RULES.addAll(this.loadRewriteRules());
        }
        return REWRITE_RULES;
    }

    private synchronized List<RewriteRule> loadRewriteRules() {
        if (!REWRITE_RULES.isEmpty()) {
            return REWRITE_RULES;
        }
        try {
            final NodeList nodeList = getUrlMappingNodes();
            return IntStream.range(0, nodeList.getLength())
                    .boxed()
                    .map(nodeList::item)
                    .filter(n -> n.getNodeType() == Node.ELEMENT_NODE)
                    .map(n -> getRewriteRules((Element) n))
                    .toList();
        }
        catch (ParserConfigurationException | IOException | SAXException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        return List.of();
    }

    private static RewriteRule getRewriteRules(final Element node) {
        final String id = node.getAttribute("id");
        String pattern = null;
        String viewId = null;
        final NodeList patternNodeList = node.getElementsByTagName("pattern");
        if (patternNodeList.getLength() > 0) {
            final Node patternNode = patternNodeList.item(0);
            if (patternNode.getNodeType() == Node.ELEMENT_NODE) {
                final Element patternElement = (Element) patternNode;
                pattern = patternElement.getAttribute("value");
            }
        }

        final NodeList viewIdNodeList = node.getElementsByTagName("view-id");
        if (viewIdNodeList.getLength() > 0) {
            final Node viewIdNode = viewIdNodeList.item(0);
            if (viewIdNode.getNodeType() == Node.ELEMENT_NODE) {
                final Element viewIdElement = (Element) viewIdNode;
                viewId = viewIdElement.getAttribute("value");
            }
        }

        if (pattern != null && viewId != null) {
            final RewriteRule rewriteRule = RewriteRule.of(pattern, viewId);
            LOGGER.info("Registering url mapping id {}: pattern {} -> view-id {} ", id, pattern, viewId);
            return rewriteRule;
        }
        return null;
    }

    private static NodeList getUrlMappingNodes() throws ParserConfigurationException, SAXException, IOException {
        final InputStream input = RewrittenURLs.class.getResourceAsStream("/META-INF/rewrite-url.xml");
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        final Document doc = dBuilder.parse(input);
        return doc.getElementsByTagName("url-mapping");
    }
}