package io.github.markwinton.pathfaces;

import java.util.List;

public record RewriteConfig(
        List<RewriteRule> rewriteRules,
        List<IgnoredPath> ignoredPaths
) {
    static RewriteConfig empty() {
        return new RewriteConfig(List.of(), List.of());
    }
}
