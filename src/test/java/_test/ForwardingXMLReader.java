/*
 * Copyright 2018 National Bank of Belgium
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
package _test;

import java.io.IOException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 *
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor
public class ForwardingXMLReader implements XMLReader {

    @lombok.experimental.Delegate
    private final XMLReader delegate;

    public ForwardingXMLReader onParse(SaxListener onParse) {
        return new ForwardingXMLReader(this) {
            @Override
            public void parse(InputSource input) throws IOException, SAXException {
                onParse.run();
                super.parse(input);
            }
        };
    }
}
