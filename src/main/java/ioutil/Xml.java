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

import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLInputFactory;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class Xml {

    @lombok.experimental.UtilityClass
    public static class StAX {

        /**
         * Prevent XXE vulnerability by disabling features.
         *
         * @param factory
         * @see
         * https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#XMLInputFactory_.28a_StAX_parser.29
         */
        public void preventXXE(@Nonnull XMLInputFactory factory) {
            if (factory.isPropertySupported(XMLInputFactory.SUPPORT_DTD)) {
                factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            }
            if (factory.isPropertySupported(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)) {
                factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            }
        }
    }

    @lombok.experimental.UtilityClass
    @lombok.extern.java.Log
    public static class SAX {

        /**
         * Prevent XXE vulnerability by disabling features.
         *
         * @param reader
         * @see
         * https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#XMLReader
         */
        public void preventXXE(@Nonnull XMLReader reader) {
            setFeatureQuietly(reader, "http://apache.org/xml/features/disallow-doctype-decl", true);
            setFeatureQuietly(reader, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            setFeatureQuietly(reader, "http://xml.org/sax/features/external-general-entities", false);
            setFeatureQuietly(reader, "http://xml.org/sax/features/external-parameter-entities", false);
        }

        private void setFeatureQuietly(XMLReader reader, String feature, boolean value) {
            try {
                reader.setFeature(feature, value);
            } catch (SAXNotRecognizedException | SAXNotSupportedException ex) {
                log.log(Level.FINE, ex, () -> String.format("Failed to set feature '%s' to '%s'", feature, value));
            }
        }
    }
}
