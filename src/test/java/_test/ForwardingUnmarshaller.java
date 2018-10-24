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

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamReader;
import org.xml.sax.InputSource;

/**
 *
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor
public class ForwardingUnmarshaller implements Unmarshaller {

    @lombok.experimental.Delegate
    private final Unmarshaller delegate;

    public ForwardingUnmarshaller onUnmarshal(JaxbListener onUnmarshal) {
        return new ForwardingUnmarshaller(this) {
            @Override
            public Object unmarshal(XMLStreamReader reader) throws JAXBException {
                onUnmarshal.run();
                return super.unmarshal(reader);
            }

            @Override
            public Object unmarshal(Reader reader) throws JAXBException {
                onUnmarshal.run();
                return super.unmarshal(reader);
            }

            @Override
            public Object unmarshal(InputStream is) throws JAXBException {
                onUnmarshal.run();
                return super.unmarshal(is);
            }

            @Override
            public Object unmarshal(File file) throws JAXBException {
                onUnmarshal.run();
                return super.unmarshal(file);
            }

            @Override
            public Object unmarshal(InputSource source) throws JAXBException {
                onUnmarshal.run();
                return super.unmarshal(source);
            }
        };
    }
}
