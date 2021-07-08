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
package nbbrd.io.xml;

import internal.io.text.LegacyFiles;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.Resource;
import nbbrd.io.WrappedIOException;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IOSupplier;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.xml.stream.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Stax {

    /**
     * Prevents XXE vulnerability by disabling features.
     *
     * @param factory non-null factory
     * @see <a href="https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#XMLInputFactory_.28a_StAX_parser.29">XXE</a>
     */
    public void preventXXE(@NonNull XMLInputFactory factory) {
        setFeature(factory, XMLInputFactory.SUPPORT_DTD, false);
        setFeature(factory, XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    }

    @FunctionalInterface
    public interface FlowHandler<I, T> {

        @NonNull
        T parse(@NonNull I input, @NonNull Closeable onClose) throws IOException, XMLStreamException;

        @StaticFactoryMethod
        static <I, T> @NonNull FlowHandler<I, T> of(@NonNull ValueHandler<I, T> handler) {
            return handler.asFlow();
        }
    }

    @FunctionalInterface
    public interface ValueHandler<I, T> {

        @NonNull
        T parse(@NonNull I input) throws XMLStreamException;

        @NonNull
        default FlowHandler<I, T> asFlow() {
            return (input, onClose) -> {
                try (Closeable c = onClose) {
                    return parse(input);
                }
            };
        }
    }

    @Deprecated
    @FunctionalInterface
    public interface OutputHandler<O, T> {

        void format(@NonNull T value, @NonNull O output) throws Exception;

        default OutputHandler2<O, T> withEncoding() {
            return (t, o, e) -> this.format(t, o);
        }
    }

    @FunctionalInterface
    public interface OutputHandler2<O, T> {

        void format(@NonNull T value, @NonNull O output, @NonNull Charset encoding) throws Exception;
    }

    @lombok.With
    @lombok.Builder(toBuilder = true)
    public static final class StreamParser<T> implements Xml.Parser<T> {

        @StaticFactoryMethod
        public static <T> @NonNull StreamParser<T> flowOf(@NonNull FlowHandler<XMLStreamReader, T> handler) {
            return StreamParser.<T>builder().flow(handler).build();
        }

        @StaticFactoryMethod
        public static <T> @NonNull StreamParser<T> valueOf(@NonNull ValueHandler<XMLStreamReader, T> handler) {
            return flowOf(handler.asFlow());
        }

        // Fix lombok.Builder.Default bug in NetBeans
        @NonNull
        public static <T> Builder<T> builder() {
            return new Builder<T>()
                    .factory(XMLInputFactory::newFactory)
                    .ignoreXXE(false);
        }

        public final static class Builder<T> {

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
        private final IOSupplier<? extends XMLInputFactory> factory;

        @lombok.Getter
        private final boolean ignoreXXE;

        @Override
        public T parseFile(File source) throws IOException {
            LegacyFiles.checkSource(source);
            InputStream resource = LegacyFiles.newInputStream(source);
            return parse(o -> o.createXMLStreamReader(LegacyFiles.toSystemId(source), resource), resource);
        }

        @Override
        public T parseFile(File source, Charset encoding) throws IOException {
            LegacyFiles.checkSource(source);
            Objects.requireNonNull(encoding, "encoding");
            InputStream resource = LegacyFiles.newInputStream(source);
            return parse(o -> o.createXMLStreamReader(LegacyFiles.toSystemId(source), resource), resource);
        }

        @Override
        public T parseReader(IOSupplier<? extends Reader> source) throws IOException {
            Objects.requireNonNull(source, "source");
            Reader resource = LegacyFiles.checkResource(source.getWithIO(), "Missing Reader");
            return parse(o -> o.createXMLStreamReader(resource), resource);
        }

        @Override
        public T parseStream(IOSupplier<? extends InputStream> source) throws IOException {
            Objects.requireNonNull(source, "source");
            InputStream resource = LegacyFiles.checkResource(source.getWithIO(), "Missing InputStream");
            return parse(o -> o.createXMLStreamReader(resource), resource);
        }

        @Override
        public @NonNull T parseStream(IOSupplier<? extends InputStream> source, @NonNull Charset encoding) throws IOException {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(encoding, "encoding");
            InputStream resource = LegacyFiles.checkResource(source.getWithIO(), "Missing InputStream");
            return parse(o -> o.createXMLStreamReader(resource, encoding.name()), resource);
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

        @Override
        public T parseStream(InputStream resource, Charset encoding) throws IOException {
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(encoding, "encoding");
            return parse(o -> o.createXMLStreamReader(resource, encoding.name()), NOTHING_TO_CLOSE);
        }

        @NonNull
        public T parse(@NonNull XMLStreamReader input, @NonNull Closeable onClose) throws IOException {
            return doParse(handler, input, onClose);
        }

        private T parse(XFunction<XMLInputFactory, XMLStreamReader> supplier, Closeable onClose) throws IOException {
            try {
                XMLStreamReader input = supplier.apply(getInputEngine(factory, ignoreXXE));
                return parse(input, () -> closeBoth(input::close, onClose));
            } catch (XMLStreamException ex) {
                Resource.ensureClosed(ex, onClose);
                throw toIOException(ex);
            } catch (Error | RuntimeException | IOException ex) {
                Resource.ensureClosed(ex, onClose);
                throw ex;
            }
        }
    }

    @lombok.With
    @lombok.Builder(toBuilder = true)
    public static final class EventParser<T> implements Xml.Parser<T> {

        @StaticFactoryMethod
        public static <T> @NonNull EventParser<T> flowOf(@NonNull FlowHandler<XMLEventReader, T> handler) {
            return EventParser.<T>builder().flow(handler).build();
        }

        @StaticFactoryMethod
        public static <T> @NonNull EventParser<T> valueOf(@NonNull ValueHandler<XMLEventReader, T> handler) {
            return flowOf(handler.asFlow());
        }

        // Fix lombok.Builder.Default bug in NetBeans
        @NonNull
        public static <T> Builder<T> builder() {
            return new Builder<T>()
                    .factory(XMLInputFactory::newFactory)
                    .ignoreXXE(false);
        }

        public final static class Builder<T> {

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
        private final IOSupplier<? extends XMLInputFactory> factory;

        @lombok.Getter
        private final boolean ignoreXXE;

        @Override
        public T parseFile(File source) throws IOException {
            LegacyFiles.checkSource(source);
            InputStream resource = LegacyFiles.newInputStream(source);
            return parse(o -> o.createXMLEventReader(LegacyFiles.toSystemId(source), resource), resource);
        }

        @Override
        public T parseFile(File source, Charset encoding) throws IOException {
            LegacyFiles.checkSource(source);
            Objects.requireNonNull(encoding, "encoding");
            InputStream resource = LegacyFiles.newInputStream(source);
            return parse(o -> o.createXMLEventReader(LegacyFiles.toSystemId(source), resource), resource);
        }

        @Override
        public T parseReader(IOSupplier<? extends Reader> source) throws IOException {
            Objects.requireNonNull(source, "source");
            Reader resource = LegacyFiles.checkResource(source.getWithIO(), "Missing Reader");
            return parse(o -> o.createXMLEventReader(resource), resource);
        }

        @Override
        public T parseStream(IOSupplier<? extends InputStream> source) throws IOException {
            Objects.requireNonNull(source, "source");
            InputStream resource = LegacyFiles.checkResource(source.getWithIO(), "Missing InputStream");
            return parse(o -> o.createXMLEventReader(resource), resource);
        }

        @Override
        public T parseStream(IOSupplier<? extends InputStream> source, Charset encoding) throws IOException {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(encoding, "encoding");
            InputStream resource = LegacyFiles.checkResource(source.getWithIO(), "Missing InputStream");
            return parse(o -> o.createXMLEventReader(resource, encoding.name()), resource);
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

        @Override
        public T parseStream(InputStream resource, Charset encoding) throws IOException {
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(encoding, "encoding");
            return parse(o -> o.createXMLEventReader(resource, encoding.name()), NOTHING_TO_CLOSE);
        }

        private T parse(XMLEventReader input, Closeable onClose) throws IOException {
            return doParse(handler, input, onClose);
        }

        private T parse(XFunction<XMLInputFactory, XMLEventReader> supplier, Closeable onClose) throws IOException {
            try {
                XMLEventReader input = supplier.apply(getInputEngine(factory, ignoreXXE));
                return parse(input, () -> closeBoth(input::close, onClose));
            } catch (XMLStreamException ex) {
                Resource.ensureClosed(ex, onClose);
                throw toIOException(ex);
            } catch (Error | RuntimeException | IOException ex) {
                Resource.ensureClosed(ex, onClose);
                throw ex;
            }
        }
    }

    @lombok.With
    @lombok.Builder(toBuilder = true)
    public static final class StreamFormatter<T> implements Xml.Formatter<T> {

        @Deprecated
        @StaticFactoryMethod
        public static <T> @NonNull StreamFormatter<T> valueOf(@NonNull OutputHandler<XMLStreamWriter, T> handler) {
            return of(handler.withEncoding());
        }

        @StaticFactoryMethod
        public static <T> @NonNull StreamFormatter<T> of(@NonNull OutputHandler2<XMLStreamWriter, T> handler2) {
            return StreamFormatter.<T>builder().handler2(handler2).build();
        }

        // Fix lombok.Builder.Default bug in NetBeans
        @NonNull
        public static <T> Builder<T> builder() {
            return new Builder<T>()
                    .factory(XMLOutputFactory::newFactory)
                    .encoding(StandardCharsets.UTF_8);
        }

        public final static class Builder<T> {

            @Deprecated
            public Builder<T> handler(OutputHandler<XMLStreamWriter, T> handler) {
                return handler2(handler.withEncoding());
            }
        }

        @Deprecated
        public StreamFormatter<T> withHandler(OutputHandler<XMLStreamWriter, T> handler) {
            return withHandler2(handler.withEncoding());
        }

        @lombok.NonNull
        private final OutputHandler2<XMLStreamWriter, T> handler2;

        @lombok.NonNull
        private final IOSupplier<? extends XMLOutputFactory> factory;

        @lombok.NonNull
        private final Charset encoding;

        @Override
        public boolean isFormatted() {
            return false;
        }

        @Override
        public Charset getDefaultEncoding() {
            return encoding;
        }

        @Override
        public void formatFile(T value, File target) throws IOException {
            Objects.requireNonNull(value, "value");
            LegacyFiles.checkTarget(target);
            try (OutputStream resource = LegacyFiles.newOutputStream(target)) {
                format(value, o -> o.createXMLStreamWriter(resource, getDefaultEncoding().name()), getDefaultEncoding());
            }
        }

        @Override
        public void formatWriter(T value, IOSupplier<? extends Writer> target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            try (Writer resource = LegacyFiles.checkResource(target.getWithIO(), "Missing Writer")) {
                format(value, o -> o.createXMLStreamWriter(resource), getDefaultEncoding());
            }
        }

        @Override
        public void formatStream(T value, IOSupplier<? extends OutputStream> target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            try (OutputStream resource = LegacyFiles.checkResource(target.getWithIO(), "Missing OutputStream")) {
                format(value, o -> o.createXMLStreamWriter(resource, getDefaultEncoding().name()), getDefaultEncoding());
            }
        }

        @Override
        public void formatWriter(T value, Writer resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            format(value, o -> o.createXMLStreamWriter(resource), getDefaultEncoding());
        }

        @Override
        public void formatStream(T value, OutputStream resource) throws IOException {
            Objects.requireNonNull(value);
            Objects.requireNonNull(resource, "resource");
            format(value, o -> o.createXMLStreamWriter(resource, getDefaultEncoding().name()), getDefaultEncoding());
        }

        @Override
        public void formatStream(T value, OutputStream resource, Charset encoding) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(encoding, "encoding");
            format(value, o -> o.createXMLStreamWriter(resource, encoding.name()), encoding);
        }

        private void format(T value, XFunction<XMLOutputFactory, XMLStreamWriter> supplier, Charset realEncoding) throws IOException {
            try {
                XMLStreamWriter output = supplier.apply(getEngine());
                doFormat(handler2, value, output, realEncoding, () -> close(output::close));
                output.close();
            } catch (XMLStreamException ex) {
                throw toIOException(ex);
            }
        }

        private XMLOutputFactory getEngine() throws IOException {
            XMLOutputFactory result = factory.getWithIO();
            return result;
        }
    }

    @lombok.With
    @lombok.Builder(toBuilder = true)
    public static final class EventFormatter<T> implements Xml.Formatter<T> {

        @Deprecated
        @StaticFactoryMethod
        public static <T> @NonNull EventFormatter<T> valueOf(@NonNull OutputHandler<XMLEventWriter, T> handler) {
            return of(handler.withEncoding());
        }

        @StaticFactoryMethod
        public static <T> @NonNull EventFormatter<T> of(@NonNull OutputHandler2<XMLEventWriter, T> handler2) {
            return EventFormatter.<T>builder().handler2(handler2).build();
        }

        // Fix lombok.Builder.Default bug in NetBeans
        @NonNull
        public static <T> Builder<T> builder() {
            return new Builder<T>()
                    .factory(XMLOutputFactory::newFactory)
                    .encoding(StandardCharsets.UTF_8);
        }

        public final static class Builder<T> {

            @Deprecated
            public Builder<T> handler(OutputHandler<XMLEventWriter, T> handler) {
                return handler2(handler.withEncoding());
            }
        }

        @Deprecated
        public EventFormatter<T> withHandler(OutputHandler<XMLEventWriter, T> handler) {
            return withHandler2(handler.withEncoding());
        }

        @lombok.NonNull
        private final OutputHandler2<XMLEventWriter, T> handler2;

        @lombok.NonNull
        private final IOSupplier<? extends XMLOutputFactory> factory;

        @lombok.NonNull
        private final Charset encoding;

        @Override
        public boolean isFormatted() {
            return false;
        }

        @Override
        public Charset getDefaultEncoding() {
            return encoding;
        }

        @Override
        public void formatFile(T value, File target) throws IOException {
            Objects.requireNonNull(value, "value");
            LegacyFiles.checkTarget(target);
            try (OutputStream resource = LegacyFiles.newOutputStream(target)) {
                format(value, o -> o.createXMLEventWriter(resource, getDefaultEncoding().name()), getDefaultEncoding());
            }
        }

        @Override
        public void formatWriter(T value, IOSupplier<? extends Writer> target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            try (Writer resource = LegacyFiles.checkResource(target.getWithIO(), "Missing Writer")) {
                format(value, o -> o.createXMLEventWriter(resource), getDefaultEncoding());
            }
        }

        @Override
        public void formatStream(T value, IOSupplier<? extends OutputStream> target) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            try (OutputStream resource = LegacyFiles.checkResource(target.getWithIO(), "Missing OutputStream")) {
                format(value, o -> o.createXMLEventWriter(resource, getDefaultEncoding().name()), getDefaultEncoding());
            }
        }

        @Override
        public void formatWriter(T value, Writer resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            format(value, o -> o.createXMLEventWriter(resource), getDefaultEncoding());
        }

        @Override
        public void formatStream(T value, OutputStream resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            format(value, o -> o.createXMLEventWriter(resource, getDefaultEncoding().name()), getDefaultEncoding());
        }

        @Override
        public void formatStream(T value, OutputStream resource, Charset encoding) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(encoding, "encoding");
            format(value, o -> o.createXMLEventWriter(resource, encoding.name()), encoding);
        }

        private void format(T value, XFunction<XMLOutputFactory, XMLEventWriter> supplier, Charset realEncoding) throws IOException {
            try {
                XMLEventWriter output = supplier.apply(getEngine());
                doFormat(handler2, value, output, realEncoding, () -> close(output::close));
                output.close();
            } catch (XMLStreamException ex) {
                throw toIOException(ex);
            }
        }

        private XMLOutputFactory getEngine() throws IOException {
            XMLOutputFactory result = factory.getWithIO();
            return result;
        }
    }

    private XMLInputFactory getInputEngine(IOSupplier<? extends XMLInputFactory> factory, boolean ignoreXXE) throws IOException {
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
            Resource.ensureClosed(ex, onClose);
            throw toIOException(ex);
        } catch (Error | RuntimeException | IOException ex) {
            Resource.ensureClosed(ex, onClose);
            throw ex;
        }
    }

    private <T, OUTPUT> void doFormat(OutputHandler2<OUTPUT, T> handler2, T value, OUTPUT output, Charset encoding, Closeable onClose) throws IOException {
        try {
            handler2.format(value, output, encoding);
        } catch (Exception ex) {
            Resource.ensureClosed(ex, onClose);
            throw toIOException(ex);
        } catch (Error ex) {
            Resource.ensureClosed(ex, onClose);
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
            Resource.ensureClosed(ex, second);
            throw toIOException(ex);
        } catch (Error | RuntimeException ex) {
            Resource.ensureClosed(ex, second);
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

    private IOException toIOException(Exception ex) {
        if (ex instanceof XMLStreamException) {
            return toIOException((XMLStreamException) ex);
        }
        if (ex instanceof IOException) {
            return (IOException) ex;
        }
        return WrappedIOException.wrap(ex);
    }

    public IOException toIOException(XMLStreamException ex) {
        if (isEOF(ex)) {
            return new EOFException(Objects.toString(getFile(ex)));
        }
        return WrappedIOException.wrap(ex);
    }

    private boolean isEOF(XMLStreamException ex) {
        return ex.getLocation() != null && ex.getMessage() != null && ex.getMessage().contains("end of file");
    }

    private File getFile(XMLStreamException ex) {
        String result = ex.getLocation().getSystemId();
        return result != null && result.startsWith("file:/") ? LegacyFiles.fromSystemId(result) : null;
    }

    private final Closeable NOTHING_TO_CLOSE = IORunnable.noOp().asCloseable();
}
