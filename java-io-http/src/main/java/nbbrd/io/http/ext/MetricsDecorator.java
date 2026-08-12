package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.DecoratorPattern;
import nbbrd.io.http.*;
import nbbrd.io.net.MediaType;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link HttpClient} decorator that collects per-request metrics
 * (timing, byte count, status code) and reports them via a {@link MetricsListener}
 * when the response is closed.
 */
@DecoratorPattern(HttpClient.class)
@lombok.AllArgsConstructor
public final class MetricsDecorator implements HttpClientDecorator {

    @lombok.Getter
    @NonNull
    private final HttpClient decorated;

    @NonNull
    private final MetricsListener listener;

    @Override
    public @NonNull String getDescription() {
        return "Metrics on " + decorated.getDescription();
    }

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        long startNanos = System.nanoTime();
        HttpResponse response = decorated.send(request);
        long networkNanos = System.nanoTime() - startNanos;
        return new MetricsResponse(response, request, networkNanos, startNanos, listener);
    }

    @lombok.RequiredArgsConstructor
    private static final class MetricsResponse implements HttpResponse {

        @NonNull
        private final HttpResponse delegate;

        @NonNull
        private final HttpRequest request;

        private final long networkNanos;

        private final long startNanos;

        @NonNull
        private final MetricsListener listener;

        private final AtomicLong byteCount = new AtomicLong();

        @Override
        public @NonNull MediaType getContentType() throws IOException {
            return delegate.getContentType();
        }

        @Override
        public long getContentLength() throws IOException {
            return delegate.getContentLength();
        }

        @Override
        public @NonNull HttpHeaders getHeaders() throws IOException {
            return delegate.getHeaders();
        }

        @Override
        public int getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public @NonNull InputStream getBody() throws IOException {
            return new CountingInputStream(delegate.getBody(), byteCount);
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
            } finally {
                long totalNanos = System.nanoTime() - startNanos;
                listener.onCompleted(MetricsEvent
                        .builder()
                        .requestUri(request.getQuery())
                        .requestMethod(request.getMethod())
                        .responseStatusCode(resolveStatusCode())
                        .responseContentLength(resolveContentLength())
                        .responseBytesRead(byteCount.get())
                        .networkNanos(networkNanos)
                        .totalNanos(totalNanos)
                        .build());
            }
        }

        private int resolveStatusCode() {
            try {
                return delegate.getStatusCode();
            } catch (IOException ex) {
                return HttpResponse.NO_STATUS_CODE;
            }
        }

        private long resolveContentLength() {
            try {
                return delegate.getContentLength();
            } catch (IOException ex) {
                return HttpResponse.NO_CONTENT_LENGTH;
            }
        }
    }

    private static final class CountingInputStream extends FilterInputStream {

        private final AtomicLong counter;

        CountingInputStream(InputStream in, AtomicLong counter) {
            super(in);
            this.counter = counter;
        }

        @Override
        public int read() throws IOException {
            int result = super.read();
            if (result != -1) counter.incrementAndGet();
            return result;
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public int read(byte[] b, int off, int len) throws IOException {
            int result = super.read(b, off, len);
            if (result > 0) counter.addAndGet(result);
            return result;
        }
    }
}
