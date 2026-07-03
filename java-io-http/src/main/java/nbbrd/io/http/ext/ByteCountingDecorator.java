package nbbrd.io.http.ext;

import lombok.NonNull;
import nbbrd.design.DecoratorPattern;
import nbbrd.design.MightBePromoted;
import nbbrd.io.http.*;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

@DecoratorPattern(HttpClient.class)
@lombok.AllArgsConstructor
public final class ByteCountingDecorator implements HttpClientDecorator {

    /**
     * The underlying HTTP client to delegate requests to.
     */
    @lombok.Getter
    @NonNull
    private final HttpClient decorated;

    /**
     * Listener that receives byte count messages.
     */
    @NonNull
    private final LongConsumer listener;

    @Override
    public @NonNull String getDescription() {
        return "Byte counting " + decorated.getDescription();
    }

    @Override
    public @NonNull HttpResponse send(@NonNull HttpRequest request) throws IOException {
        return new ByteCountingResponse(decorated.send(request), listener);
    }

    /**
     * HTTP response wrapper that tracks the number of bytes read from the response body.
     * <p>
     * Monitors all bytes read through input stream access and reports the total count
     * via the listener when the response is closed.
     * </p>
     */
    @MightBePromoted
    @lombok.AllArgsConstructor
    private static final class ByteCountingResponse implements HttpResponse {

        /**
         * The underlying HTTP response to delegate to.
         */
        @NonNull
        private final HttpResponse delegate;

        /**
         * Listener that receives byte count messages.
         */
        @NonNull
        private final LongConsumer listener;

        /**
         * Counter for tracking total bytes read from response body.
         */
        private final AtomicLong byteCount = new AtomicLong();

        @Override
        public @NonNull nbbrd.io.net.MediaType getContentType() throws IOException {
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
        public @NonNull String getReasonPhrase() throws IOException {
            return delegate.getReasonPhrase();
        }

        /**
         * Returns the response body as an input stream with byte counting.
         *
         * @return an input stream that tracks bytes read through this method
         * @throws IOException if an I/O error occurs
         */
        @Override
        public @NonNull InputStream getBody() throws IOException {
            return new CountingInputStream(delegate.getBody(), byteCount);
        }

        /**
         * Returns the response body as a disconnecting input stream with byte counting.
         *
         * @return an input stream that tracks bytes read and disconnects on close
         * @throws IOException if an I/O error occurs
         */
        @Override
        public @NonNull InputStream asDisconnectingInputStream() throws IOException {
            return new CountingInputStream(delegate.asDisconnectingInputStream(), byteCount);
        }

        /**
         * Closes the response and reports the total bytes read if any were recorded.
         *
         * @throws IOException if an I/O error occurs while closing
         */
        @Override
        public void close() throws IOException {
            try {
                delegate.close();
            } finally {
                long bytes = byteCount.get();
                if (bytes > 0) {
                    listener.accept(bytes);
                }
            }
        }
    }

    /**
     * Input stream wrapper that counts bytes read.
     * <p>
     * Extends {@link FilterInputStream} to transparently count all bytes read from the
     * wrapped input stream, updating a shared {@link AtomicLong} counter.
     * </p>
     */
    @MightBePromoted
    private static final class CountingInputStream extends FilterInputStream {

        /**
         * Atomic counter for tracking bytes read.
         */
        private final AtomicLong counter;

        /**
         * Creates a counting input stream.
         *
         * @param in      the input stream to wrap and count bytes from
         * @param counter the atomic counter to update with byte count
         */
        CountingInputStream(InputStream in, AtomicLong counter) {
            super(in);
            this.counter = counter;
        }

        /**
         * Reads a single byte and increments the counter.
         *
         * @return the byte read, or -1 if EOF is reached
         * @throws IOException if an I/O error occurs
         */
        @Override
        public int read() throws IOException {
            int result = super.read();
            if (result != -1) counter.incrementAndGet();
            return result;
        }

        /**
         * Reads bytes into the specified array and increments the counter by the number read.
         *
         * @param b   the array to read into
         * @param off the offset in the array
         * @param len the maximum number of bytes to read
         * @return the number of bytes read, or -1 if EOF is reached
         * @throws IOException if an I/O error occurs
         */
        @Override
        @SuppressWarnings("NullableProblems")
        public int read(byte[] b, int off, int len) throws IOException {
            int result = super.read(b, off, len);
            if (result > 0) counter.addAndGet(result);
            return result;
        }
    }
}
