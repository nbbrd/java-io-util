/*
 * Copyright 2018 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package nbbrd.io.http;

import nbbrd.io.text.Parser;
import org.junit.jupiter.api.Test;
import wiremock.org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static nbbrd.io.http.URLQueryBuilder.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Philippe Charles
 */
public class URLQueryBuilderTest {

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testFactory() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> of(null));

        assertThat(of(urlWithoutSlash).build())
                .isEqualTo(uriBuilderAsURL(urlWithoutSlash, DO_NOTHING))
                .hasToString("http://localhost");

        assertThat(of(urlWithSlash).build())
                .isEqualTo(uriBuilderAsURL(urlWithSlash, DO_NOTHING))
                .hasToString("http://localhost/");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testPath() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> of(urlWithoutSlash).path((String) null));

        assertThat(of(urlWithoutSlash).path("hello").path("").path("worl/d").build())
                .isEqualTo(uriBuilderAsURL(urlWithoutSlash, o -> o.appendPathSegments("hello", "", "worl/d")))
                .hasToString("http://localhost/hello//worl%2Fd");

        assertThat(of(urlWithSlash).path("hello").path("").path("worl/d").build())
                .isNotEqualTo(uriBuilderAsURL(urlWithSlash, o -> o.appendPathSegments("hello", "", "worl/d")))
                .hasToString("http://localhost/hello//worl%2Fd");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testPathList() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> of(urlWithoutSlash).path((List<String>) null));

        assertThat(of(urlWithoutSlash).path(asList("hello", "", "worl/d")).build())
                .isEqualTo(uriBuilderAsURL(urlWithoutSlash, o -> o.appendPathSegments("hello", "", "worl/d")))
                .hasToString("http://localhost/hello//worl%2Fd");

        assertThat(of(urlWithSlash).path(asList("hello", "", "worl/d")).build())
                .isNotEqualTo(uriBuilderAsURL(urlWithSlash, o -> o.appendPathSegments("hello", "", "worl/d")))
                .hasToString("http://localhost/hello//worl%2Fd");
    }

    @Test
    @SuppressWarnings({"DataFlowIssue"})
    public void testParam() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> of(urlWithoutSlash).param(null, ""));

        assertThatNullPointerException()
                .isThrownBy(() -> of(urlWithoutSlash).param("", null));

        assertThat(of(urlWithoutSlash).param("p1", "v1").param("p&=2", "v&=2").build())
                .isEqualTo(uriBuilderAsURL(urlWithoutSlash, o -> o.addParameter("p1", "v1").addParameter("p&=2", "v&=2")))
                .hasToString("http://localhost?p1=v1&p%26%3D2=v%26%3D2");

        assertThat(of(urlWithSlash).param("p1", "v1").param("p&=2", "v&=2").build())
                .isEqualTo(uriBuilderAsURL(urlWithSlash, o -> o.addParameter("p1", "v1").addParameter("p&=2", "v&=2")))
                .hasToString("http://localhost/?p1=v1&p%26%3D2=v%26%3D2");

        assertThat(of(urlWithoutSlash).path("hello").path("worl/d").param("p1", "v1").param("p&=2", "v&=2").build())
                .isEqualTo(uriBuilderAsURL(urlWithoutSlash, o -> o.appendPathSegments("hello", "worl/d").addParameter("p1", "v1").addParameter("p&=2", "v&=2")))
                .hasToString("http://localhost/hello/worl%2Fd?p1=v1&p%26%3D2=v%26%3D2");

        assertThat(of(urlWithSlash).path("hello").path("worl/d").param("p1", "v1").param("p&=2", "v&=2").build())
                .isNotEqualTo(uriBuilderAsURL(urlWithSlash, o -> o.appendPathSegments("hello", "worl/d").addParameter("p1", "v1").addParameter("p&=2", "v&=2")))
                .hasToString("http://localhost/hello/worl%2Fd?p1=v1&p%26%3D2=v%26%3D2");

        assertThat(of(urlWithoutSlash).param("b", "2").param("a", "1").build())
                .isEqualTo(uriBuilderAsURL(urlWithoutSlash, o -> o.addParameter("b", "2").addParameter("a", "1")))
                .hasToString("http://localhost?b=2&a=1");

        assertThat(of(urlWithSlash).param("b", "2").param("a", "1").build())
                .isEqualTo(uriBuilderAsURL(urlWithSlash, o -> o.addParameter("b", "2").addParameter("a", "1")))
                .hasToString("http://localhost/?b=2&a=1");
    }

    @Test
    @SuppressWarnings({"DataFlowIssue"})
    public void testParamWithoutValue() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> of(urlWithoutSlash).param(null));

        assertThat(of(urlWithoutSlash).param("b").param("a").build())
                .isEqualTo(uriBuilderAsURL(urlWithoutSlash, o -> o.addParameter("b", null).addParameter("a", null)))
                .hasToString("http://localhost?b&a");

        assertThat(of(urlWithSlash).param("b").param("a").build())
                .isEqualTo(uriBuilderAsURL(urlWithSlash, o -> o.addParameter("b", null).addParameter("a", null)))
                .hasToString("http://localhost/?b&a");
    }

    @Test
    public void testTrailingSlash() throws IOException {
        assertThat(of(urlWithoutSlash).trailingSlash(true).build())
                .hasToString("http://localhost/");

        assertThat(of(urlWithoutSlash).trailingSlash(true).param("p1", "v1").param("p&=2", "v&=2").build())
                .hasToString("http://localhost/?p1=v1&p%26%3D2=v%26%3D2");

        assertThat(of(urlWithoutSlash).trailingSlash(true).path("hello").path("worl/d").build())
                .hasToString("http://localhost/hello/worl%2Fd/");

        assertThat(of(urlWithoutSlash).trailingSlash(true).path("hello").path("worl/d").param("p1", "v1").param("p&=2", "v&=2").build())
                .hasToString("http://localhost/hello/worl%2Fd/?p1=v1&p%26%3D2=v%26%3D2");
    }

    @Test
    public void testEncodingOfSpaceCharacter() throws IOException {
        assertThat(of(urlWithoutSlash).path("a b+").build())
                .isEqualTo(uriBuilderAsURL(urlWithoutSlash, o -> o.appendPathSegments("a b+")))
                .hasToString("http://localhost/a%20b%2B");

        assertThat(of(urlWithSlash).path("a b+").build())
                .isNotEqualTo(uriBuilderAsURL(urlWithSlash, o -> o.appendPathSegments("a b+")))
                .hasToString("http://localhost/a%20b%2B");

        assertThat(of(urlWithoutSlash).param("x y+", "a b+").build())
                .isEqualTo(uriBuilderAsURL(urlWithoutSlash, o -> o.addParameter("x y+","a b+")))
                .hasToString("http://localhost?x%20y%2B=a%20b%2B");

        assertThat(of(urlWithSlash).param("x y+", "a b+").build())
                .isEqualTo(uriBuilderAsURL(urlWithSlash, o -> o.addParameter("x y+","a b+")))
                .hasToString("http://localhost/?x%20y%2B=a%20b%2B");
    }

    private final URL urlWithoutSlash = Parser.onURL().parseValue("http://localhost").orElseThrow(RuntimeException::new);
    private final URL urlWithSlash = Parser.onURL().parseValue("http://localhost/").orElseThrow(RuntimeException::new);

    private static URL uriBuilderAsURL(URL base, Consumer<? super URIBuilder> consumer) {
        try {
            URIBuilder builder = new URIBuilder(base.toURI());
            consumer.accept(builder);
            return builder.build().toURL();
        } catch (URISyntaxException | MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final Consumer<Object> DO_NOTHING = ignore -> {
    };
}
