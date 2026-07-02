package internal.io.http.ext;

import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.http.HttpHeaders;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Parsed representation of the {@code Cache-Control} header directives relevant to a
 * private cache (RFC 9111). Shared-cache directives ({@code s-maxage}, {@code proxy-revalidate})
 * are intentionally ignored.
 */
@lombok.Value
@lombok.Builder
public class CacheControl {

    public static final String CACHE_CONTROL_HEADER = "Cache-Control";

    boolean noStore;
    boolean noCache;
    boolean isPrivate;

    @Nullable
    Long maxAge;

    @Nullable
    Long staleWhileRevalidate;

    @StaticFactoryMethod
    public static @NonNull CacheControl parse(@NonNull HttpHeaders headers) {
        Builder result = builder();
        for (String value : headers.allValues(CACHE_CONTROL_HEADER)) {
            for (String token : value.split(",")) {
                parseDirective(result, token.trim());
            }
        }
        return result.build();
    }

    private static void parseDirective(Builder result, String directive) {
        if (directive.isEmpty()) {
            return;
        }
        int eq = directive.indexOf('=');
        String name = (eq == -1 ? directive : directive.substring(0, eq)).trim().toLowerCase(Locale.ROOT);
        String argument = eq == -1 ? null : unquote(directive.substring(eq + 1).trim());
        switch (name) {
            case "no-store":
                result.noStore(true);
                break;
            case "no-cache":
                result.noCache(true);
                break;
            case "private":
                result.isPrivate(true);
                break;
            case "max-age":
                result.maxAge(parseSeconds(argument));
                break;
            case "stale-while-revalidate":
                result.staleWhileRevalidate(parseSeconds(argument));
                break;
            default:
                // Ignore unknown and shared-cache directives (s-maxage, proxy-revalidate, ...)
                break;
        }
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static @Nullable Long parseSeconds(@Nullable String argument) {
        if (argument == null) {
            return null;
        }
        try {
            long value = Long.parseLong(argument);
            return value < 0 ? null : value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static class Builder {
        // Fix Javadoc error
    }
}
