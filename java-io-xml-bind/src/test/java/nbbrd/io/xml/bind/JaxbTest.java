/*
 * Copyright 2017 National Bank of Belgium
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
package nbbrd.io.xml.bind;

import _test.*;
import _test.sample.XmlParserAssertions;
import _test.sample.Person;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import nbbrd.io.WrappedIOException;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.xml.Xml;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static _test.sample.XmlFormatterAssertions.assertXmlFormatterCompliance;
import static _test.sample.XmlFormatterAssertions.assertFormatterSafety;
import static _test.sample.XmlParserAssertions.assertXmlParserCompliance;
import static _test.sample.XmlParserAssertions.assertParserSafety;
import static _test.sample.Person.BOOLS;
import static _test.sample.Person.ENCODINGS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philippe Charles
 */
public class JaxbTest {

    @RegisterExtension
    final WireMockExtension wire = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().dynamicPort())
            .build();

    private final IOSupplier<Unmarshaller> validUnmarshaller = () -> Jaxb.createUnmarshaller(Person.class);
    private final IOSupplier<Marshaller> validMarshaller = () -> Jaxb.createMarshaller(Person.class);
    private final IOSupplier<XMLInputFactory> validXxeFactory = XMLInputFactory::newFactory;

    @Test
    @SuppressWarnings("null")
    public void testCreateUnmarshaller() throws Exception {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.createUnmarshaller((Class<?>) null));

        Assertions.assertThatThrownBy(() -> Jaxb.createUnmarshaller(Runnable.class))
                .isInstanceOf(WrappedIOException.class)
                .hasCauseInstanceOf(JAXBException.class);

        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.createUnmarshaller((JAXBContext) null));
    }

    @Test
    @SuppressWarnings("null")
    public void testCreateMarshaller() throws Exception {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.createMarshaller((Class<?>) null));

        Assertions.assertThatThrownBy(() -> Jaxb.createMarshaller(Runnable.class))
                .isInstanceOf(WrappedIOException.class)
                .hasCauseInstanceOf(JAXBException.class);

        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.createMarshaller((JAXBContext) null));
    }

    @Test
    public void testXXE() throws Exception {
        Jaxb.Parser<Person> p = Jaxb.Parser.of(Person.class);
        XmlParserAssertions.testXXE(wire, p, p.withIgnoreXXE(true));
    }

    @Test
    @SuppressWarnings("null")
    public void testParserOfClass(@TempDir Path temp) throws Exception {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.of((Class<?>) null));

        assertXmlParserCompliance(temp, Jaxb.Parser.of(Person.class));
    }

    @Test
    @SuppressWarnings("null")
    public void testParserOfContext(@TempDir Path temp) throws Exception {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.of((JAXBContext) null));

        assertXmlParserCompliance(temp, Jaxb.Parser.of(JAXBContext.newInstance(Person.class)));
    }

    @Test
    @SuppressWarnings("null")
    public void testParserBuilder(@TempDir Path temp) throws IOException {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.builder().build());

        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.builder().factory(null).build());

        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.builder().factory(validUnmarshaller).xxeFactory(null).build());

        for (boolean ignoreXXE : BOOLS) {
            assertXmlParserCompliance(
                    temp, Jaxb.Parser.<Person>builder()
                            .factory(validUnmarshaller)
                            .xxeFactory(validXxeFactory)
                            .ignoreXXE(ignoreXXE)
                            .build()
            );
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testParserWither() throws IOException {
        Jaxb.Parser<Person> parser = Jaxb.Parser.of(Person.class);

        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> parser.withFactory(null));

        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> parser.withXxeFactory(null));
    }

    @Test
    public void testParserSafety() throws IOException {
        List<Meta<IOSupplier<Unmarshaller>>> factories = Meta.<IOSupplier<Unmarshaller>>builder()
                .valid("Ok", validUnmarshaller)
                .invalid("Null", IOSupplier.of(null))
                .invalid("Throwing", errorSupplier())
                .invalid("Checked", validUnmarshaller.andThen(o -> new ForwardingUnmarshaller(o).onUnmarshal(JaxbListener.checked(JaxbError::new))))
                .invalid("Unchecked", validUnmarshaller.andThen(o -> new ForwardingUnmarshaller(o).onUnmarshal(JaxbListener.unchecked(UncheckedError::new))))
                .build();

        for (boolean xxe : BOOLS) {

            List<Meta<IOSupplier<XMLInputFactory>>> xxeFactories = Meta.<IOSupplier<XMLInputFactory>>builder()
                    .valid("Ok", validXxeFactory)
                    .of("Null", xxe, IOSupplier.of(null))
                    .of("Throwing", xxe, errorSupplier())
                    .of("Checked", xxe, validXxeFactory.andThen(o -> new ForwardingXMLInputFactory(o).onCreate(StaxListener.checked(StaxError::new))))
                    .of("Unchecked", xxe, validXxeFactory.andThen(o -> new ForwardingXMLInputFactory(o).onCreate(StaxListener.unchecked(UncheckedError::new))))
                    .build();

            for (Meta<IOSupplier<Unmarshaller>> factory : factories) {
                for (Meta<IOSupplier<XMLInputFactory>> xxeFactory : xxeFactories) {

                    Xml.Parser<Person> parser = Jaxb.Parser
                            .<Person>builder()
                            .factory(factory.getTarget())
                            .ignoreXXE(!xxe)
                            .xxeFactory(xxeFactory.getTarget())
                            .build();

                    assertParserSafety(parser, Meta.lookupExpectedException(factory, xxeFactory));
                }
            }
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testFormatterOfClass(@TempDir Path temp) throws Exception {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Formatter.of((Class<?>) null));

        assertXmlFormatterCompliance(temp, Jaxb.Formatter.of(Person.class), false);
    }

    @Test
    @SuppressWarnings("null")
    public void testFormatterOfContext(@TempDir Path temp) throws Exception {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Formatter.of((JAXBContext) null));

        assertXmlFormatterCompliance(temp, Jaxb.Formatter.of(JAXBContext.newInstance(Person.class)), false);
    }

    @Test
    @SuppressWarnings("null")
    public void testFormatterBuilder(@TempDir Path temp) throws IOException {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Formatter.builder().build());

        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Formatter.builder().factory(null).build());

        for (boolean formatted : BOOLS) {
            assertXmlFormatterCompliance(
                    temp, Jaxb.Formatter.<Person>builder()
                            .factory(validMarshaller)
                            .formatted(formatted)
                            .build(),
                    formatted);
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testFormatterWither() throws IOException {
        Jaxb.Formatter<Person> formatter = Jaxb.Formatter.of(Person.class);

        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> formatter.withFactory(null));

        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> formatter.withEncoding(null));
    }

    @Test
    @SuppressWarnings("null")
    public void testFormatterWithAlternateEncoding() throws IOException {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Formatter.of(Person.class).withEncoding(null));

        assertThat(Jaxb.Formatter.of(Person.class).withEncoding(StandardCharsets.ISO_8859_1).formatToString(Person.JOHN_DOE))
                .isEqualTo(Person.getString(StandardCharsets.UTF_8, false).replace("UTF-8", "ISO-8859-1"));
    }

    @Test
    public void testFormatterSafety(@TempDir Path temp) throws IOException {
        List<Meta<IOSupplier<Marshaller>>> factories = Meta.<IOSupplier<Marshaller>>builder()
                .valid("Ok", validMarshaller)
                .invalid("Null", IOSupplier.of(null))
                .invalid("Throwing", errorSupplier())
                .invalid("Checked", validMarshaller.andThen(o -> new ForwardingMarshaller(o).onMarshal(JaxbListener.checked(JaxbError::new))))
                .invalid("Unchecked", validMarshaller.andThen(o -> new ForwardingMarshaller(o).onMarshal(JaxbListener.unchecked(UncheckedError::new))))
                .build();

        for (boolean formatted : BOOLS) {
            for (Charset encoding : ENCODINGS) {
                for (Meta<IOSupplier<Marshaller>> factory : factories) {

                    Xml.Formatter<Person> formatter = Jaxb.Formatter
                            .<Person>builder()
                            .factory(factory.getTarget())
                            .formatted(formatted)
                            .encoding(encoding)
                            .build();

                    assertFormatterSafety(temp, formatter, Meta.lookupExpectedException(factory));
                }
            }
        }
    }

    private static final class IOError extends IOException {
    }

    private static final class UncheckedError extends RuntimeException {
    }

    private static final class JaxbError extends JAXBException {

        public JaxbError() {
            super("");
        }
    }

    private static final class StaxError extends XMLStreamException {
    }

    private static <X> IOSupplier<X> errorSupplier() {
        return () -> {
            throw new IOError();
        };
    }
}
