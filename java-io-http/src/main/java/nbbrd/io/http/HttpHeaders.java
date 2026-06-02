package nbbrd.io.http;

import lombok.AccessLevel;
import lombok.NonNull;
import nbbrd.design.BuilderPattern;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.design.VisibleForTesting;
import nbbrd.io.net.MediaType;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@lombok.Value
@lombok.RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpHeaders {

    public static final HttpHeaders EMPTY = new HttpHeaders(Collections.emptyMap());

    public static final String HTTP_ACCEPT_HEADER = "Accept";
    public static final String HTTP_ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    public static final String HTTP_ACCEPT_ENCODING_HEADER = "Accept-Encoding";
    public static final String HTTP_LOCATION_HEADER = "Location";
    public static final String HTTP_AUTHORIZATION_HEADER = "Authorization";
    public static final String HTTP_AUTHENTICATE_HEADER = "WWW-Authenticate";
    public static final String HTTP_USER_AGENT_HEADER = "User-Agent";
    public static final String HTTP_CONTENT_TYPE_HEADER = "Content-Type";
    public static final String HTTP_CONTENT_ENCODING_HEADER = "Content-Encoding";
    public static final String HTTP_CONTENT_LENGTH_HEADER = "Content-Length";

    @NonNull
    Map<String, List<String>> map;

    @StaticFactoryMethod
    public static @NonNull HttpHeaders of(@NonNull Map<String, List<String>> headerMap) {
        return builder().put(headerMap).build();
    }

    @StaticFactoryMethod
    public static @NonNull HttpHeaders of(@NonNull Map<String, List<String>> headerMap, @NonNull BiPredicate<String, String> filter) {
        return builder().put(headerMap).filter(filter).build();
    }

    @VisibleForTesting
    static @NonNull String toAcceptHeader(@NonNull List<MediaType> mediaTypes) {
        return mediaTypes.stream().map(MediaType::toString).collect(Collectors.joining(", "));
    }

    public @NonNull Optional<String> firstValue(@NonNull String name) {
        Objects.requireNonNull(name);
        List<String> values = allValues(name);
        return values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }

    public @NonNull List<String> allValues(@NonNull String name) {
        Objects.requireNonNull(name);
        List<String> values = map.get(name);
        return values != null ? values : Collections.emptyList();
    }

    public @NonNull Stream<Map.Entry<String, String>> keyValues() {
        return Builder.keyValues(map);
    }

    public static @NonNull Builder builder() {
        return new Builder();
    }

    public @NonNull Builder toBuilder() {
        return new Builder().put(map);
    }

    @BuilderPattern(HttpHeaders.class)
    @lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    public static final class Builder {

        private final List<Map.Entry<String, String>> data = new ArrayList<>();
        private BiPredicate<String, String> filter = (key, value) -> true;

        private static boolean valueNotNullNorEmpty(String k, String v) {
            return v != null && !v.isEmpty();
        }

        public @NonNull HttpHeaders.Builder put(@NonNull Map<String, List<String>> headers) {
            keyValues(headers).forEach(data::add);
            return this;
        }

        public @NonNull HttpHeaders.Builder put(@NonNull String key, @Nullable String value) {
            data.add(headerOf(key, value));
            return this;
        }

        public @NonNull HttpHeaders.Builder put(@NonNull HttpHeaders headers) {
            return put(headers.map);
        }

        public @NonNull HttpHeaders.Builder mediaTypes(@NonNull List<MediaType> mediaTypes) {
            data.add(headerOf(HTTP_ACCEPT_HEADER, toAcceptHeader(mediaTypes)));
            return this;
        }

        public @NonNull HttpHeaders.Builder mediaType(@NonNull MediaType mediaType) {
            data.add(headerOf(HTTP_ACCEPT_HEADER, mediaType.toString()));
            return this;
        }

        public @NonNull HttpHeaders.Builder languages(@NonNull String languages) {
            data.add(headerOf(HTTP_ACCEPT_LANGUAGE_HEADER, languages));
            return this;
        }

        public @NonNull HttpHeaders.Builder filter(@NonNull BiPredicate<String, String> filter) {
            this.filter = filter;
            return this;
        }

        public @NonNull HttpHeaders build() {
            return new HttpHeaders(
                    data.stream()
                            .filter(header -> valueNotNullNorEmpty(header.getKey(), header.getValue()))
                            .filter(header -> filter.test(header.getKey(), header.getValue()))
                            .collect(COLLECTOR)
            );
        }

        private static final Collector<Map.Entry<String, String>, ?, Map<String, List<String>>> COLLECTOR =
                Collectors.collectingAndThen(
                        Collectors.groupingBy(
                                Map.Entry::getKey,
                                () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER),
                                toUnmodifiableList(Map.Entry::getValue)
                        ),
                        Collections::unmodifiableMap
                );

        private static <T, U> Collector<T, ?, List<U>> toUnmodifiableList(Function<? super T, ? extends U> mapper) {
            return Collectors.mapping(mapper, toUnmodifiableList());
        }

        private static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
            return Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList);
        }

        private static Map.@NonNull Entry<String, String> headerOf(@NonNull String key, @Nullable String value) {
            return new AbstractMap.SimpleImmutableEntry<>(key, value);
        }

        public static @NonNull Stream<Map.Entry<String, String>> keyValues(@NonNull Map<String, List<String>> headers) {
            return headers
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .flatMap(entry -> entry.getValue().stream().map(value -> headerOf(entry.getKey(), value)));
        }
    }
}

