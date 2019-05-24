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

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Stax {

    /**
     * Prevents XXE vulnerability by disabling features.
     *
     * @param factory non-null factory
     * @see
     * https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#XMLInputFactory_.28a_StAX_parser.29
     */
    public void preventXXE(@Nonnull XMLInputFactory factory) {
        setFeature(factory, XMLInputFactory.SUPPORT_DTD, false);
        setFeature(factory, XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    }

    @FunctionalInterface
    public interface FlowHandler<I, T> {

        @Nonnull
        T parse(@Nonnull I input, @Nonnull Closeable onClose) throws IOException, XMLStreamException;

        @Nonnull
        static <I, T> FlowHandler<I, T> of(@Nonnull ValueHandler<I, T> handler) {
            return handler.asFlow();
        }
    }

    @FunctionalInterface
    public interface ValueHandler<I, T> {

        @Nonnull
        T parse(@Nonnull I input) throws XMLStreamException;

        @Nonnull
        default FlowHandler<I, T> asFlow() {
            return (input, onClose) -> {
                try (Closeable c = onClose) {
                    return parse(input);
                }
            };
        }
    }

    @FunctionalInterface
    public interface OutputHandler<O, T> {

        void format(@Nonnull T value, @Nonnull O output) throws XMLStreamException;
    }

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public static final class StreamParser<T> implements Xml.Parser<T> {

        @Nonnull
        public static <T> StreamParser<T> flowOf(@Nonnull FlowHandler<XMLStreamReader, T> handler) {
            return StreamParser.<T>builder().flow(handler).build();
        }

        @Nonnull
        public static <T> StreamParser<T> valueOf(@Nonnull ValueHandler<XMLStreamReader, T> handler) {
            return flowOf(handler.asFlow());
        }

        public static class Builder<T> {

            // default values
            Builder() {
                this.handler = null;
                this.factory = XMLInputFactory::newFactory;
                this.ignoreXXE = false;
            }

            @Deprecated
            public Builder<T> preventXXE(boolean preventXXE) {
                this.ignoreXXE = !preventXXE;
                return this;
            }

            public Builder<T> flow(FlowHandler<XMLStreamReader, T> handler) {
                return handler(handler);
            }

            public Builder<T> value(ValueHandler<XMLStreamReader, T> handler) {
                return handler(handler.asFlow());
            }
        }

        @lombok.NonNull
        private final FlowHandler<XMLStreamReader, T> handler;

        @lombok.NonNull
        private final IO.Supplier<? extends XMLInputFactory> factory;

        private final boolean ignoreXXE;

        @Override
        public T parseFile(File source) throws IOException {
            LegacyFiles.checkSource(source);
            InputStream resource = LegacyFiles.newInputStream(source);
            return parse(o -> o.createXMLStreamReader(Xml.toSystemId(source), resource), resource);
        }

        @Override
        public T parseReader(IO.Supplier<? extends Reader> source) throws IOException {
            Objects.requireNonNull(source, "source");
            Reader resource = Xml.open(source);
            return parse(o -> o.createXMLStreamReader(resource), resource);
        }

        @Override
        public T parseStream(IO.Supplier<? extends InputStream> source) throws IOException {
            Objects.requireNonNull(source, "source");
            InputStream resource = Xml.open(source);
            return parse(o -> o.createXMLStreamReader(resource), resource);
        }

        @Override
        public T parseReader(Reader resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
            return parse(o -> o.createXMLStreamReader(resource), NOTHING_TO_CLOSE);
        }

        @Override
        public T parseStream(InputStream resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
            return parse(o -> o.createXMLStreamReader(resource), NOTHING_TO_CLOSE);
        }

        @Nonnull
        public T parse(@Nonnull XMLStreamReader input, @Nonnull Closeable onClose) throws IOException {
            return doParse(handler, input, onClose);
        }

        private T parse(XFunction<XMLInputFactory, XMLStreamReader> supplier, Closeable onClose) throws IOException {
            try {
                XMLStreamReader input = supplier.apply(getInputEngine(factory, ignoreXXE));
                return parse(input, () -> closeBoth(input::close, onClose));
            } catch (XMLStreamException ex) {
                IO.ensureClosed(ex, onClose);
                throw toIOException(ex);
            } catch (Error | RuntimeException | IOException ex) {
                IO.ensureClosed(ex, onClose);
                throw ex;
            }
        }
    }

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public static final class EventParser<T> implements Xml.Parser<T> {

        @Nonnull
        public static <T> EventParser<T> flowOf(@Nonnull FlowHandler<XMLEventReader, T> handler) {
            return EventParser.<T>builder().flow(handler).build();
        }

        @Nonnull
        public static <T> EventParser<T> valueOf(@Nonnull ValueHandler<XMLEventReader, T> handler) {
            return flowOf(handler.asFlow());
        }

        public static class Builder<T> {

            // default values
            Builder() {
                this.handler = null;
                this.factory = XMLInputFactory::newFactory;
                this.ignoreXXE = false;
            }

            @Deprecated
            public Builder<T> preventXXE(boolean preventXXE) {
                this.ignoreXXE = !preventXXE;
                return this;
            }

            public Builder<T> flow(FlowHandler<XMLEventReader, T> handler) {
                return handler(handler);
            }

            public Builder<T> value(ValueHandler<XMLEventReader, T> handler) {
                return handler(handler.asFlow());
            }
        }

        @lombok.NonNull
        private final FlowHandler<XMLEventReader, T> handler;

        @lombok.NonNull
        private final IO.Supplier<? extends XMLInputFactory> factory;

        private final boolean ignoreXXE;

        @Override
        public T parseFile(File source) throws IOException {
            LegacyFiles.checkSource(source);
            InputStream resource = LegacyFiles.newInputStream(source);
            return parse(o -> o.createXMLEventReader(Xml.toSystemId(source), resource), resource);
        }

        @Override
        public T parseReader(IO.Supplier<? extends Reader> source) throws IOException {
            Objects.requireNonNull(source, "source");
            Reader resource = Xml.open(source);
            return parse(o -> o.createXMLEventReader(resource), resource);
        }

        @Override
        public T parseStream(IO.Supplier<? extends InputStream> source) throws IOException {
            Objects.requireNonNull(source, "source");
            InputStream resource = Xml.open(source);
            return parse(o -> o.createXMLEventReader(resource), resource);
        }

        @Override
        public T parseReader(Reader resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
            return parse(o -> o.createXMLEventReader(resource), NOTHING_TO_CLOSE);
        }

        @Override
        public T parseStream(InputStream resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
            return parse(o -> o.createXMLEventReader(resource), NOTHING_TO_CLOSE);
        }

        private T parse(XMLEventReader input, Closeable onClose) throws IOException {
            return doParse(handler, input, onClose);
        }

        private T parse(XFunction<XMLInputFactory, XMLEventReader> supplier, Closeable onClose) throws IOException {
            try {
                XMLEventReader input = supplier.apply(getInputEngine(factory, ignoreXXE));
                return parse(input, () -> closeBoth(input::close, onClose));
            } catch (XMLStreamException ex) {
                IO.ensureClosed(ex, onClose);
                throw toIOException(ex);
            } catch (Error | RuntimeException | IOException ex) {
                IO.ensureClosed(ex, onClose);
                throw ex;
            }
        }
    }

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public static final class StreamFormatter<T> implements Xml.Formatter<T> {

        @Nonnull
        public static <T> StreamFormatter<T> valueOf(@Nonnull OutputHandler<XMLStreamWriter, T> handler) {
            return StreamFormatter.<T>builder().handler(handler).build();
        }

        public static class Builder<T> {

            // default values
            Builder() {
                this.handler = null;
                this.factory = XMLOutputFactory::newFactory;
            }
        }

        @lombok.NonNull
        private final OutputHandler<XMLStreamWriter, T> handler;

        @lombok.NonNull
        private final IO.Supplier<? extends XMLOutputFactory> factory;

        @Override
        public void formatFile(T value, File target) throws IOException {
            Objects.requireNonNull(value, "value");
            LegacyFiles.checkTarget(target);
            try (OutputStream resource = LegacyFiles.newOutputStream(target)) {
                format(value, o -> o.createXMLStreamWriter(resource));
            }
        }

        @Override
        public void formatWriter(T value, IO.Supplier<? extends Writer> target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            try (Writer resource = Xml.open(target)) {
                format(value, o -> o.createXMLStreamWriter(resource));
            }
        }

        @Override
        public void formatStream(T value, IO.Supplier<? extends OutputStream> target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            try (OutputStream resource = Xml.open(target)) {
                format(value, o -> o.createXMLStreamWriter(resource));
            }
        }

        @Override
        public void formatWriter(T value, Writer resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            format(value, o -> o.createXMLStreamWriter(resource));
        }

        @Override
        public void formatStream(T value, OutputStream resource) throws IOException {
            Objects.requireNonNull(value);
            Objects.requireNonNull(resource, "resource");
            format(value, o -> o.createXMLStreamWriter(resource));
        }

        private void format(T value, XFunction<XMLOutputFactory, XMLStreamWriter> supplier) throws IOException {
            try {
                XMLStreamWriter output = supplier.apply(getEngine());
                doFormat(handler, value, output, () -> close(output::close));
            } catch (XMLStreamException ex) {
                throw toIOException(ex);
            }
        }

        private XMLOutputFactory getEngine() throws IOException {
            XMLOutputFactory result = factory.getWithIO();
            return result;
        }
    }

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public static final class EventFormatter<T> implements Xml.Formatter<T> {

        @Nonnull
        public static <T> EventFormatter<T> valueOf(@Nonnull OutputHandler<XMLEventWriter, T> handler) {
            return EventFormatter.<T>builder().handler(handler).build();
        }

        public static class Builder<T> {

            // default values
            Builder() {
                this.handler = null;
                this.factory = XMLOutputFactory::newFactory;
            }
        }

        @lombok.NonNull
        private final OutputHandler<XMLEventWriter, T> handler;

        @lombok.NonNull
        private final IO.Supplier<? extends XMLOutputFactory> factory;

        @Override
        public void formatFile(T value, File target) throws IOException {
            Objects.requireNonNull(value, "value");
            LegacyFiles.checkTarget(target);
            try (OutputStream resource = LegacyFiles.newOutputStream(target)) {
                format(value, o -> o.createXMLEventWriter(resource));
            }
        }

        @Override
        public void formatWriter(T value, IO.Supplier<? extends Writer> target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            try (Writer resource = Xml.open(target)) {
                format(value, o -> o.createXMLEventWriter(resource));
            }
        }

        @Override
        public void formatStream(T value, IO.Supplier<? extends OutputStream> target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            try (OutputStream resource = Xml.open(target)) {
                format(value, o -> o.createXMLEventWriter(resource));
            }
        }

        @Override
        public void formatWriter(T value, Writer resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            format(value, o -> o.createXMLEventWriter(resource));
        }

        @Override
        public void formatStream(T value, OutputStream resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            format(value, o -> o.createXMLEventWriter(resource));
        }

        private void format(T value, XFunction<XMLOutputFactory, XMLEventWriter> supplier) throws IOException {
            try {
                XMLEventWriter output = supplier.apply(getEngine());
                doFormat(handler, value, output, () -> close(output::close));
            } catch (XMLStreamException ex) {
                throw toIOException(ex);
            }
        }

        private XMLOutputFactory getEngine() throws IOException {
            XMLOutputFactory result = factory.getWithIO();
            return result;
        }
    }

    private XMLInputFactory getInputEngine(IO.Supplier<? extends XMLInputFactory> factory, boolean ignoreXXE) throws IOException {
        XMLInputFactory result = factory.getWithIO();
        if (!ignoreXXE) {
            preventXXE(result);
        }
        return result;
    }

    private <T, INPUT> T doParse(FlowHandler<INPUT, T> handler, INPUT input, Closeable onClose) throws IOException {
        try {
            return handler.parse(input, onClose);
        } catch (XMLStreamException ex) {
            IO.ensureClosed(ex, onClose);
            throw toIOException(ex);
        } catch (Error | RuntimeException | IOException ex) {
            IO.ensureClosed(ex, onClose);
            throw ex;
        }
    }

    private <T, OUTPUT> void doFormat(OutputHandler<OUTPUT, T> handler, T value, OUTPUT output, Closeable onClose) throws IOException {
        try {
            handler.format(value, output);
        } catch (XMLStreamException ex) {
            IO.ensureClosed(ex, onClose);
            throw toIOException(ex);
        } catch (Error | RuntimeException ex) {
            IO.ensureClosed(ex, onClose);
            throw ex;
        }
    }

    private void close(XRunnable first) throws IOException {
        try {
            first.run();
        } catch (XMLStreamException ex) {
            throw toIOException(ex);
        }
    }

    private void closeBoth(XRunnable first, Closeable second) throws IOException {
        try {
            first.run();
        } catch (XMLStreamException ex) {
            IO.ensureClosed(ex, second);
            throw toIOException(ex);
        } catch (Error | RuntimeException ex) {
            IO.ensureClosed(ex, second);
            throw ex;
        }
        second.close();
    }

    @FunctionalInterface
    private interface XRunnable {

        void run() throws XMLStreamException;
    }

    @FunctionalInterface
    private interface XFunction<T, R> {

        R apply(T t) throws XMLStreamException;
    }

    private void setFeature(XMLInputFactory factory, String feature, boolean value) {
        if (factory.isPropertySupported(feature)
                && ((Boolean) factory.getProperty(feature)) != value) {
            factory.setProperty(feature, value);
        }
    }

    IOException toIOException(XMLStreamException ex) {
        if (isEOF(ex)) {
            return new EOFException(Objects.toString(getFile(ex)));
        }
        return new Xml.WrappedException(ex);
    }

    private boolean isEOF(XMLStreamException ex) {
        return ex.getLocation() != null && ex.getMessage() != null && ex.getMessage().contains("end of file");
    }

    private File getFile(XMLStreamException ex) {
        String result = ex.getLocation().getSystemId();
        return result != null && result.startsWith("file:/") ? Xml.fromSystemId(result) : null;
    }

    private final Closeable NOTHING_TO_CLOSE = IO.Runnable.noOp().asCloseable();
}
