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

import javax.xml.stream.XMLInputFactory;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * @author Philippe Charles
 */
public class XmlTest {

    @Test
    @SuppressWarnings("null")
    public void testXmlStAXPreventXXE() {
        assertThatNullPointerException().isThrownBy(() -> Xml.StAX.preventXXE(null));
        assertThatCode(() -> Xml.StAX.preventXXE(XMLInputFactory.newFactory())).doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("null")
    public void testXmlSAXPreventXXE() {
        assertThatNullPointerException().isThrownBy(() -> Xml.SAX.preventXXE(null));
        assertThatCode(() -> Xml.SAX.preventXXE(XMLReaderFactory.createXMLReader())).doesNotThrowAnyException();
    }
}
