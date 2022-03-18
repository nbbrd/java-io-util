package nbbrd.io.text;

import lombok.NonNull;
import nbbrd.design.MightBePromoted;
import nbbrd.design.SealedType;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SealedType({
        Property.class,
        BooleanProperty.class,
        IntProperty.class,
        LongProperty.class
})
public abstract class BaseProperty implements CharSequence {

    abstract public @NonNull String getKey();

    @Override
    public int length() {
        return getKey().length();
    }

    @Override
    public char charAt(int index) {
        return getKey().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return getKey().subSequence(start, end);
    }

    @Override
    public IntStream chars() {
        return getKey().chars();
    }

    @Override
    public IntStream codePoints() {
        return getKey().codePoints();
    }

    @Override
    public String toString() {
        return getKey();
    }

    public static @NonNull List<String> keysOf(@NonNull BaseProperty... properties) {
        return Stream.of(properties)
                .map(BaseProperty::getKey)
                .collect(toUnmodifiableList());
    }

    @MightBePromoted
    private static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList);
    }
}
