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

import java.io.IOException;
import java.net.URL;
import java.util.List;

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

        assertThat(of(withoutTrailingSlash))
                .hasToString("http://localhost");

        assertThat(of(withTrailingSlash))
                .hasToString("http://localhost/");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testPath() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> of(withoutTrailingSlash).path((String) null));

        assertThatNullPointerException()
                .isThrownBy(() -> of(withoutTrailingSlash).path((List<String>) null));

        assertThat(of(withoutTrailingSlash).path("hello").path("").path("worl/d").build())
                .hasToString("http://localhost/hello//worl%2Fd");

        assertThat(of(withTrailingSlash).path("hello").path("").path("worl/d").build())
                .hasToString("http://localhost/hello//worl%2Fd");

        assertThat(of(withoutTrailingSlash).path(asList("hello", "", "worl/d")).build())
                .hasToString("http://localhost/hello//worl%2Fd");

        assertThat(of(withTrailingSlash).path(asList("hello", "", "worl/d")).build())
                .hasToString("http://localhost/hello//worl%2Fd");
    }

    @Test
    @SuppressWarnings({"DataFlowIssue"})
    public void testParam() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> of(withoutTrailingSlash).param(null, ""));

        assertThatNullPointerException()
                .isThrownBy(() -> of(withoutTrailingSlash).param("", null));

        assertThatNullPointerException()
                .isThrownBy(() -> of(withoutTrailingSlash).param(null));

        assertThat(of(withoutTrailingSlash).param("p1", "v1").param("p&=2", "v&=2").build())
                .hasToString("http://localhost?p1=v1&p%26%3D2=v%26%3D2");

        assertThat(of(withTrailingSlash).param("p1", "v1").param("p&=2", "v&=2").build())
                .hasToString("http://localhost/?p1=v1&p%26%3D2=v%26%3D2");

        assertThat(of(withoutTrailingSlash).path("hello").path("worl/d").param("p1", "v1").param("p&=2", "v&=2").build())
                .hasToString("http://localhost/hello/worl%2Fd?p1=v1&p%26%3D2=v%26%3D2");

        assertThat(of(withTrailingSlash).path("hello").path("worl/d").param("p1", "v1").param("p&=2", "v&=2").build())
                .hasToString("http://localhost/hello/worl%2Fd?p1=v1&p%26%3D2=v%26%3D2");

        assertThat(of(withoutTrailingSlash).param("b", "2").param("a", "1").build())
                .hasToString("http://localhost?b=2&a=1");

        assertThat(of(withTrailingSlash).param("b", "2").param("a", "1").build())
                .hasToString("http://localhost/?b=2&a=1");

        assertThat(of(withoutTrailingSlash).param("b").param("a").build())
                .hasToString("http://localhost?b&a");

        assertThat(of(withTrailingSlash).param("b").param("a").build())
                .hasToString("http://localhost/?b&a");
    }

    @Test
    public void testTrailingSlash() throws IOException {
        assertThat(of(withoutTrailingSlash).trailingSlash(true).build())
                .hasToString("http://localhost/");

        assertThat(of(withoutTrailingSlash).trailingSlash(true).param("p1", "v1").param("p&=2", "v&=2").build())
                .hasToString("http://localhost/?p1=v1&p%26%3D2=v%26%3D2");

        assertThat(of(withoutTrailingSlash).trailingSlash(true).path("hello").path("worl/d").build())
                .hasToString("http://localhost/hello/worl%2Fd/");

        assertThat(of(withoutTrailingSlash).trailingSlash(true).path("hello").path("worl/d").param("p1", "v1").param("p&=2", "v&=2").build())
                .hasToString("http://localhost/hello/worl%2Fd/?p1=v1&p%26%3D2=v%26%3D2");
    }

    private final URL withoutTrailingSlash = Parser.onURL().parseValue("http://localhost").orElseThrow(RuntimeException::new);
    private final URL withTrailingSlash = Parser.onURL().parseValue("http://localhost/").orElseThrow(RuntimeException::new);
}
