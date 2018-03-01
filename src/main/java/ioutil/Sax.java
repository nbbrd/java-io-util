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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Objects;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

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
            return XMLReaderFactory.createXMLReader();
        } catch (SAXException ex) {
            throw new Xml.WrappedException(ex);
        }
    }

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public static final class Parser<T> implements Xml.Parser<T> {

        @Nonnull
        public static <T> Parser<T> of(@Nonnull ContentHandler handler, @Nonnull IO.Supplier<? extends T> after) {
            return Parser.<T>builder().handler(handler).after(after).build();
        }

        @lombok.NonNull
        @lombok.Builder.Default
        private final IO.Supplier<? extends XMLReader> factory = Sax::createReader;

        @lombok.NonNull
        private final ContentHandler handler;

        @lombok.NonNull
        @lombok.Builder.Default
        private final IO.Runnable before = IO.Runnable.noOp();

        @lombok.NonNull
        private final IO.Supplier<? extends T> after;

        @lombok.Builder.Default
        private boolean preventXXE = true;

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
            if (preventXXE) {
                preventXXE(engine);
            }
            engine.setContentHandler(handler);
            before.runWithIO();
            try {
                engine.parse(input);
            } catch (SAXException ex) {
                throw new Xml.WrappedException(ex);
            }
            return after.getWithIO();
        }
    }

    private void setFeatureQuietly(XMLReader reader, String feature, boolean value) {
        try {
            reader.setFeature(feature, value);
        } catch (SAXNotRecognizedException | SAXNotSupportedException ex) {
            log.log(Level.FINE, ex, () -> String.format("Failed to set feature '%s' to '%s'", feature, value));
        }
    }
}
