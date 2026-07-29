package nbbrd.io.http.urlconnection;

import lombok.NonNull;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpClientFactory;
import nbbrd.io.http.HttpContext;
import nbbrd.io.http.ext.AuthenticatingDecorator;
import nbbrd.io.http.ext.RedirectDecorator;
import nbbrd.io.http.ext.RetryDecorator;
import nbbrd.service.ServiceProvider;

/**
 * {@link HttpClientFactory} backed by {@link UrlConnectionHttpClient}.
 * <p>
 * This factory is always available since it relies solely on the JDK
 * {@link java.net.HttpURLConnection}. Redirect, authentication and retry
 * concerns are delegated to the corresponding decorators.
 * </p>
 */
@ServiceProvider(HttpClientFactory.class)
public final class UrlConnectionHttpClientFactory implements HttpClientFactory {

    private static final int PRIORITY = 50;

    @Override
    public @NonNull String getFactoryId() {
        return "urlconnection";
    }

    @Override
    public boolean isFactoryAvailable() {
        return true;
    }

    @Override
    public int getFactoryPriority() {
        return PRIORITY;
    }

    @Override
    public @NonNull HttpClient getClient(@NonNull HttpContext context) {
        HttpClient client = UrlConnectionHttpClient
                .builder()
                .readTimeout(context.getReadTimeout())
                .connectTimeout(context.getConnectTimeout())
                .proxySelector(context.getProxySelector().get())
                .sslSocketFactory(context.getSslSocketFactory().get())
                .hostnameVerifier(context.getHostnameVerifier().get())
                .userAgent(context.getUserAgent())
                .build();
        client = new AuthenticatingDecorator(client, context.getAuthenticator(), context.getAuthScheme(), context.getListener());
        client = new RedirectDecorator(client, context.getMaxRedirects(), context.getListener());
        client = new RetryDecorator(client, context.getMaxRetries(), context.getListener());
        return client;
    }
}

