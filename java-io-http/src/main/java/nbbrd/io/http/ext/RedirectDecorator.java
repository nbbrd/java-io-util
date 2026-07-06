package nbbrd.io.http.ext;

import internal.io.http.UrlHelper;
import lombok.NonNull;
import nbbrd.design.DecoratorPattern;
import nbbrd.design.NonNegative;
import nbbrd.design.VisibleForTesting;
import nbbrd.io.http.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link HttpClient} decorator that follows HTTP redirects (3xx responses with
 * a {@code Location} header).
 * <p>
 * Only genuine redirect status codes (301, 302, 303, 307, 308) are followed.
 * Other 3xx codes (e.g. 300 Multiple Choices, 304 Not Modified) are returned
 * as regular responses so that callers (e.g. a caching layer that needs 304
 * for conditional revalidation) can handle them.
 * </p>
 * <p>
 * Protocol downgrades (HTTPS → HTTP) on redirect are rejected with an
 * {@link IOException}.
 * </p>
 */
@DecoratorPattern(HttpClient.class)
@lombok.AllArgsConstructor
public final class RedirectDecorator implements HttpClientDecorator {

    // RFC 7231/7538: 3xx codes that carry a Location header to follow.
    private static final Set<Integer> FOLLOWED_REDIRECT_CODES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    HttpURLConnection.HTTP_MOVED_PERM,  // 301
                    HttpURLConnection.HTTP_MOVED_TEMP,  // 302
                    HttpURLConnection.HTTP_SEE_OTHER,   // 303
                    307,                                // Temporary Redirect (no JDK constant)
                    308                                 // Permanent Redirect (no JDK constant)
            )));

    @lombok.Getter
    @NonNull
    private final HttpClient decorated;

    @NonNegative
    private final int maxRedirects;

    @NonNull
    private final RedirectListener listener;

    @Override
    public @NonNull String getDescription() {
        return "Redirecting (" + maxRedirects + ") on " + decorated.getDescription();
    }

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        return send(request, 0);
    }

    private HttpResponse send(HttpRequest request, int redirects) throws IOException {
        HttpResponse response = decorated.send(request);
        int code = response.getStatusCode();
        if (FOLLOWED_REDIRECT_CODES.contains(code)) {
            return redirect(response, request, redirects);
        }
        return response;
    }

    private HttpResponse redirect(HttpResponse response, HttpRequest request, int redirects) throws IOException {
        URI oldUri = request.getQuery();
        URI newUri;
        try {
            if (redirects == maxRedirects) {
                throw new IOException("Max redirection reached");
            }

            String location = response.getHeaders()
                    .firstValue(HttpHeaders.HTTP_LOCATION_HEADER)
                    .orElse(null);
            if (location == null || location.isEmpty()) {
                throw new IOException("Missing redirection url");
            }

            // RFC 7231: Location is a URI-reference; resolve it against the request URI.
            newUri = oldUri.resolve(location);
        } finally {
            response.close();
        }

        if (isDowngradingProtocolOnRedirect(oldUri, newUri)) {
            throw new IOException("Downgrading protocol on redirect from '" + oldUri + "' to '" + newUri + "'");
        }

        listener.onRedirection(oldUri, newUri);

        return send(request.toBuilder().query(newUri).build(), redirects + 1);
    }

    // https://en.wikipedia.org/wiki/Downgrade_attack
    @VisibleForTesting
    static boolean isDowngradingProtocolOnRedirect(URI oldUri, URI newUri) {
        return UrlHelper.isHttpsProtocol(oldUri) && !UrlHelper.isHttpsProtocol(newUri);
    }
}
