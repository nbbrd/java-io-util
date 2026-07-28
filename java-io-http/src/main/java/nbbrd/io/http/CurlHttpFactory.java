package nbbrd.io.http;

import lombok.NonNull;
import nbbrd.design.VisibleForTesting;
import nbbrd.io.http.ext.*;
import nbbrd.service.ServiceProvider;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

/**
 * {@link HttpFactory} backed by {@link CurlHttpClient}.
 * <p>
 * This factory is only available when both the optional {@code java-io-curl}
 * module and the {@code curl} command-line tool are present. Redirect,
 * authentication and retry concerns are delegated to the corresponding
 * decorators.
 * </p>
 */
@ServiceProvider(HttpFactory.class)
public final class CurlHttpFactory implements HttpFactory {

    private static final int PRIORITY = 10;

    @Override
    public @NonNull String getFactoryId() {
        return "curl";
    }

    @Override
    public boolean isFactoryAvailable() {
        return isCurlModulePresent() && isCurlBinaryPresent();
    }

    @Override
    public int getFactoryPriority() {
        return PRIORITY;
    }

    @Override
    public @NonNull HttpClient getClient(@NonNull HttpContext context) {
        HttpClient client = CurlHttpClient
                .builder()
                .readTimeout(context.getReadTimeout())
                .connectTimeout(context.getConnectTimeout())
                .proxySelector(context.getProxySelector().get())
                .userAgent(context.getUserAgent())
                .followRedirects(false)
                .build();
        client = new AuthenticatingDecorator(client, context.getAuthenticator(), context.getAuthScheme(), AuthenticatingListener.noOp());
        client = new RedirectDecorator(client, context.getMaxRedirects(), RedirectListener.noOp());
        client = new RetryDecorator(client, context.getMaxRetries(), RetryListener.noOp());
        return client;
    }

    private static boolean isCurlModulePresent() {
        try {
            return nbbrd.io.curl.Curl.class.getClassLoader() != null;
        } catch (NoClassDefFoundError ex) {
            return false;
        }
    }

    @VisibleForTesting
    static boolean isCurlBinaryPresent() {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String dir : path.split(File.pathSeparator)) {
            try {
                if (Files.isExecutable(Paths.get(dir, "curl"))
                        || Files.isExecutable(Paths.get(dir, "curl.exe"))) {
                    return true;
                }
            } catch (InvalidPathException ignore) {
            }
        }
        return false;
    }
}



