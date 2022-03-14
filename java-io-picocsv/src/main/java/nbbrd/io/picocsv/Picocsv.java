package nbbrd.io.picocsv;

import internal.io.text.LegacyFiles;
import nbbrd.io.text.TextBuffers;
import nbbrd.io.text.TextFormatter;
import nbbrd.io.text.TextParser;
import nbbrd.picocsv.Csv;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static nbbrd.io.text.TextResource.newBufferedReader;
import static nbbrd.io.text.TextResource.newBufferedWriter;

@lombok.experimental.UtilityClass
public class Picocsv {

    @FunctionalInterface
    public interface InputHandler<T> {

        @NonNull T parse(Csv.@NonNull Reader reader) throws IOException;
    }

    @lombok.Builder(toBuilder = true)
    public static final class Parser<T> implements TextParser<T> {

        public static <T> @NonNull Builder<T> builder(@NonNull InputHandler<T> handler) {
            return new Builder<T>().handler(handler);
        }

        @lombok.NonNull
        @lombok.Builder.Default
        private final Csv.Format format = Csv.Format.DEFAULT;

        @lombok.NonNull
        @lombok.Builder.Default
        private final Csv.ReaderOptions options = Csv.ReaderOptions.DEFAULT;

        @lombok.NonNull
        private final InputHandler<T> handler;

        @Override
        public @NonNull T parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(encoding, "encoding");
            LegacyFiles.checkSource(source);
            CharsetDecoder decoder = encoding.newDecoder();
            try (InputStream resource = LegacyFiles.newInputStream(source)) {
                return parse(newBufferedReader(resource, decoder), TextBuffers.of(source.toPath(), decoder));
            }
        }

        @Override
        public @NonNull T parsePath(@NonNull Path source, @NonNull Charset encoding) throws IOException {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(encoding, "encoding");
            checkIsFile(source);
            CharsetDecoder decoder = encoding.newDecoder();
            try (InputStream resource = Files.newInputStream(source)) {
                return parse(newBufferedReader(resource, decoder), TextBuffers.of(source, decoder));
            }
        }

        @Override
        public @NonNull T parseReader(@NonNull Reader resource) throws IOException {
            Objects.requireNonNull(resource, "resource");
            return parse(resource, TextBuffers.UNKNOWN);
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(encoding, "encoding");
            CharsetDecoder decoder = encoding.newDecoder();
            return parse(newBufferedReader(resource, decoder), TextBuffers.of(resource, decoder));
        }

        private T parse(Reader charReader, TextBuffers buffers) throws IOException {
            try (Csv.Reader csv = Csv.Reader.of(format, options, charReader, buffers.getCharBufferSize())) {
                return handler.parse(csv);
            }
        }

        public final static class Builder<T> {
        }
    }

    @FunctionalInterface
    public interface OutputHandler<T> {

        void format(@NonNull T value, Csv.@NonNull Writer writer) throws IOException;
    }

    @lombok.Builder(toBuilder = true)
    public static final class Formatter<T> implements TextFormatter<T> {

        public static <T> @NonNull Builder<T> builder(@NonNull OutputHandler<T> handler) {
            return new Builder<T>().handler(handler);
        }

        @lombok.NonNull
        @lombok.Builder.Default
        private final Csv.Format format = Csv.Format.DEFAULT;

        @lombok.NonNull
        @lombok.Builder.Default
        private final Csv.WriterOptions options = Csv.WriterOptions.DEFAULT;

        @lombok.NonNull
        private final OutputHandler<T> handler;

        @Override
        public void formatFile(@NonNull T value, @NonNull File target, @NonNull Charset encoding) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(encoding, "encoding");
            LegacyFiles.checkTarget(target);
            CharsetEncoder encoder = encoding.newEncoder();
            try (OutputStream resource = LegacyFiles.newOutputStream(target)) {
                format(value, newBufferedWriter(resource, encoder), TextBuffers.of(target.toPath(), encoder));
            }
        }

        @Override
        public void formatPath(@NonNull T value, @NonNull Path target, @NonNull Charset encoding) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(encoding, "encoding");
            checkIsFile(target);
            CharsetEncoder encoder = encoding.newEncoder();
            try (OutputStream resource = Files.newOutputStream(target)) {
                format(value, newBufferedWriter(resource, encoder), TextBuffers.of(target, encoder));
            }
        }

        @Override
        public void formatWriter(@NonNull T value, @NonNull Writer resource) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            format(value, resource, TextBuffers.UNKNOWN);
        }

        @Override
        public void formatStream(@NonNull T value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(encoding, "encoding");
            CharsetEncoder encoder = encoding.newEncoder();
            format(value, newBufferedWriter(resource, encoder), TextBuffers.of(resource, encoder));
        }

        private void format(T value, Writer charWriter, TextBuffers buffers) throws IOException {
            try (Csv.Writer csv = Csv.Writer.of(format, options, charWriter, buffers.getCharBufferSize())) {
                handler.format(value, csv);
            }
        }

        public final static class Builder<T> {
        }
    }

    private static void checkIsFile(@NonNull Path source) throws FileSystemException {
        if (Files.isDirectory(source)) {
            throw new AccessDeniedException(source.toString());
        }
    }
}
