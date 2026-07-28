package nbbrd.io.http;

import nbbrd.io.net.MediaType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;

public class HttpHeadersTest {

    @Test
    public void testOfAppliesFilterAndBuildsCaseInsensitiveHeaders() {
        Map<String, java.util.List<String>> source = new LinkedHashMap<>();
        source.put("Content-Type", Collections.singletonList("text/plain"));
        source.put("X-Test", Arrays.asList("skip", "keep"));

        HttpHeaders headers = HttpHeaders.of(source, (name, value) -> !"skip".equals(value));

        assertThat(headers.firstValue("content-type")).hasValue("text/plain");
        assertThat(headers.allValues("x-test")).containsExactly("keep");
        assertThat(headers.getMap()).containsKey("X-Test");
    }

    @Test
    public void testAccessorsForMissingHeaders() {
        HttpHeaders headers = HttpHeaders.of(Collections.emptyMap());

        assertThat(headers.firstValue("missing")).isEmpty();
        assertThat(headers.allValues("missing")).isEmpty();
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testOfRejectsNullInputs() {
        assertThatNullPointerException()
                .isThrownBy(() -> HttpHeaders.of(null));

        assertThatNullPointerException()
                .isThrownBy(() -> HttpHeaders.of(Collections.emptyMap(), null));

        Map<String, java.util.List<String>> nullName = new LinkedHashMap<>();
        nullName.put(null, Collections.singletonList("x"));
        assertThat(HttpHeaders.of(nullName).getMap())
                .isEmpty();

        Map<String, java.util.List<String>> nullValues = new LinkedHashMap<>();
        nullValues.put("X", null);
        assertThat(HttpHeaders.of(nullValues).getMap())
                .isEmpty();

        Map<String, java.util.List<String>> nullValueItem = new LinkedHashMap<>();
        nullValueItem.put("X", Collections.singletonList(null));
        assertThat(HttpHeaders.of(nullValueItem).getMap())
                .isEmpty();
    }

    @Test
    public void testMapAndValueListsAreImmutable() {
        Map<String, java.util.List<String>> source = Collections.singletonMap("X", Collections.singletonList("y"));
        HttpHeaders headers = HttpHeaders.of(source);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> headers.getMap().put("Z", Collections.singletonList("1")));

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> headers.allValues("X").add("2"));
    }

    @Test
    public void test() {
        assertThat(HttpHeaders.builder().build().getMap())
                .isEmpty();

        assertThatNullPointerException()
                .isThrownBy(() -> HttpHeaders.builder().put(null, "v1"));

        assertThatCode(() -> HttpHeaders.builder().put("k1", null))
                .doesNotThrowAnyException();

        assertThat(HttpHeaders.builder().put("k1", "v1").put("k2", "v2").build().getMap())
                .containsEntry("k1", singletonList("v1"))
                .containsEntry("k2", singletonList("v2"))
                .hasSize(2);

        assertThat(HttpHeaders.builder().put("k1", "v1").put("k1", "v2").build().getMap())
                .containsEntry("k1", asList("v1", "v2"))
                .hasSize(1);

        assertThat(HttpHeaders.builder().put("k1", "v2").put("k1", "v1").build().getMap())
                .containsEntry("k1", asList("v2", "v1"))
                .containsKeys("K1", "k1")
                .hasSize(1);

        assertThat(HttpHeaders.builder().put("k1", "v2").put("K1", "v1").build().getMap())
                .containsEntry("k1", asList("v2", "v1"))
                .containsKeys("K1", "k1")
                .hasSize(1);

        assertThat(HttpHeaders.builder().put("K1", "v2").put("k1", "v1").build().getMap())
                .containsEntry("K1", asList("v2", "v1"))
                .containsKeys("K1", "k1")
                .hasSize(1);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> HttpHeaders.builder().put("K1", "v2").build().getMap().put("k2", singletonList("v2")));

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> HttpHeaders.builder().put("K1", "v2").build().getMap().get("k1").add("v2"));

        assertThat(HttpHeaders.builder().put("k1", "v1").put("k2", null).build().getMap())
                .containsEntry("k1", singletonList("v1"))
                .hasSize(1);

        assertThat(HttpHeaders.builder().put("k1", "v1").put("k2", "").build().getMap())
                .containsEntry("k1", singletonList("v1"))
                .hasSize(1);
    }

    @Test
    public void testToBuilderAndKeyValues() {
        HttpHeaders headers = HttpHeaders.builder()
                .put("X-One", "a")
                .put("X-One", "b")
                .build();

        HttpHeaders copy = headers.toBuilder().build();

        assertThat(copy).isEqualTo(headers);
        assertThat(copy.keyValues().collect(java.util.stream.Collectors.toList()))
                .containsExactly(
                        new AbstractMap.SimpleImmutableEntry<>("X-One", "a"),
                        new AbstractMap.SimpleImmutableEntry<>("X-One", "b")
                );
    }

    @Test
    public void testBuilderConvenienceMethods() {
        HttpHeaders base = HttpHeaders.builder()
                .put("X-Base", "1")
                .build();

        HttpHeaders result = HttpHeaders.builder()
                .put(base)
                .mediaType(MediaType.parse("text/plain"))
                .mediaTypes(asList(MediaType.parse("application/json"), MediaType.parse("application/xml")))
                .languages("en")
                .build();

        assertThat(result.getMap())
                .containsEntry("X-Base", singletonList("1"))
                .containsEntry(HttpHeaders.HTTP_ACCEPT_HEADER, asList("text/plain", "application/json, application/xml"))
                .containsEntry(HttpHeaders.HTTP_ACCEPT_LANGUAGE_HEADER, singletonList("en"));
    }

    @Test
    public void testToAcceptHeader() {
        Assertions.assertThat(HttpHeaders.toAcceptHeader(emptyList()))
                .isEqualTo("");

        assertThat(HttpHeaders.toAcceptHeader(asList(MediaType.parse("text/html"), MediaType.parse("application/xhtml+xml"))))
                .isEqualTo("text/html, application/xhtml+xml");
    }
}
