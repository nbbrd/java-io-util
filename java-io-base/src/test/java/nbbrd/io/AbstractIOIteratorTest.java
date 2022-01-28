package nbbrd.io;

import _test.io.IOIteratorAssertions;
import _test.io.IOIteratorFactory;
import java.io.IOException;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.assertThatIOException;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Philippe Charles
 */
public class AbstractIOIteratorTest {

    private static class Sample extends AbstractIOIterator<Integer> {

        int i = 0;

        @Override
        protected Integer get() throws IOException {
            return i - 1;
        }

        @Override
        protected boolean moveNext() throws IOException {
            if (i < 3) {
                i++;
                return true;
            }
            return false;
        }
    }

    @Test
    public void test() throws IOException {
        IOIteratorAssertions.assertApi(Sample::new);
        IOIteratorAssertions.assertContent(Sample::new, 0, 1, 2);
    }

    @Test
    public void testGet() throws IOException {
        Supplier<IOIterator<Integer>> factory = () -> new Sample() {
            @Override
            protected Integer get() throws IOException {
                throw new IOException();
            }
        };

        assertThatIOException()
                .isThrownBy(() -> IOIteratorFactory.browseNext(factory.get()));
    }

    @Test
    public void testGetNull() throws IOException {
        Supplier<IOIterator<Integer>> factory = () -> new Sample() {
            @Override
            protected Integer get() throws IOException {
                return null;
            }
        };

        IOIteratorAssertions.assertApi(factory);
        IOIteratorAssertions.assertContent(factory, null, null, null);
    }

    @Test
    public void testMoveNext() throws IOException {
        Supplier<IOIterator<Integer>> factory = () -> new Sample() {
            @Override
            protected boolean moveNext() throws IOException {
                throw new IOException();
            }
        };

        assertThatIOException()
                .isThrownBy(() -> IOIteratorFactory.browseNext(factory.get()));
    }
}
