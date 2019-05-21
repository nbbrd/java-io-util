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
package ioutil;

import _test.sample.ParseAssertions;
import _test.sample.Person;
import java.io.IOException;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;
import _test.ForwardingUnmarshaller;
import _test.ForwardingXMLInputFactory;
import _test.StaxListener;
import _test.JaxbListener;
import _test.Meta;
import javax.xml.bind.Marshaller;
import static _test.sample.FormatAssertions.assertFormatterCompliance;
import static _test.sample.ParseAssertions.assertParserCompliance;

/**
 *
 * @author Philippe Charles
 */
public class JaxbTest {

    private final IO.Supplier<Unmarshaller> validUnmarshaller = () -> Jaxb.createUnmarshaller(Person.class);
    private final IO.Supplier<Marshaller> validMarshaller = () -> Jaxb.createMarshaller(Person.class);
    private final IO.Supplier<XMLInputFactory> validXxeFactory = XMLInputFactory::newFactory;

    @Test
    @SuppressWarnings("null")
    public void testCreateUnmarshaller() throws Exception {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.createUnmarshaller((Class<?>) null));

        assertThatThrownBy(() -> Jaxb.createUnmarshaller(Runnable.class))
                .isInstanceOf(Xml.WrappedException.class)
                .hasCauseInstanceOf(JAXBException.class);

        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.createUnmarshaller((JAXBContext) null));
    }

    @Test
    @SuppressWarnings("null")
    public void testCreateMarshaller() throws Exception {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.createMarshaller((Class<?>) null));

        assertThatThrownBy(() -> Jaxb.createMarshaller(Runnable.class))
                .isInstanceOf(Xml.WrappedException.class)
                .hasCauseInstanceOf(JAXBException.class);

        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.createMarshaller((JAXBContext) null));
    }

    @Test
    public void testXXE() throws Exception {
        Jaxb.Parser<Person> p = Jaxb.Parser.of(Person.class);
        ParseAssertions.testXXE(p, p.toBuilder().ignoreXXE(true).build());
    }

    @Test
    @SuppressWarnings("null")
    public void testParserOfClass() throws Exception {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.of((Class<?>) null));

        assertParserCompliance(Jaxb.Parser.of(Person.class));
    }

    @Test
    @SuppressWarnings("null")
    public void testParserOfContext() throws Exception {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.of((JAXBContext) null));

        assertParserCompliance(Jaxb.Parser.of(JAXBContext.newInstance(Person.class)));
    }

    @Test
    @SuppressWarnings("null")
    public void testParserBuilder() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.builder().build());

        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.builder().factory(null).build());

        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Parser.builder().factory(validUnmarshaller).xxeFactory(null).build());

        for (boolean ignoreXXE : new boolean[]{false, true}) {
            assertParserCompliance(
                    Jaxb.Parser.<Person>builder()
                            .factory(validUnmarshaller)
                            .xxeFactory(validXxeFactory)
                            .ignoreXXE(ignoreXXE)
                            .build()
            );
        }
    }

    @Test
    public void testParserResources() throws IOException {
        List<Meta<IO.Supplier<Unmarshaller>>> factories = Meta.<IO.Supplier<Unmarshaller>>builder()
                .valid("Ok", validUnmarshaller)
                .invalid("Null", IO.Supplier.of(null))
                .invalid("Throwing", IO.Supplier.throwing(IOError::new))
                .invalid("Checked", validUnmarshaller.andThen(o -> new ForwardingUnmarshaller(o).onUnmarshal(JaxbListener.checked(JaxbError::new))))
                .invalid("Unchecked", validUnmarshaller.andThen(o -> new ForwardingUnmarshaller(o).onUnmarshal(JaxbListener.unchecked(UncheckedError::new))))
                .build();

        for (boolean xxe : new boolean[]{true, false}) {

            List<Meta<IO.Supplier<XMLInputFactory>>> xxeFactories = Meta.<IO.Supplier<XMLInputFactory>>builder()
                    .valid("Ok", validXxeFactory)
                    .of("Null", xxe, IO.Supplier.of(null))
                    .of("Throwing", xxe, IO.Supplier.throwing(IOError::new))
                    .of("Checked", xxe, validXxeFactory.andThen(o -> new ForwardingXMLInputFactory(o).onCreate(StaxListener.checked(StaxError::new))))
                    .of("Unchecked", xxe, validXxeFactory.andThen(o -> new ForwardingXMLInputFactory(o).onCreate(StaxListener.unchecked(UncheckedError::new))))
                    .build();

            for (Meta<IO.Supplier<Unmarshaller>> factory : factories) {
                for (Meta<IO.Supplier<XMLInputFactory>> xxeFactory : xxeFactories) {

                    Xml.Parser<Person> p = Jaxb.Parser.<Person>builder()
                            .factory(factory.getTarget())
                            .ignoreXXE(!xxe)
                            .xxeFactory(xxeFactory.getTarget())
                            .build();

                    ParseAssertions.testParserResources(p, Meta.lookupExpectedException(factory, xxeFactory));
                }
            }
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testFormatterOfClass() throws Exception {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Formatter.of((Class<?>) null));

        assertFormatterCompliance(Jaxb.Formatter.of(Person.class), false);
    }

    @Test
    @SuppressWarnings("null")
    public void testFormatterOfContext() throws Exception {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Formatter.of((JAXBContext) null));

        assertFormatterCompliance(Jaxb.Formatter.of(JAXBContext.newInstance(Person.class)), false);
    }

    @Test
    @SuppressWarnings("null")
    public void testFormatterBuilder() throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Formatter.builder().build());

        assertThatNullPointerException()
                .isThrownBy(() -> Jaxb.Formatter.builder().factory(null).build());

        for (boolean formatted : new boolean[]{false, true}) {
            assertFormatterCompliance(
                    Jaxb.Formatter.<Person>builder()
                            .factory(validMarshaller)
                            .formatted(formatted)
                            .build(),
                    formatted);
        }
    }

    @Test
    public void testFormatterResources() throws IOException {
        // TODO
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
}
