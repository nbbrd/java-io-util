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
package test;

import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 *
 * @author Philippe Charles
 * @param <T>
 */
@lombok.AllArgsConstructor(staticName = "of")
public class Meta<T> {

    @lombok.Getter
    private final String name;

    @lombok.Getter
    private final boolean invalid;

    @lombok.Getter
    private final T target;

    @Override
    public String toString() {
        return name;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {

        private final ImmutableList.Builder<Meta<T>> result = ImmutableList.builder();

        public Builder<T> valid(String name, T target) {
            return of(name, false, target);
        }

        public Builder<T> invalid(String name, T target) {
            return of(name, true, target);
        }

        public Builder<T> of(String name, boolean invalid, T target) {
            result.add(Meta.of(name, invalid, target));
            return this;
        }

        public List<Meta<T>> build() {
            return result.build();
        }
    }
}
