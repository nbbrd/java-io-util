package internal.io.text;

import lombok.NonNull;
import nbbrd.design.VisibleForTesting;

import java.io.File;
import java.nio.file.*;

@lombok.experimental.UtilityClass
public class FileSystemExceptions {

    public static @NonNull File checkSource(@NonNull File source) throws FileSystemException {
        checkExist(source);
        checkIsFile(source);
        return source;
    }

    public static @NonNull File checkTarget(@NonNull File target) throws FileSystemException {
        if (target.exists()) {
            checkIsFile(target);
        }
        return target;
    }

    @VisibleForTesting
    static void checkExist(File source) throws NoSuchFileException {
        if (!source.exists()) {
            throw new NoSuchFileException(source.getPath());
        }
    }

    @VisibleForTesting
    static void checkIsFile(File source) throws AccessDeniedException {
        if (!source.isFile()) {
            throw new AccessDeniedException(source.getPath());
        }
    }

    public static @NonNull Path checkSource(@NonNull Path source) throws FileSystemException {
        checkExist(source);
        checkIsFile(source);
        return source;
    }

    public static @NonNull Path checkTarget(@NonNull Path target) throws FileSystemException {
        if (Files.exists(target)) {
            checkIsFile(target);
        }
        return target;
    }

    @VisibleForTesting
    static void checkExist(Path source) throws NoSuchFileException {
        if (!Files.exists(source)) {
            throw new NoSuchFileException(source.toString());
        }
    }

    @VisibleForTesting
    static void checkIsFile(Path source) throws AccessDeniedException {
        if (!Files.isRegularFile(source)) {
            throw new AccessDeniedException(source.toString());
        }
    }
}
