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
import nbbrd.io.WrappedIOException;
import nbbrd.io.function.IORunnable;
import nbbrd.io.function.IOSupplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.logging.Level;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
@lombok.extern.java.Log
public class Sax {

    /**
     * Prevents XXE vulnerability by disabling features.
     *
     * @param reader non-null reader
     * @see <a href="https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#XMLReader">XXE</a>
     */
    public static void preventXXE(@NonNull XMLReader reader) {
        disableFeature(reader, XERCES_FEATURES_NONVALIDATING_LOAD_EXTERNAL_DTD);
        disableFeature(reader, SAX_FEATURES_EXTERNAL_GENERAL_ENTITIES);
        disableFeature(reader, SAX_FEATURES_EXTERNAL_PARAMETER_ENTITIES);
    }

    public static @NonNull XMLReader createReader() throws IOException {
        try {
            return DEFAULT_FACTORY.newSAXParser().getXMLReader();
        } catch (ParserConfigurationException ex) {
            throw wrapConfigException(ex);
        } catch (SAXException ex) {
            throw toIOException(ex);
        }
    }

    /**
     * Creates a new InputSource from a file.
     *
     * @param file a non-null file
     * @return a new InputSource
     * @see SAXParser#parse(java.io.File, org.xml.sax.helpers.DefaultHandler)
     */
    public static @NonNull InputSource newInputSource(@NonNull File file) {
        return new InputSource(LegacyFiles.toSystemId(file));
    }

    /**
     * Creates a new InputSource from a file.
     *
     * @param file     a non-null file
     * @param encoding a non-null encoding
     * @return a new InputSource
     * @see SAXParser#parse(java.io.File, org.xml.sax.helpers.DefaultHandler)
     */
    public static @NonNull InputSource newInputSource(@NonNull File file, @NonNull Charset encoding) {
        InputSource result = new InputSource(LegacyFiles.toSystemId(file));
        result.setEncoding(encoding.name());
        return result;
    }

    public static @NonNull IOException toIOException(@NonNull SAXException ex) {
        if (ex instanceof SAXParseException) {
            return wrapParseException((SAXParseException) ex);
        }
        return WrappedIOException.wrap(ex);
    }

    @lombok.With
    @lombok.Builder(toBuilder = true)
    public static final class Parser<T> implements Xml.Parser<T> {

        @StaticFactoryMethod
        public static <T> @NonNull Parser<T> of(@NonNull ContentHandler handler, IOSupplier<? extends T> after) {
            Parser.Builder<T> result = Parser.<T>builder().contentHandler(handler);
            if (handler instanceof DTDHandler) {
                result.dtdHandler((DTDHandler) handler);
            }
            if (handler instanceof EntityResolver) {
                result.entityResolver((EntityResolver) handler);
            }
            if (handler instanceof ErrorHandler) {
                result.errorHandler((ErrorHandler) handler);
            }
            return result.after(after).build();
        }

        public final static class Builder<T> {

        }

        @lombok.NonNull
        @lombok.Builder.Default
        private final IOSupplier<? extends XMLReader> factory = Sax::createReader;

        @lombok.NonNull
        private final ContentHandler contentHandler;

        @lombok.NonNull
        @lombok.Builder.Default
        private final DTDHandler dtdHandler = DEFAULT_HANDLER;

        @lombok.NonNull
        @lombok.Builder.Default
        private final EntityResolver entityResolver = DEFAULT_HANDLER;

        @lombok.NonNull
        @lombok.Builder.Default
        private final ErrorHandler errorHandler = DEFAULT_HANDLER;

        @lombok.NonNull
        @lombok.Builder.Default
        private final IORunnable before = IORunnable.noOp();

        @lombok.NonNull
        private final IOSupplier<? extends T> after;

        @lombok.Getter
        @lombok.Builder.Default
        private final boolean ignoreXXE = false;

        @Override
        public @NonNull T parseFile(@NonNull File source) throws IOException {
            LegacyFiles.checkSource(source);
            return parse(newInputSource(source));
        }

        @Override
        public @NonNull T parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(encoding, "encoding");
            LegacyFiles.checkSource(source);
            return parse(newInputSource(source, encoding));
        }

        @Override
        public @NonNull T parseReader(@NonNull Reader resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
            return parse(new InputSource(resource));
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
            return parse(new InputSource(resource));
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(encoding, "encoding");
            InputSource input = new InputSource(resource);
            input.setEncoding(encoding.name());
            return parse(input);
        }

        private T parse(InputSource input) throws IOException {
            XMLReader engine = factory.getWithIO();
            if (!ignoreXXE) {
                preventXXE(engine);
            }
            engine.setContentHandler(contentHandler);
            engine.setDTDHandler(dtdHandler);
            engine.setEntityResolver(entityResolver);
            engine.setErrorHandler(errorHandler);
            before.runWithIO();
            try {
                engine.parse(input);
            } catch (SAXException ex) {
                throw toIOException(ex);
            }
            return after.getWithIO();
        }
    }

    private final static SAXParserFactory DEFAULT_FACTORY = initFactory();
    private final static DefaultHandler DEFAULT_HANDLER = new DefaultHandler();

    private static SAXParserFactory initFactory() {
        SAXParserFactory result = SAXParserFactory.newInstance();
        result.setNamespaceAware(true);
        return result;
    }

    private static void disableFeature(XMLReader reader, String feature) {
        try {
            reader.setFeature(feature, false);
        } catch (SAXNotRecognizedException | SAXNotSupportedException ex) {
            log.log(Level.FINE, ex, () -> String.format("Failed to disable feature '%s'", feature));
        }
    }

    private static IOException wrapConfigException(ParserConfigurationException ex) {
        return WrappedIOException.wrap(ex);
    }

    private static IOException wrapParseException(SAXParseException ex) {
        if (isEOF(ex)) {
            return new EOFException(Objects.toString(getFileOrNull(ex)));
        }
        return WrappedIOException.wrap(ex);
    }

    private static boolean isEOF(SAXParseException ex) {
        return ex.getMessage() != null && ex.getMessage().contains("end of file");
    }

    private static File getFileOrNull(SAXParseException ex) {
        String result = ex.getSystemId();
        return result != null && result.startsWith("file:/") ? LegacyFiles.fromSystemId(result) : null;
    }

    private static final String SAX_FEATURES_EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String SAX_FEATURES_EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    private static final String XERCES_FEATURES_NONVALIDATING_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
}
