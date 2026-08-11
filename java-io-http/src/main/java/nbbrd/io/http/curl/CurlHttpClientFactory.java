package nbbrd.io.http.curl;

import lombok.NonNull;
import nbbrd.design.VisibleForTesting;
import nbbrd.io.http.HttpClient;
import nbbrd.io.http.HttpClientFactory;
import nbbrd.io.http.HttpContext;
import nbbrd.service.ServiceProvider;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

/**
 * {@link HttpClientFactory} backed by {@link CurlHttpClient}.
 * <p>
 * This factory is only available when both the optional {@code java-io-curl}
 * module and the {@code curl} command-line tool are present.
 * </p>
 */
@ServiceProvider(HttpClientFactory.class)
public final class CurlHttpClientFactory implements HttpClientFactory {

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
        return CurlHttpClient
                .builder()
                .readTimeout(context.getReadTimeout())
                .connectTimeout(context.getConnectTimeout())
                .proxySelector(context.getProxySelector().get())
                .userAgent(context.getUserAgent())
                .followRedirects(false)
                .normalizeUri(context.isNormalizeUri())
                .build();
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



