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
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.Resource;
import nbbrd.io.WrappedIOException;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IOSupplier;

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
    public static void preventXXE(@NonNull XMLInputFactory factory) {
        disableFeature(factory, XMLInputFactory.SUPPORT_DTD);
        disableFeature(factory, XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES);
    }

    public static @NonNull IOException toIOException(@NonNull XMLStreamException ex) {
        if (isEOF(ex)) {
            return new EOFException(Objects.toString(getFileOrNull(ex)));
        }
        return WrappedIOException.wrap(ex);
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
                try (Closeable ignored = onClose) {
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

        public final static class Builder<T> {

            public Builder<T> flow(FlowHandler<XMLStreamReader, T> handler) {
                return handler(handler);
            }

            public Builder<T> value(ValueHandler<XMLStreamReader, T> handler) {
                return handler(handler.asFlow());
            }
        }

        @NonNull
        private final FlowHandler<XMLStreamReader, T> handler;

        @NonNull
        @lombok.Builder.Default
        private final IOSupplier<? extends XMLInputFactory> factory = XMLInputFactory::newFactory;

        @lombok.Getter
        @lombok.Builder.Default
        private final boolean ignoreXXE = false;

        @Override
        public @NonNull T parseFile(@NonNull File source) throws IOException {
            InputStream resource = LegacyFiles.openInputStream(source);
            return parse(o -> o.createXMLStreamReader(LegacyFiles.toSystemId(source), resource), resource);
        }

        @Override
        public @NonNull T parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
            InputStream resource = LegacyFiles.openInputStream(source);
            return parse(o -> o.createXMLStreamReader(LegacyFiles.toSystemId(source), resource), resource);
        }

        @Override
        public @NonNull T parseReader(@NonNull IOSupplier<? extends Reader> source) throws IOException {
            Reader resource = LegacyFiles.openReader(source);
            return parse(o -> o.createXMLStreamReader(resource), resource);
        }

        @Override
        public @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source) throws IOException {
            InputStream resource = LegacyFiles.openInputStream(source);
            return parse(o -> o.createXMLStreamReader(resource), resource);
        }

        @Override
        public @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source, @NonNull Charset encoding) throws IOException {
            InputStream resource = LegacyFiles.openInputStream(source);
            return parse(o -> o.createXMLStreamReader(resource, encoding.name()), resource);
        }

        @Override
        public @NonNull T parseReader(@NonNull Reader resource) throws IOException {
            return parse(o -> o.createXMLStreamReader(resource), NOTHING_TO_CLOSE);
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource) throws IOException {
            return parse(o -> o.createXMLStreamReader(resource), NOTHING_TO_CLOSE);
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
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

        public final static class Builder<T> {

            public Builder<T> flow(FlowHandler<XMLEventReader, T> handler) {
                return handler(handler);
            }

            public Builder<T> value(ValueHandler<XMLEventReader, T> handler) {
                return handler(handler.asFlow());
            }
        }

        @NonNull
        private final FlowHandler<XMLEventReader, T> handler;

        @NonNull
        @lombok.Builder.Default
        private final IOSupplier<? extends XMLInputFactory> factory = XMLInputFactory::newFactory;

        @lombok.Getter
        @lombok.Builder.Default
        private final boolean ignoreXXE = false;

        @Override
        public @NonNull T parseFile(@NonNull File source) throws IOException {
            InputStream resource = LegacyFiles.openInputStream(source);
            return parse(o -> o.createXMLEventReader(LegacyFiles.toSystemId(source), resource), resource);
        }

        @Override
        public @NonNull T parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
            InputStream resource = LegacyFiles.openInputStream(source);
            return parse(o -> o.createXMLEventReader(LegacyFiles.toSystemId(source), resource), resource);
        }

        @Override
        public @NonNull T parseReader(@NonNull IOSupplier<? extends Reader> source) throws IOException {
            Reader resource = LegacyFiles.openReader(source);
            return parse(o -> o.createXMLEventReader(resource), resource);
        }

        @Override
        public @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source) throws IOException {
            InputStream resource = LegacyFiles.openInputStream(source);
            return parse(o -> o.createXMLEventReader(resource), resource);
        }

        @Override
        public @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source, @NonNull Charset encoding) throws IOException {
            InputStream resource = LegacyFiles.openInputStream(source);
            return parse(o -> o.createXMLEventReader(resource, encoding.name()), resource);
        }

        @Override
        public @NonNull T parseReader(@NonNull Reader resource) throws IOException {
            return parse(o -> o.createXMLEventReader(resource), NOTHING_TO_CLOSE);
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource) throws IOException {
            return parse(o -> o.createXMLEventReader(resource), NOTHING_TO_CLOSE);
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
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

        @NonNull
        private final OutputHandler2<XMLStreamWriter, T> handler2;

        @NonNull
        @lombok.Builder.Default
        private final IOSupplier<? extends XMLOutputFactory> factory = XMLOutputFactory::newFactory;

        @NonNull
        @lombok.Builder.Default
        private final Charset encoding = StandardCharsets.UTF_8;

        @Override
        public boolean isFormatted() {
            return false;
        }

        @Override
        public @NonNull Charset getDefaultEncoding() {
            return encoding;
        }

        @Override
        public void formatFile(@NonNull T value, @NonNull File target) throws IOException {
            try (OutputStream resource = LegacyFiles.openOutputStream(target)) {
                format(value, o -> o.createXMLStreamWriter(resource, getDefaultEncoding().name()), getDefaultEncoding());
            }
        }

        @Override
        public void formatWriter(@NonNull T value, @NonNull IOSupplier<? extends Writer> target) throws IOException {
            try (Writer resource = LegacyFiles.openWriter(target)) {
                format(value, o -> o.createXMLStreamWriter(resource), getDefaultEncoding());
            }
        }

        @Override
        public void formatStream(@NonNull T value, @NonNull IOSupplier<? extends OutputStream> target) throws IOException {
            try (OutputStream resource = LegacyFiles.openOutputStream(target)) {
                format(value, o -> o.createXMLStreamWriter(resource, getDefaultEncoding().name()), getDefaultEncoding());
            }
        }

        @Override
        public void formatWriter(@NonNull T value, @NonNull Writer resource) throws IOException {
            format(value, o -> o.createXMLStreamWriter(resource), getDefaultEncoding());
        }

        @Override
        public void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException {
            format(value, o -> o.createXMLStreamWriter(resource, getDefaultEncoding().name()), getDefaultEncoding());
        }

        @Override
        public void formatStream(@NonNull T value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException {
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
            return factory.getWithIO();
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

        @NonNull
        private final OutputHandler2<XMLEventWriter, T> handler2;

        @NonNull
        @lombok.Builder.Default
        private final IOSupplier<? extends XMLOutputFactory> factory = XMLOutputFactory::newFactory;

        @NonNull
        @lombok.Builder.Default
        private final Charset encoding = StandardCharsets.UTF_8;

        @Override
        public boolean isFormatted() {
            return false;
        }

        @Override
        public @NonNull Charset getDefaultEncoding() {
            return encoding;
        }

        @Override
        public void formatFile(@NonNull T value, @NonNull File target) throws IOException {
            try (OutputStream resource = LegacyFiles.openOutputStream(target)) {
                format(value, o -> o.createXMLEventWriter(resource, getDefaultEncoding().name()), getDefaultEncoding());
            }
        }

        @Override
        public void formatWriter(@NonNull T value, @NonNull IOSupplier<? extends Writer> target) throws IOException {
            try (Writer resource = LegacyFiles.openWriter(target)) {
                format(value, o -> o.createXMLEventWriter(resource), getDefaultEncoding());
            }
        }

        @Override
        public void formatStream(@NonNull T value, @NonNull IOSupplier<? extends OutputStream> target) throws IOException {
            try (OutputStream resource = LegacyFiles.openOutputStream(target)) {
                format(value, o -> o.createXMLEventWriter(resource, getDefaultEncoding().name()), getDefaultEncoding());
            }
        }

        @Override
        public void formatWriter(@NonNull T value, @NonNull Writer resource) throws IOException {
            format(value, o -> o.createXMLEventWriter(resource), getDefaultEncoding());
        }

        @Override
        public void formatStream(@NonNull T value, @NonNull OutputStream resource) throws IOException {
            format(value, o -> o.createXMLEventWriter(resource, getDefaultEncoding().name()), getDefaultEncoding());
        }

        @Override
        public void formatStream(@NonNull T value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException {
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
            return factory.getWithIO();
        }
    }

    private static XMLInputFactory getInputEngine(IOSupplier<? extends XMLInputFactory> factory, boolean ignoreXXE) throws IOException {
        XMLInputFactory result = factory.getWithIO();
        if (!ignoreXXE) {
            preventXXE(result);
        }
        return result;
    }

    private static <T, INPUT> T doParse(FlowHandler<INPUT, T> handler, INPUT input, Closeable onClose) throws IOException {
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

    private static <T, OUTPUT> void doFormat(OutputHandler2<OUTPUT, T> handler2, T value, OUTPUT output, Charset encoding, Closeable onClose) throws IOException {
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

    private static void close(XRunnable first) throws IOException {
        try {
            first.run();
        } catch (XMLStreamException ex) {
            throw toIOException(ex);
        }
    }

    private static void closeBoth(XRunnable first, Closeable second) throws IOException {
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

    private static void disableFeature(XMLInputFactory factory, String feature) {
        if (factory.isPropertySupported(feature)
                && ((Boolean) factory.getProperty(feature))) {
            factory.setProperty(feature, false);
        }
    }

    private static IOException toIOException(Exception ex) {
        if (ex instanceof XMLStreamException) {
            return toIOException((XMLStreamException) ex);
        }
        if (ex instanceof IOException) {
            return (IOException) ex;
        }
        return WrappedIOException.wrap(ex);
    }

    private static boolean isEOF(XMLStreamException ex) {
        return ex.getLocation() != null && ex.getMessage() != null && ex.getMessage().contains("end of file");
    }

    private static File getFileOrNull(XMLStreamException ex) {
        String result = ex.getLocation().getSystemId();
        return result != null && result.startsWith("file:/") ? LegacyFiles.fromSystemId(result) : null;
    }

    private static final Closeable NOTHING_TO_CLOSE = IORunnable.noOp().asCloseable();
}
