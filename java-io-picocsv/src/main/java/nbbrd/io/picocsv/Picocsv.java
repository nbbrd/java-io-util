package nbbrd.io.picocsv;

import internal.io.text.LegacyFiles;
import lombok.NonNull;
import nbbrd.io.function.IOSupplier;
import nbbrd.io.net.MediaType;
import nbbrd.io.text.TextBuffers;
import nbbrd.io.text.TextFormatter;
import nbbrd.io.text.TextParser;
import nbbrd.picocsv.Csv;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

import static nbbrd.io.text.TextResource.newBufferedReader;
import static nbbrd.io.text.TextResource.newBufferedWriter;

@lombok.experimental.UtilityClass
public class Picocsv {

    public static final MediaType CSV_UTF_8 = MediaType.builder().type("text").subtype("csv").build().withCharset(StandardCharsets.UTF_8);

    @FunctionalInterface
    public interface InputHandler<T> {

        @NonNull T parse(Csv.@NonNull Reader reader) throws IOException;
    }

    @lombok.Builder(toBuilder = true)
    public static final class Parser<T> implements TextParser<T> {

        public static <T> @NonNull Builder<T> builder(@NonNull InputHandler<T> handler) {
            return new Builder<T>().handler(handler);
        }

        @NonNull
        @lombok.Getter
        @lombok.Builder.Default
        private final Csv.Format format = Csv.Format.DEFAULT;

        @NonNull
        @lombok.Getter
        @lombok.Builder.Default
        private final Csv.ReaderOptions options = Csv.ReaderOptions.DEFAULT;

        @NonNull
        private final InputHandler<T> handler;

        @Override
        public @NonNull T parseFile(@NonNull File source, @NonNull Charset encoding) throws IOException {
            LegacyFiles.checkSource(source);
            CharsetDecoder decoder = encoding.newDecoder();
            try (InputStream resource = LegacyFiles.newInputStream(source)) {
                return parse(newBufferedReader(resource, decoder), TextBuffers.of(source.toPath(), decoder));
            }
        }

        @Override
        public @NonNull T parsePath(@NonNull Path source, @NonNull Charset encoding) throws IOException {
            checkIsFile(source);
            CharsetDecoder decoder = encoding.newDecoder();
            try (InputStream resource = Files.newInputStream(source)) {
                return parse(newBufferedReader(resource, decoder), TextBuffers.of(source, decoder));
            }
        }

        @Override
        public @NonNull T parseReader(@NonNull Reader resource) throws IOException {
            return parse(resource, TextBuffers.UNKNOWN);
        }

        @Override
        public @NonNull T parseStream(@NonNull InputStream resource, @NonNull Charset encoding) throws IOException {
            CharsetDecoder decoder = encoding.newDecoder();
            return parse(newBufferedReader(resource, decoder), TextBuffers.of(resource, decoder));
        }

        public @NonNull T parseCsv(IOSupplier<Csv.@NonNull Reader> source) throws IOException {
            try (Csv.Reader resource = source.getWithIO()) {
                return parseCsv(resource);
            }
        }

        public @NonNull T parseCsv(Csv.@NonNull Reader resource) throws IOException {
            return handler.parse(resource);
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

        @NonNull
        @lombok.Getter
        @lombok.Builder.Default
        private final Csv.Format format = Csv.Format.DEFAULT;

        @NonNull
        @lombok.Getter
        @lombok.Builder.Default
        private final Csv.WriterOptions options = Csv.WriterOptions.DEFAULT;

        @NonNull
        private final OutputHandler<T> handler;

        @Override
        public void formatFile(@NonNull T value, @NonNull File target, @NonNull Charset encoding) throws IOException {
            LegacyFiles.checkTarget(target);
            CharsetEncoder encoder = encoding.newEncoder();
            try (OutputStream resource = LegacyFiles.newOutputStream(target)) {
                format(value, newBufferedWriter(resource, encoder), TextBuffers.of(target.toPath(), encoder));
            }
        }

        @Override
        public void formatPath(@NonNull T value, @NonNull Path target, @NonNull Charset encoding) throws IOException {
            checkIsFile(target);
            CharsetEncoder encoder = encoding.newEncoder();
            try (OutputStream resource = Files.newOutputStream(target)) {
                format(value, newBufferedWriter(resource, encoder), TextBuffers.of(target, encoder));
            }
        }

        @Override
        public void formatWriter(@NonNull T value, @NonNull Writer resource) throws IOException {
            format(value, resource, TextBuffers.UNKNOWN);
        }

        @Override
        public void formatStream(@NonNull T value, @NonNull OutputStream resource, @NonNull Charset encoding) throws IOException {
            CharsetEncoder encoder = encoding.newEncoder();
            format(value, newBufferedWriter(resource, encoder), TextBuffers.of(resource, encoder));
        }

        public void formatCsv(@NonNull T value, IOSupplier<Csv.@NonNull Writer> source) throws IOException {
            try (Csv.Writer resource = source.getWithIO()) {
                formatCsv(value, resource);
            }
        }

        public void formatCsv(@NonNull T value, Csv.@NonNull Writer resource) throws IOException {
            handler.format(value, resource);
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
