package io.github.markwinton.pathfaces;

public record IgnoredPath(
        String id,
        String path,
        boolean isExactMatch
) {

    public static IgnoredPath of(String id, String path, boolean isExactMatch) {
        return new IgnoredPath(id, path, isExactMatch);
    }

    public static IgnoredPath of(String id, String path) {
        return new IgnoredPath(id, path, false);
    }

    public boolean matches(final String action) {
        if (action == null) {
            return false;
        }
        return isExactMatch
                ? action.equals(path)
                : action.startsWith(path);
    }
}
