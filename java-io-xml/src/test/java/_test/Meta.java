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
package _test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 *
 * @author Philippe Charles
 * @param <T>
 */
@lombok.AllArgsConstructor
public class Meta<T> {

    @lombok.Getter
    private final String name;

    @lombok.Getter
    private final Class<? extends Throwable> expectedException;

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

        private final List<Meta<T>> result = new ArrayList<>();
        private String group = "";

        private Builder<T> addItem(String name, T target, Class<? extends Throwable> expectedException) {
            result.add(new Meta<>(group + (group.isEmpty() ? "" : "/") + name, expectedException, target));
            return this;
        }

        public Builder<T> group(String group) {
            this.group = group;
            return this;
        }

        public Builder<T> valid(String name, T target) {
            return of(name, false, target);
        }

        public Builder<T> invalid(String name, T target) {
            return of(name, true, target);
        }

        public Builder<T> invalid(String name, T target, Class<? extends Throwable> expectedException) {
            return addItem(name, target, expectedException);
        }

        public Builder<T> of(String name, boolean invalid, T target) {
            return addItem(name, target, invalid ? Throwable.class : null);
        }

        public CodeStep<T> code() {
            return new CodeStep<>(this);
        }

        public ExceptionStep<T> exception(Class<? extends Throwable> ex) {
            return new ExceptionStep<>(ex, this);
        }

        public List<Meta<T>> build() {
            return Collections.unmodifiableList(new ArrayList<>(result));
        }
    }

    @lombok.RequiredArgsConstructor
    public static final class CodeStep<T> {

        private final Builder<T> builder;
        private String name = "";

        public CodeStep<T> as(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> xxx() {
            return builder;
        }

        public Builder<T> doesNotRaiseExceptionWhen(T target) {
            return builder.valid(name, target);
        }
    }

    @lombok.RequiredArgsConstructor
    public static final class ExceptionStep<T> {

        private final Class<? extends Throwable> ex;
        private final Builder<T> builder;
        private String name = "";

        public ExceptionStep<T> as(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> isThrownBy(T target) {
            return builder.invalid(name, target, ex);
        }
    }

    public static Class<? extends Throwable> lookupExpectedException(Meta... list) {
        return Stream.of(list).map(Meta::getExpectedException).filter(Objects::nonNull).findFirst().orElse(null);
    }
}
