/*
 * Copyright 2019 National Bank of Belgium
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

module nbbrd.io.base {

    requires static org.checkerframework.checker.qual;
    requires static lombok;
    requires static nbbrd.design;

    exports nbbrd.io;
    exports nbbrd.io.function;
    exports nbbrd.io.net;
    exports nbbrd.io.sys;
    exports nbbrd.io.text;
    exports nbbrd.io.zip;

    exports internal.io.text to nbbrd.io.xml, nbbrd.io.xml.bind, nbbrd.io.picocsv;
}
