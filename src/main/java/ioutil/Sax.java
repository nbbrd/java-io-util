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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Objects;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
@lombok.extern.java.Log
public class Sax {

    /**
     * Prevents XXE vulnerability by disabling features.
     *
     * @param reader non-null reader
     * @see
     * https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#XMLReader
     */
    public void preventXXE(@Nonnull XMLReader reader) {
//        setFeatureQuietly(reader, "http://apache.org/xml/features/disallow-doctype-decl", true);
        setFeatureQuietly(reader, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        setFeatureQuietly(reader, "http://xml.org/sax/features/external-general-entities", false);
        setFeatureQuietly(reader, "http://xml.org/sax/features/external-parameter-entities", false);
    }

    @Nonnull
    public static XMLReader createReader() throws IOException {
        try {
            return DEFAULT_FACTORY.newSAXParser().getXMLReader();
        } catch (ParserConfigurationException ex) {
            throw new Xml.WrappedException(ex);
        } catch (SAXException ex) {
            throw toIOException(ex);
        }
    }

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public static final class Parser<T> implements Xml.Parser<T> {

        @Nonnull
        public static <T> Parser<T> of(@Nonnull ContentHandler handler, @Nonnull IO.Supplier<? extends T> after) {
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

        public static class Builder<T> {

            Builder() {
                this.factory = Sax::createReader;
                this.contentHandler = null;
                this.dtdHandler = DEFAULT_HANDLER;
                this.entityResolver = DEFAULT_HANDLER;
                this.errorHandler = DEFAULT_HANDLER;
                this.before = IO.Runnable.noOp();
                this.after = null;
                this.ignoreXXE = false;
            }

            @Deprecated
            @Nonnull
            public Builder<T> handler(@Nonnull ContentHandler handler) {
                this.contentHandler = Objects.requireNonNull(handler);
                return this;
            }

            @Deprecated
            public Builder<T> preventXXE(boolean preventXXE) {
                this.ignoreXXE = !preventXXE;
                return this;
            }
        }

        @lombok.NonNull
        private final IO.Supplier<? extends XMLReader> factory;

        @lombok.NonNull
        private final ContentHandler contentHandler;

        @lombok.NonNull
        private final DTDHandler dtdHandler;

        @lombok.NonNull
        private final EntityResolver entityResolver;

        @lombok.NonNull
        private final ErrorHandler errorHandler;

        @lombok.NonNull
        private final IO.Runnable before;

        @lombok.NonNull
        private final IO.Supplier<? extends T> after;

        private final boolean ignoreXXE;

        @Override
        public T parseFile(File source) throws IOException {
            Xml.checkFile(source);
            return parse(newInputSource(source));
        }

        @Override
        public T parseReader(Reader resource) throws IOException {
            Objects.requireNonNull(resource);
            return parse(new InputSource(resource));
        }

        @Override
        public T parseStream(InputStream resource) throws IOException {
            Objects.requireNonNull(resource);
            return parse(new InputSource(resource));
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

        /**
         * Creates a new InputSource from a file.
         *
         * @param file
         * @return
         * @see SAXParser#parse(java.io.File,
         * org.xml.sax.helpers.DefaultHandler)
         */
        private InputSource newInputSource(File file) {
            return new InputSource(file.toURI().toASCIIString());
        }
    }

    private final static SAXParserFactory DEFAULT_FACTORY = initFactory();
    private final static DefaultHandler DEFAULT_HANDLER = new DefaultHandler();

    private static SAXParserFactory initFactory() {
        SAXParserFactory result = SAXParserFactory.newInstance();
        result.setNamespaceAware(true);
        return result;
    }

    private void setFeatureQuietly(XMLReader reader, String feature, boolean value) {
        try {
            reader.setFeature(feature, value);
        } catch (SAXNotRecognizedException | SAXNotSupportedException ex) {
            log.log(Level.FINE, ex, () -> String.format("Failed to set feature '%s' to '%s'", feature, value));
        }
    }

    IOException toIOException(SAXException ex) {
        if (ex instanceof SAXParseException) {
            return toIOException((SAXParseException) ex);
        }
        return new Xml.WrappedException(ex);
    }

    private IOException toIOException(SAXParseException ex) {
        if (isEOF(ex)) {
            return new EOFException(getFile(ex));
        }
        return new Xml.WrappedException(ex);
    }

    private boolean isEOF(SAXParseException ex) {
        return ex.getLineNumber() == 1 && ex.getColumnNumber() == 1;
    }

    private String getFile(SAXParseException ex) {
        return ex.getSystemId();
    }
}
