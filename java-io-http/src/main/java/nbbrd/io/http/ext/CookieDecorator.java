package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.DecoratorPattern;
import nbbrd.io.http.*;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Decorator that adds per-connection HTTP cookie handling.
 * <p>
 * Some sources sit behind an access gateway (e.g. MobilityGuard) that performs a cookie
 * challenge: the first request is answered with a redirect that sets a session cookie and only
 * returns the expected payload once that cookie is sent back. Since the underlying HTTP stack has
 * no cookie support, cookies are dropped across the redirect chain and the client ends up on the
 * challenge page instead of the actual resource.
 * <p>
 * This decorator stores cookies received in responses and replays them on subsequent requests,
 * mimicking the behavior of a web browser. It must be placed <em>inside</em> the redirect
 * decoration so that cookies are carried across redirect hops.
 */
@DecoratorPattern(HttpClient.class)
@lombok.AllArgsConstructor
public final class CookieDecorator implements HttpClientDecorator {

    @lombok.Getter
    @lombok.NonNull
    private final HttpClient decorated;

    @lombok.NonNull
    private final CookieHandler cookieHandler;

    @Override
    public @NonNull String getDescription() {
        return "Cookie " + decorated.getDescription();
    }

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        HttpRequest withCookies = addCookies(request);
        HttpResponse response = decorated.send(withCookies);
        storeCookies(withCookies.getQuery(), response);
        return response;
    }

    private HttpRequest addCookies(HttpRequest request) throws IOException {
        Map<String, List<String>> cookieHeaders = cookieHandler.get(request.getQuery(), request.getHeaders().getMap());
        HttpHeaders.Builder headers = request.getHeaders().toBuilder();
        boolean hasCookies = false;
        for (Map.Entry<String, List<String>> entry : cookieHeaders.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                // All cookies must be sent as a single header value separated by "; ".
                // Adding them as multiple headers would cause the underlying
                // HttpURLConnection#setRequestProperty to overwrite, keeping only one
                // cookie and breaking cookie-challenge gateways (e.g. MobilityGuard).
                headers.put(entry.getKey(), String.join("; ", entry.getValue()));
                hasCookies = true;
            }
        }
        return hasCookies ? request.toBuilder().headers(headers.build()).build() : request;
    }

    private void storeCookies(URI uri, HttpResponse response) throws IOException {
        cookieHandler.put(uri, response.getHeaders().getMap());
    }
}
