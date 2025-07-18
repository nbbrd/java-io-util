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

import internal.io.InternalResource;
import internal.io.text.InternalTextResource;
import internal.io.text.LegacyFiles;
import lombok.NonNull;
import nbbrd.design.StaticFactoryMethod;
import nbbrd.io.Resource;
import nbbrd.io.WrappedIOException;
import nbbrd.io.function.IOFunction;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IOSupplier;

import javax.xml.stream.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static nbbrd.io.Resource.uncloseableInputStream;
import static nbbrd.io.text.TextResource.uncloseableReader;

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
        return wrapXMLStreamException(ex);
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
        private final IOSupplier<? extends XMLInputFactory> factory = XMLInputFactory::newInstance;

        @lombok.Getter
        @lombok.Builder.Default
        private final boolean ignoreXXE = false;

        @Override
        public @NonNull T parseFile(@NonNull File source) throws IOException {
            BufferedInputStream bufferedResource = new BufferedInputStream(LegacyFiles.newInputStream(source));
            return doParseOrClose(o -> o.createXMLStreamReader(LegacyFiles.toSystemId(source), uncloseableInputStream(bufferedResource)), bufferedResource);
        }

        @Override
        public @NonNull T parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
            BufferedInputStream bufferedResource = new BufferedInputStream(LegacyFiles.newInputStream(source));
            return doParseOrClose(o -> o.createXMLStreamReader(LegacyFiles.toSystemId(source), uncloseableInputStream(bufferedResource)), bufferedResource);
        }

        @Override
        public @NonNull T parseReader(@NonNull IOSupplier<? extends Reader> source) throws IOException {
            Reader resource = InternalTextResource.openReader(source);
            return doParseOrClose(o -> o.createXMLStreamReader(uncloseableReader(resource)), resource);
        }

        @Override
        public @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source) throws IOException {
            InputStream resource = InternalResource.openInputStream(source);
            return doParseOrClose(o -> o.createXMLStreamReader(uncloseableInputStream(resource)), resource);
        }

        @Override
        public @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source, @NonNull Charset encoding) throws IOException {
            InputStream resource = InternalResource.openInputStream(source);
            return doParseOrClose(o -> o.createXMLStreamReader(uncloseableInputStream(resource), encoding.name()), resource);
        }

        @Override
        public @NonNull T parseReader(@NonNull Reader resource) throws IOException {
            return doParseOrClose(o -> o.createXMLStreamReader(uncloseableReader(resource)), NOTHING_TO_CLOSE);
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource) throws IOException {
            return doParseOrClose(o -> o.createXMLStreamReader(uncloseableInputStream(resource)), NOTHING_TO_CLOSE);
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
            return doParseOrClose(o -> o.createXMLStreamReader(uncloseableInputStream(resource), encoding.name()), NOTHING_TO_CLOSE);
        }

        @NonNull
        public T parse(@NonNull XMLStreamReader input, @NonNull Closeable onClose) throws IOException {
            return doParse(handler, input, onClose);
        }

        private T doParseOrClose(StaxFunction<XMLInputFactory, XMLStreamReader> supplier, Closeable inputSource) throws IOException {
            try {
                XMLStreamReader input = asIOFunction(supplier).applyWithIO(getEngine());
                return doParse(handler, input, closingReaderAndSource(input, inputSource));
            } catch (Error | RuntimeException | IOException ex) {
                Resource.ensureClosed(ex, inputSource);
                throw ex;
            }
        }

        // XMLStreamReader#close() should not close the underlying input source !
        private static Closeable closingReaderAndSource(XMLStreamReader reader, Closeable inputSource) {
            return () -> Resource.closeBoth(() -> closeWithIO(reader), inputSource);
        }

        private static void closeWithIO(XMLStreamReader reader) throws IOException {
            try {
                reader.close();
            } catch (XMLStreamException ex) {
                throw wrapXMLStreamException(ex);
            }
        }

        private XMLInputFactory getEngine() throws IOException {
            return getInputEngine(factory, ignoreXXE);
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
        private final IOSupplier<? extends XMLInputFactory> factory = XMLInputFactory::newInstance;

        @lombok.Getter
        @lombok.Builder.Default
        private final boolean ignoreXXE = false;

        @Override
        public @NonNull T parseFile(@NonNull File source) throws IOException {
            BufferedInputStream bufferedResource = new BufferedInputStream(LegacyFiles.newInputStream(source));
            return doParseOrClose(o -> o.createXMLEventReader(LegacyFiles.toSystemId(source), uncloseableInputStream(bufferedResource)), bufferedResource);
        }

        @Override
        public @NonNull T parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
            BufferedInputStream bufferedResource = new BufferedInputStream(LegacyFiles.newInputStream(source));
            return doParseOrClose(o -> o.createXMLEventReader(LegacyFiles.toSystemId(source), uncloseableInputStream(bufferedResource)), bufferedResource);
        }

        @Override
        public @NonNull T parseReader(@NonNull IOSupplier<? extends Reader> source) throws IOException {
            Reader resource = InternalTextResource.openReader(source);
            return doParseOrClose(o -> o.createXMLEventReader(uncloseableReader(resource)), resource);
        }

        @Override
        public @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source) throws IOException {
            InputStream resource = InternalResource.openInputStream(source);
            return doParseOrClose(o -> o.createXMLEventReader(uncloseableInputStream(resource)), resource);
        }

        @Override
        public @NonNull T parseStream(@NonNull IOSupplier<? extends InputStream> source, @NonNull Charset encoding) throws IOException {
            InputStream resource = InternalResource.openInputStream(source);
            return doParseOrClose(o -> o.createXMLEventReader(uncloseableInputStream(resource), encoding.name()), resource);
        }

        @Override
        public @NonNull T parseReader(@NonNull Reader resource) throws IOException {
            return doParseOrClose(o -> o.createXMLEventReader(uncloseableReader(resource)), NOTHING_TO_CLOSE);
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource) throws IOException {
            return doParseOrClose(o -> o.createXMLEventReader(uncloseableInputStream(resource)), NOTHING_TO_CLOSE);
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
            return doParseOrClose(o -> o.createXMLEventReader(uncloseableInputStream(resource), encoding.name()), NOTHING_TO_CLOSE);
        }

        private T doParseOrClose(StaxFunction<XMLInputFactory, XMLEventReader> supplier, Closeable inputSource) throws IOException {
            try {
                XMLEventReader input = asIOFunction(supplier).applyWithIO(getEngine());
                return doParse(handler, input, closingReaderAndSource(input, inputSource));
            } catch (Error | RuntimeException | IOException ex) {
                Resource.ensureClosed(ex, inputSource);
                throw ex;
            }
        }

        // XmlEventReader#close() should not close the underlying input source !
        private static Closeable closingReaderAndSource(XMLEventReader reader, Closeable inputSource) {
            return () -> Resource.closeBoth(() -> closeWithIO(reader), inputSource);
        }

        private static void closeWithIO(XMLEventReader reader) throws IOException {
            try {
                reader.close();
            } catch (XMLStreamException ex) {
                throw wrapXMLStreamException(ex);
            }
        }

        private XMLInputFactory getEngine() throws IOException {
            return getInputEngine(factory, ignoreXXE);
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
            try (BufferedOutputStream bufferedResource = new BufferedOutputStream(LegacyFiles.newOutputStream(target))) {
                format(value, o -> o.createXMLStreamWriter(bufferedResource, getDefaultEncoding().name()), getDefaultEncoding());
            }
        }

        @Override
        public void formatWriter(@NonNull T value, @NonNull IOSupplier<? extends Writer> target) throws IOException {
            try (Writer resource = InternalTextResource.openWriter(target)) {
                format(value, o -> o.createXMLStreamWriter(resource), getDefaultEncoding());
            }
        }

        @Override
        public void formatStream(@NonNull T value, @NonNull IOSupplier<? extends OutputStream> target) throws IOException {
            try (OutputStream resource = InternalResource.openOutputStream(target)) {
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

        private void format(T value, StaxFunction<XMLOutputFactory, XMLStreamWriter> supplier, Charset realEncoding) throws IOException {
            try {
                XMLStreamWriter output = supplier.applyWithXMLStream(getEngine());
                doFormat(handler2, value, output, realEncoding, () -> closeWithIO(output));
                output.close();
            } catch (XMLStreamException ex) {
                throw wrapXMLStreamException(ex);
            }
        }

        private static void closeWithIO(XMLStreamWriter writer) throws IOException {
            try {
                writer.close();
            } catch (XMLStreamException ex) {
                throw wrapXMLStreamException(ex);
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
            try (BufferedOutputStream bufferedResource = new BufferedOutputStream(LegacyFiles.newOutputStream(target))) {
                format(value, o -> o.createXMLEventWriter(bufferedResource, getDefaultEncoding().name()), getDefaultEncoding());
            }
        }

        @Override
        public void formatWriter(@NonNull T value, @NonNull IOSupplier<? extends Writer> target) throws IOException {
            try (Writer resource = InternalTextResource.openWriter(target)) {
                format(value, o -> o.createXMLEventWriter(resource), getDefaultEncoding());
            }
        }

        @Override
        public void formatStream(@NonNull T value, @NonNull IOSupplier<? extends OutputStream> target) throws IOException {
            try (OutputStream resource = InternalResource.openOutputStream(target)) {
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

        private void format(T value, StaxFunction<XMLOutputFactory, XMLEventWriter> supplier, Charset realEncoding) throws IOException {
            try {
                XMLEventWriter output = supplier.applyWithXMLStream(getEngine());
                doFormat(handler2, value, output, realEncoding, () -> closeWithIO(output));
                output.close();
            } catch (XMLStreamException ex) {
                throw wrapXMLStreamException(ex);
            }
        }

        private static void closeWithIO(XMLEventWriter writer) throws IOException {
            try {
                writer.close();
            } catch (XMLStreamException ex) {
                throw wrapXMLStreamException(ex);
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
            throw wrapXMLStreamException(ex);
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
            throw wrapException(ex);
        } catch (Error ex) {
            Resource.ensureClosed(ex, onClose);
            throw ex;
        }
    }

    private static <T, R> IOFunction<T, R> asIOFunction(StaxFunction<T, R> function) {
        return t -> {
            try {
                return function.applyWithXMLStream(t);
            } catch (XMLStreamException ex) {
                throw wrapXMLStreamException(ex);
            }
        };
    }

    @FunctionalInterface
    private interface StaxFunction<T, R> {

        R applyWithXMLStream(T t) throws XMLStreamException;
    }

    private static void disableFeature(XMLInputFactory factory, String feature) {
        if (factory.isPropertySupported(feature)
                && ((Boolean) factory.getProperty(feature))) {
            factory.setProperty(feature, false);
        }
    }

    private static IOException wrapException(Exception ex) {
        if (ex instanceof XMLStreamException) {
            return wrapXMLStreamException((XMLStreamException) ex);
        }
        if (ex instanceof IOException) {
            return (IOException) ex;
        }
        return WrappedIOException.wrap(ex);
    }

    private static IOException wrapXMLStreamException(XMLStreamException ex) {
        if (StaxEOF.isEOF(ex)) {
            return new EOFException(Objects.toString(getFileOrNull(ex)));
        }
        return WrappedIOException.wrap(ex);
    }

    private static File getFileOrNull(XMLStreamException ex) {
        Location location = ex.getLocation();
        if (location == null) return null;
        String systemId = location.getSystemId();
        if (systemId == null) return null;
        return LegacyFiles.fromSystemId(systemId);
    }

    private static final Closeable NOTHING_TO_CLOSE = IORunnable.noOp().asCloseable();

    private static final class StaxEOF {

        public static boolean isEOF(XMLStreamException ex) {
            return ex.getLocation() != null && isEOFMessage(ex.getMessage());
        }

        private static boolean isEOFMessage(String message) {
            return message.contains(EOF_MESSAGE_BY_LOCALE.computeIfAbsent(Locale.getDefault(), StaxEOF::loadEOFMessage));
        }

        private static String loadEOFMessage(Locale locale) {
            try {
                parseEmptyContent(locale);
            } catch (XMLStreamException e) {
                return extractEOFMessage(e);
            }
            return "Premature end of file.";
        }

        private static void parseEmptyContent(Locale ignore) throws XMLStreamException {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(""));
            try {
                while (reader.hasNext()) {
                    reader.next();
                }
            } finally {
                reader.close();
            }
        }

        private static String extractEOFMessage(XMLStreamException e) {
            String text = e.getMessage();
            int index = text.indexOf(EOF_MESSAGE_PREFIX);
            return index != -1 ? text.substring(index + EOF_MESSAGE_PREFIX.length()) : text;
        }

        private static final String EOF_MESSAGE_PREFIX = "Message: ";

        private static final ConcurrentMap<Locale, String> EOF_MESSAGE_BY_LOCALE = new ConcurrentHashMap<>();
    }
}
