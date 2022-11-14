package nbbrd.io.sys;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class SystemPropertiesTest {

    @Test
    public void testFactories() {
        assertThatNullPointerException()
                .isThrownBy(() -> SystemProperties.of(null, FileSystems.getDefault()));

        assertThatNullPointerException()
                .isThrownBy(() -> SystemProperties.of(System.getProperties(), null));

        assertThat(SystemProperties.DEFAULT)
                .isNotNull();
    }

    @Test
    public void testGetUserHome() {
        Properties p = new Properties();
        SystemProperties x = SystemProperties.of(p, FileSystems.getDefault());

        assertThat(x.getUserHome())
                .isNull();

        p.put(SystemProperties.USER_HOME, "C:\\Temp");
        assertThat(x.getUserHome())
                .isEqualByComparingTo(Paths.get("C:\\Temp"));
    }

    @Test
    public void testGetUserDir() {
        Properties p = new Properties();
        SystemProperties x = SystemProperties.of(p, FileSystems.getDefault());

        assertThat(x.getUserDir())
                .isNull();

        p.put(SystemProperties.USER_DIR, "C:\\Temp");
        assertThat(x.getUserDir())
                .isEqualByComparingTo(Paths.get("C:\\Temp"));
    }

    @Test
    public void testGetJavaClassPath() {
        Properties p = new Properties();
        SystemProperties x = SystemProperties.of(p, FileSystems.getDefault());

        assertThat(x.getJavaClassPath())
                .isEmpty();

        p.put(SystemProperties.PATH_SEPARATOR, ";");
        assertThat(x.getJavaClassPath())
                .isEmpty();

        p.put(SystemProperties.JAVA_CLASS_PATH, "C:\\Temp\\x.jar");
        assertThat(x.getJavaClassPath())
                .containsExactly(Paths.get("C:\\Temp\\x.jar"));

        p.put(SystemProperties.JAVA_CLASS_PATH, "C:\\Temp\\x.jar;C:\\Temp\\y.jar");
        assertThat(x.getJavaClassPath())
                .containsExactly(
                        Paths.get("C:\\Temp\\x.jar"),
                        Paths.get("C:\\Temp\\y.jar")
                );
    }

    @Test
    public void testGetPathSeparator() {
        Properties p = new Properties();
        SystemProperties x = SystemProperties.of(p, FileSystems.getDefault());

        assertThat(x.getPathSeparator()).isNull();

        p.put(SystemProperties.PATH_SEPARATOR, "");
        assertThat(x.getPathSeparator()).isNull();

        p.put(SystemProperties.PATH_SEPARATOR, "; ");
        assertThat(x.getPathSeparator()).isNull();

        p.put(SystemProperties.PATH_SEPARATOR, ";");
        assertThat(x.getPathSeparator()).isEqualTo(';');
    }

    @Test
    public void testDEFAULT() {
        SystemProperties p = SystemProperties.DEFAULT;

        assertThat(p.getJavaVersion()).isEqualTo(System.getProperty("java.version"));
        assertThat(p.getJavaVendor()).isEqualTo(System.getProperty("java.vendor"));
        assertThat(p.getJavaVendorUrl()).hasToString(System.getProperty("java.vendor.url"));
        assertThat(p.getJavaHome()).isEqualTo(Paths.get(System.getProperty("java.home")));
        assertThat(p.getJavaVmSpecificationVersion()).isEqualTo(System.getProperty("java.vm.specification.version"));
        assertThat(p.getJavaVmSpecificationVendor()).isEqualTo(System.getProperty("java.vm.specification.vendor"));
        assertThat(p.getJavaVmSpecificationName()).isEqualTo(System.getProperty("java.vm.specification.name"));
        assertThat(p.getJavaVmVersion()).isEqualTo(System.getProperty("java.vm.version"));
        assertThat(p.getJavaVmVendor()).isEqualTo(System.getProperty("java.vm.vendor"));
        assertThat(p.getJavaVmName()).isEqualTo(System.getProperty("java.vm.name"));
        assertThat(p.getJavaSpecificationVersion()).isEqualTo(System.getProperty("java.specification.version"));
        assertThat(p.getJavaSpecificationVendor()).isEqualTo(System.getProperty("java.specification.vendor"));
        assertThat(p.getJavaSpecificationName()).isEqualTo(System.getProperty("java.specification.name"));
        assertThat(p.getJavaClassVersion()).isEqualTo(System.getProperty("java.class.version"));
        assertThat(p.getJavaClassPath()).containsExactlyElementsOf(splitPath(System.getProperty("java.class.path")));
        assertThat(p.getJavaLibraryPath()).containsExactlyElementsOf(splitPath(System.getProperty("java.library.path")));
        assertThat(p.getJavaIoTmpdir()).isEqualTo(Paths.get(System.getProperty("java.io.tmpdir")));
        assertThat(p.getJavaCompiler()).isEqualTo(System.getProperty("java.compiler"));
        assertThat(p.getOsName()).isEqualTo(System.getProperty("os.name"));
        assertThat(p.getOsArch()).isEqualTo(System.getProperty("os.arch"));
        assertThat(p.getOsVersion()).isEqualTo(System.getProperty("os.version"));
        assertThat(p.getFileSeparator()).hasToString(System.getProperty("file.separator"));
        assertThat(p.getPathSeparator()).hasToString(System.getProperty("path.separator"));
        assertThat(p.getLineSeparator()).hasToString(System.getProperty("line.separator"));
        assertThat(p.getUserName()).isEqualTo(System.getProperty("user.name"));
        assertThat(p.getUserHome()).isEqualTo(Paths.get(System.getProperty("user.home")));
        assertThat(p.getUserDir()).isEqualTo(Paths.get(System.getProperty("user.dir")));
    }

    private static List<Path> splitPath(String input) {
        return Stream.of(input.split(File.pathSeparator, -1))
                .map(Paths::get)
                .collect(Collectors.toList());
    }
}
