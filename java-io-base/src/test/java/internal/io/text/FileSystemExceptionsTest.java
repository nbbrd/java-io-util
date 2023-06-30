package internal.io.text;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static internal.io.text.FileSystemExceptions.checkExist;
import static internal.io.text.FileSystemExceptions.checkIsFile;
import static org.assertj.core.api.Assertions.*;

public class FileSystemExceptionsTest {

    @Test
    public void testCheckExistOnFile(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> checkExist((File) null));

        {
            File file = Files.createFile(temp.resolve("exampleFile")).toFile();

            assertThatCode(() -> checkExist(file))
                    .doesNotThrowAnyException();

            file.delete();
            assertThatIOException()
                    .isThrownBy(() -> checkExist(file))
                    .isInstanceOf(NoSuchFileException.class)
                    .withMessageContaining(file.getPath());
        }

        {
            File folder = Files.createDirectory(temp.resolve("exampleDir")).toFile();

            assertThatCode(() -> checkExist(folder))
                    .doesNotThrowAnyException();

            folder.delete();
            assertThatIOException()
                    .isThrownBy(() -> checkExist(folder))
                    .isInstanceOf(NoSuchFileException.class)
                    .withMessageContaining(folder.getPath());
        }
    }

    @Test
    public void testCheckIsFileOnFile(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> checkIsFile((File) null));

        {
            File file = Files.createFile(temp.resolve("exampleFile")).toFile();

            assertThatCode(() -> checkIsFile(file))
                    .doesNotThrowAnyException();

            file.delete();
            assertThatIOException()
                    .isThrownBy(() -> checkIsFile(file))
                    .isInstanceOf(AccessDeniedException.class)
                    .withMessageContaining(file.getPath());
        }

        {
            File folder = Files.createDirectory(temp.resolve("exampleDir")).toFile();

            assertThatIOException()
                    .isThrownBy(() -> checkIsFile(folder))
                    .isInstanceOf(AccessDeniedException.class)
                    .withMessageContaining(folder.getPath());

            folder.delete();
            assertThatIOException()
                    .isThrownBy(() -> checkIsFile(folder))
                    .isInstanceOf(AccessDeniedException.class)
                    .withMessageContaining(folder.getPath());
        }
    }

    @Test
    public void testCheckExistOnPath(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> checkExist((Path) null));

        {
            Path file = Files.createFile(temp.resolve("exampleFile"));

            assertThatCode(() -> checkExist(file))
                    .doesNotThrowAnyException();

            Files.delete(file);
            assertThatIOException()
                    .isThrownBy(() -> checkExist(file))
                    .isInstanceOf(NoSuchFileException.class)
                    .withMessageContaining(file.toString());
        }

        {
            Path folder = Files.createDirectory(temp.resolve("exampleDir"));

            assertThatCode(() -> checkExist(folder))
                    .doesNotThrowAnyException();

            Files.delete(folder);
            assertThatIOException()
                    .isThrownBy(() -> checkExist(folder))
                    .isInstanceOf(NoSuchFileException.class)
                    .withMessageContaining(folder.toString());
        }
    }

    @Test
    public void testCheckIsFileOnPath(@TempDir Path temp) throws IOException {
        assertThatNullPointerException()
                .isThrownBy(() -> checkIsFile((Path) null));

        {
            Path file = Files.createFile(temp.resolve("exampleFile"));

            assertThatCode(() -> checkIsFile(file))
                    .doesNotThrowAnyException();

            Files.delete(file);
            assertThatIOException()
                    .isThrownBy(() -> checkIsFile(file))
                    .isInstanceOf(AccessDeniedException.class)
                    .withMessageContaining(file.toString());
        }

        {
            Path folder = Files.createDirectory(temp.resolve("exampleDir"));

            assertThatIOException()
                    .isThrownBy(() -> checkIsFile(folder))
                    .isInstanceOf(AccessDeniedException.class)
                    .withMessageContaining(folder.toString());

            Files.delete(folder);
            assertThatIOException()
                    .isThrownBy(() -> checkIsFile(folder))
                    .isInstanceOf(AccessDeniedException.class)
                    .withMessageContaining(folder.toString());
        }
    }
}
