package io.github.markwinton.pathfaces;

import java.util.List;
import java.util.Map;

record RewriteResult(
        String rewrittenUrl,
        Map<String, List<String>> parameters
) {
}
